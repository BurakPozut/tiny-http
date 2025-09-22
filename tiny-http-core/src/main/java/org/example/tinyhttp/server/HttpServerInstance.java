package org.example.tinyhttp.server;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.example.tinyhttp.config.Config;
import org.example.tinyhttp.context.RequestContext;
import org.example.tinyhttp.http.HttpExceptions;
import org.example.tinyhttp.http.request.Accepts;
import org.example.tinyhttp.http.request.HttpHeaders;
import org.example.tinyhttp.http.request.HttpRequest;
import org.example.tinyhttp.http.request.RequestMetrics;
import org.example.tinyhttp.http.response.Cors;
import org.example.tinyhttp.http.response.HttpErrorHandler;
import org.example.tinyhttp.http.response.HttpResponses;
import org.example.tinyhttp.logging.AccessLog;
import org.example.tinyhttp.parsing.Url;
import org.example.tinyhttp.parsing.UrlParser;
import org.example.tinyhttp.routing.ResponseMetaData;
import org.example.tinyhttp.routing.Router;
import org.example.tinyhttp.util.Ids;

public class HttpServerInstance {
  private final Router router;
  private volatile boolean running = false;
  private ServerSocket serverSocket;
  private ThreadPoolExecutor workerPool;
  private Thread serverThread;
  private final Config config;

  public HttpServerInstance(Config config, Router router) {
    this.config = config;
    this.router = router; 
  }

  public void start() throws IOException {
    if (running) {
      throw new IllegalStateException("Server is already running");
    }

    serverSocket = new ServerSocket(config.port, config.acceptBacklog);
    serverSocket.setReuseAddress(true);
    workerPool = newWorkerPool();
    running = true;

    serverThread = new Thread(() -> {
      while (running) {
        try {
          Socket client = serverSocket.accept();
          client.setSoTimeout(config.socketReadTimeoutMs);
          try {
            workerPool.execute(() -> handle(client));
          } catch (RejectedExecutionException rex) {
            HttpErrorHandler.sendErrorResponse(client, 503, "Service Unavailable", "Server overloaded, please try again");
          }
        } catch (SocketException se) {
          if (running) {
            System.err.println("[tiny-http] accept error: " + se.getMessage());
          }
        } catch (IOException e) {
          System.err.println("[tiny-http] accept error: " + e.getMessage());
        }
      }
    }, "tiny-http-server");
    serverThread.start();
  }

  public void stop() {
    if (!running) {
      return;
    }

    running = false;
    try {
      if (serverSocket != null) {
        serverSocket.close();
      }
    } catch (IOException ignored) {}

    if (workerPool != null) {
      workerPool.shutdown();
      try {
        if (!workerPool.awaitTermination(10, TimeUnit.SECONDS)) {
          System.out.println("[tiny-http] forcing worker shutdown");
          workerPool.shutdownNow();
        }
      } catch (InterruptedException e) {
        workerPool.shutdownNow();
        Thread.currentThread().interrupt();
      }
    }

    if (serverThread != null) {
      try {
        serverThread.join(5000);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }

    private ThreadPoolExecutor newWorkerPool(){
      var queue = new ArrayBlockingQueue<Runnable>(config.queueCapacity);
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
      return new ThreadPoolExecutor(config.workerThreads, config.workerThreads, 0L, 
      TimeUnit.MILLISECONDS, queue, tf, new ThreadPoolExecutor.AbortPolicy());
  }

    
    private void handle(Socket client) {
      try(client) {
        OutputStream out = client.getOutputStream();
        BufferedInputStream bufferedIn = new BufferedInputStream(client.getInputStream());

        int served = 0;
        boolean keepAlive = true;

        String requestId = Ids.requestId();
        // client.setSoTimeout(KEEP_ALIVE_IDLE_TIMEOUT_MS);
        client.setSoTimeout(config.headerReadTimeoutMs);

        long statNs = System.nanoTime();
        RequestMetrics.set(new RequestMetrics(requestId, "?", "?", 
        client.getInetAddress().getHostAddress() + ":" + client.getPort(), statNs));

        while(keepAlive && served < config.maxRequestsPerConn){
          keepAlive = false; // Start pessimistic, set to true only on success

          try {
            HttpRequest request = HttpRequest.parse(bufferedIn);
            
            // CORS preflight?
            if(Cors.isCorsPreflight(request.getMethod(), request.getHeaders())){
              System.out.println("In CORS Preflight check");
              String[][] h = Cors.preflightHeaders(request.getHeaders());
              // 204 No Content, Content-Lenght: 0
              keepAlive = true;
              HttpResponses.writeText(out, 204, "No Content", "", keepAlive, h);
              // handleOptionsRequest(url, out, keepAlive, headers);
              continue;
            }

            String incomingId = request.getHeaders().first("x-request-id");
            var m = RequestMetrics.get();

            if(m != null){
              if(incomingId != null && !incomingId.isBlank()){
                m.requestId = incomingId.trim();
              }
              m.method = request.getMethod();
              // Check Accept header
              boolean accept = Accepts.wantsJson(request.getHeaders());
              m.prefersJson = accept;
            }
            
            client.setSoTimeout(config.keepAliveIdleTimeoutMs);
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

            boolean keepThisResponseAlive = !clientWantsClose && !serverWantsClose && (served < config.maxRequestsPerConn);

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
                handleOptionsRequest(url, out, keepAlive, request.getHeaders());
                continue;
              }
              var match = router.find(request.getMethod(), url.path());
              if (match.isPresent()) {
                RequestContext ctx = new RequestContext(request, url, match.get().pathVars, config);
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
          } catch (HttpExceptions.BadRequest e) {
            HttpErrorHandler.sendBadRequest(client, e.getMessage());
          } catch (HttpExceptions.HeaderTooLarge e) {
            HttpErrorHandler.sendHeaderTooLarge(client, e.getMessage());
          } catch (HttpExceptions.NotImplemented e) {
            HttpErrorHandler.sendNotImplemented(client, e.getMessage());
          } catch (HttpExceptions.HttpVersionNotSupported e) {
            HttpErrorHandler.sendHttpVersionNotSupported(client, e.getMessage());
          } catch (HttpExceptions.LineTooLong e) {
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
        RequestMetrics.clear();
      }
  }

  private void handleOptionsRequest(Url url, OutputStream out, boolean keepAlive, HttpHeaders headers) throws IOException {
    Set<String> allowed;
    if("*".equals(url.path()) || "/".equals(url.path()) || url.path().isEmpty()){
      allowed = router.getAllServerMethods();
    } else {
      allowed = router.allowedForPath(url.path());
    }
    // var allowed = router.allowedForPath(url.path());
    if(!allowed.isEmpty()){
        String allowHeader = String.join(",", allowed);
        String[][] allowHeaders = {{"Allow", allowHeader}};
        
        // Add CORS support - you'll need to get the headers from the request
        // This method needs access to the request headers
        // String[][] corsHeaders = Cors.preflightHeaders(headers);
        String[][] allHeaders = Cors.combinewithExtraHeaders(allowHeaders, headers, true);
        
        HttpResponses.writeText(out, 204, "No Content", "", keepAlive, allHeaders);
    } else{
        HttpResponses.writeText(out, 404, "Not Found", "No route: " + url.path() + "\n", keepAlive);
    }
  }

    private void handleNoMatchFound(Socket client, Url url, OutputStream out) throws IOException {
      var allowed = router.allowedForPath(url.path());
      // Only return 405 if there are actual route matches for this path
      // (excluding the default OPTIONS that might be returned)
      if (!allowed.isEmpty() && allowed.size() > 1) {
        String allowHeader = String.join(",", allowed);
        HttpErrorHandler.sendMethodNotAllowed(client, allowHeader);
      } else {
        HttpResponses.writeText(out, 404, "Not Found", "No route: " + url.path() + "\n", false);
      }
    }

    public Thread getServerThread() {
      return serverThread;
  }
}