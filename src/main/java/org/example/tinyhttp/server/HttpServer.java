package org.example.tinyhttp.server;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.example.tinyhttp.context.RequestContext;
import org.example.tinyhttp.http.HttpExceptions.BadRequest;
import org.example.tinyhttp.http.HttpExceptions.HeaderTooLarge;
import org.example.tinyhttp.http.HttpExceptions.HttpVersionNotSupported;
import org.example.tinyhttp.http.HttpExceptions.LineTooLong;
import org.example.tinyhttp.http.HttpExceptions.NotImplemented;
import org.example.tinyhttp.http.request.HttpRequest;
import org.example.tinyhttp.http.request.RequestMetrics;
import org.example.tinyhttp.http.response.HttpErrorHandler;
import org.example.tinyhttp.http.response.HttpResponses;
import org.example.tinyhttp.parsing.Url;
import org.example.tinyhttp.parsing.UrlParser;
import org.example.tinyhttp.routing.ResponseMetaData;
import org.example.tinyhttp.routing.Router;



public final class HttpServer {
  private static final int PORT = 8080;

  // Concurrency / socket tuning
  private static final int ACCEPT_BACKLOG         = 128; // pending connections OS queue
  private static final int WORKER_THREADS         = Math.max(2, Runtime.getRuntime().availableProcessors());
  private static final int QUEUE_CAPACITY         = 256; // backpressure; reject beyond this
  private static final int SOCKET_READ_TIMEOUT_MS = 10_000; // per-connection read timeout

  public static final int KEEP_ALIVE_IDLE_TIMEOUT_MS = 5000;  // idle gap between requests
  public static final int MAX_REQUESTS_PER_CONN      = 100;   // safety cap

  public static final int HEADER_READ_TIMEOUT_MS = 3000;

  private HttpServer() {}

  private static volatile boolean running = true;

  private static ThreadPoolExecutor newWorkerPool(){
      var queue = new ArrayBlockingQueue<Runnable>(QUEUE_CAPACITY);
      var tf = new ThreadFactory() {
          private final ThreadFactory def = Executors.defaultThreadFactory();
          private int n = 0;
          @Override public Thread newThread(Runnable r){
              Thread t = def.newThread(r);
              t.setName("tiny-http-" + (++n));
              t.setDaemon(false);
              return t;
          }
      };
      return new ThreadPoolExecutor(WORKER_THREADS, WORKER_THREADS, 0L, 
      TimeUnit.MILLISECONDS, queue, tf, new ThreadPoolExecutor.AbortPolicy());
  }

  public static void main(String[] args) {
      System.out.println("[tiny-http] listening on http://localhost:" + PORT);

      ThreadPoolExecutor pool = newWorkerPool();

      try(ServerSocket server = new ServerSocket(PORT, ACCEPT_BACKLOG)){
          server.setReuseAddress(true);

          Runtime.getRuntime().addShutdownHook(new Thread(() -> {
              System.out.println("[tiny-http] Shut down requested");
              running = false;
              try{server.close();}catch(IOException ignored){}
              pool.shutdown();
              try{
                  if(!pool.awaitTermination(10, TimeUnit.SECONDS)){
                      System.out.println("[tiny-http] forcing worker shutdown");
                      pool.shutdownNow();
                  }
              }catch(InterruptedException ie){
                  pool.shutdownNow();
                  Thread.currentThread().interrupt();
              }
          },"tiny-http-shutdown"));

          while(running){
              try{
                  Socket client = server.accept();
                  client.setSoTimeout(SOCKET_READ_TIMEOUT_MS);
                  try {
                      pool.execute(() -> {handle(client);});
                  } catch (RejectedExecutionException rex) {
                        // Queue full → send 503 and drop (backpressure)
                        HttpErrorHandler.sendErrorResponse(client, 503, "Service Unavailable", "Server overloaded, please try again");
                  }
              } catch(SocketException se){
                  // Likely closed during shutdown
                  if (running) System.err.println("[tiny-http] accept error: " + se.getMessage());
              } catch(IOException e){
                  System.err.println("[tiny-http] accept error: " + e.getMessage());
              }
          }
      } catch(IOException e) {
          System.err.println("[tiny-http] server error: " + e.getMessage());
      } finally{
          pool.shutdown();
      }
      System.out.println("[tiny-http] done (accepted one connection)");

  }

  private static void handle(Socket client) {
      try(client) {
        OutputStream out = client.getOutputStream();
        BufferedInputStream bufferedIn = new BufferedInputStream(client.getInputStream());

        int served = 0;
        boolean keepAlive = true;

        String requestId = Ids.requestId();
        // client.setSoTimeout(KEEP_ALIVE_IDLE_TIMEOUT_MS);
        client.setSoTimeout(HEADER_READ_TIMEOUT_MS);

        long statNs = System.nanoTime();
        RequestMetrics.set(new RequestMetrics(requestId, "?", "?", 
        client.getInetAddress().getHostAddress() + ":" + client.getPort(), statNs));

        while(keepAlive && served < MAX_REQUESTS_PER_CONN){
          keepAlive = false; // Start pessimistic, set to true only on success

          try {
            HttpRequest request = HttpRequest.parse(bufferedIn);
            String incomingId = request.getHeaders().first("x-request-id");
            if(incomingId != null && !incomingId.isBlank()){
              var m = RequestMetrics.get();
              if( m != null) { m.requestId = incomingId.trim();}
            }
            var m = RequestMetrics.get();
            if( m != null){
              m.method = request.getMethod();
            }

            client.setSoTimeout(KEEP_ALIVE_IDLE_TIMEOUT_MS);
            served++;

            // Expect: 100-continue (ack before reading body if your parser defers body)
            String expect = request.getHeaders().first("expect");
            if(expect != null && expect.equalsIgnoreCase("100-continue")){
                out.write("HTTP/1.1 100 Continue\r\n\r\n".getBytes(StandardCharsets.US_ASCII));
                out.flush();
            }
            // Decide connection semantics for THIS response
            String connHeader = request.getHeaders().first("connection");
            boolean clientWantsClose = connHeader != null && connHeader.equalsIgnoreCase("close");
            boolean serverWantsClose = false;

            boolean keepThisResponseAlive = !clientWantsClose && !serverWantsClose && (served < MAX_REQUESTS_PER_CONN);

            try {
              // --- build Url from target ---
              String[] pq = UrlParser.splitPathAndQuery(request.getTarget());
              String rawPath  = pq[0];
              String rawQuery = pq[1];
          
              String normPath = UrlParser.normalizePath(rawPath);
              var query = UrlParser.parseQuery(rawQuery);
              Url url = new Url(request.getTarget(), normPath, query);

              var mx = RequestMetrics.get();
              if(mx != null)
                mx.path = url.path();

              // --- route match ---
              // If the method is Options we dont need to look at anything else
              if("OPTIONS".equals(request.getMethod())){
                handleOptionsRequest(url, out, keepAlive);
                continue;
              }
              var match = ROUTER.find(request.getMethod(), url.path());
              if (match.isPresent()) {
                RequestContext ctx = new RequestContext(request, url, match.get().pathVars);
                if ("HEAD".equals(request.getMethod())) {
                  ResponseMetaData meta = match.get().handler.getMetaData(ctx);
                  HttpResponses.writeHEAD(out, 200, "OK", meta.contentType, meta.contentLength, keepThisResponseAlive);
                } else {
                    // Normal GET, POST, etc.
                    match.get().handler.handle(ctx, out, keepThisResponseAlive);
                }
              } else {
                handleNoMatchFound(client, url, out);
              }
            } catch (IOException badUrl) {
              // normalizePath / pctDecode / parseQuery errors → 400 and close
              HttpErrorHandler.sendBadRequest(client, badUrl.getMessage());
              keepThisResponseAlive = false;
            }
            // Next iteration: keep the loop only if we kept this response alive
            keepAlive = keepThisResponseAlive;
            AccessLog.log(RequestMetrics.get());
          } catch (BadRequest e) {
            HttpErrorHandler.sendBadRequest(client, e.getMessage());
          } catch (HeaderTooLarge e) {
            HttpErrorHandler.sendHeaderTooLarge(client, e.getMessage());
          } catch (NotImplemented e) {
            HttpErrorHandler.sendNotImplemented(client, e.getMessage());
          } catch (HttpVersionNotSupported e) {
            HttpErrorHandler.sendHttpVersionNotSupported(client, e.getMessage());
          } catch (LineTooLong e) {
            HttpErrorHandler.sendLineTooLong(client, e.getMessage());
          } catch (IOException ioe) {
            System.err.println("[tiny-http] io error: " + ioe.getMessage());
            // best-effort error response only if stream still usable (optional)
          } catch(Exception e){
            HttpErrorHandler.sendInternalServerError(client, "oops");
          }
        }
      } catch(IOException e){
        // Socket-level errors (connection issues, etc.)
        System.err.println("[tiny-http] socket error: " + e.getMessage());
      } finally{
        RequestMetrics.cler();
      }
  }

  private static void handleOptionsRequest(Url url, OutputStream out, boolean keepAlive) throws IOException {
    var allowed = ROUTER.allowedForPath(url.path());
    if(!allowed.isEmpty()){
      String allowHeader = String.join(",", allowed);
      HttpResponses.writeText(out, 204, "No Content", "", keepAlive, new String[][]{{"Allow",allowHeader}});
    } else{
      HttpResponses.writeText(out, 404, "Not Found", "No route: " + url.path() + "\n", keepAlive);
    }
  }

  private static void handleNoMatchFound(Socket client, Url url, OutputStream out) throws IOException{
    var allowed = ROUTER.allowedForPath(url.path());
    // If the path exits but the method is wrong, If there
    if(!allowed.isEmpty()){
      String allowHeader = String.join(",", allowed);
      HttpErrorHandler.sendMethodNotAllowed(client, allowHeader);
    } else{
      HttpResponses.writeText(out, 404, "Not Found", "No route: " + url.path() + "\n", false);
    }
  }

  private static final Router ROUTER = new Router()
      .get("/hello", (ctx, out, keepAlive) -> {
        String name = ctx.query("name");
        String msg = (name == null) ? "Hello World\n" : ("Hello " + name + "\n");
        HttpResponses.writeText(out, 200, "OK", msg, keepAlive);
      })
      .get("/users/:id", (ctx, out, keepAlive) -> {
        String id = ctx.pathVars("id"); // Already decoded
        HttpResponses.writeText(out, 200, "OK", "user " + id + "\n", keepAlive);
      })
      .post("/echo", (ctx, out, keepAlive) -> {
        String ct = ctx.request().getHeaders().first("content-type", "application/octet-stream");
        HttpResponses.writeRaw(out, 200, "OK", ct, ctx.request().getBody(), keepAlive);
      }).options("*", (ctx, out, keepAlive) -> {
        // Advertise what you generally support
        String allow = "GET,HEAD,POST,OPTIONS";
        HttpResponses.writeText(out, 204, "No Content", "", keepAlive, new String[][]{{"Allow",allow}});
    });
}

/*
 * Step 7 — Router & URL parsing
 * 
 * 
# Normal Post
curl -v --http1.1 \
  -H "Host: localhost" \
  -H "Content-Type: text/plain" \
  --data "Hello World" \
  http://localhost:8080/echo

  Chunked body (curl auto-chunking via stdin)
  printf 'Hello World' | curl -v --http1.1 \
  -H "Host: localhost" \
  -H "Content-Type: text/plain" \
  --data-binary @- \
  http://localhost:8080/echo

  Bad Chunk Size
  printf 'POST /echo HTTP/1.1\r\nHost: localhost\r\nTransfer-Encoding: chunked\r\n\r\nZ\r\nhi\r\n0\r\n\r\n' \
| nc -w 1 localhost 8080
 */