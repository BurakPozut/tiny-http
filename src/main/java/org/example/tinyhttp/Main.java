package org.example.tinyhttp;

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

import org.example.tinyhttp.HttpExceptions.BadRequest;
import org.example.tinyhttp.HttpExceptions.HeaderTooLarge;
import org.example.tinyhttp.HttpExceptions.HttpVersionNotSupported;
import org.example.tinyhttp.HttpExceptions.LineTooLong;
import org.example.tinyhttp.HttpExceptions.NotImplemented;
import org.example.tinyhttp.routes.GetRoutes;
import org.example.tinyhttp.routes.PostRoutes;

public final class Main {
    private static final int PORT = 8080;

    // Concurrency / socket tuning
    private static final int ACCEPT_BACKLOG         = 128; // pending connections OS queue
    private static final int WORKER_THREADS         = Math.max(2, Runtime.getRuntime().availableProcessors());
    private static final int QUEUE_CAPACITY         = 256; // backpressure; reject beyond this
    private static final int SOCKET_READ_TIMEOUT_MS = 10_000; // per-connection read timeout

    public static final int KEEP_ALIVE_IDLE_TIMEOUT_MS = 5000;  // idle gap between requests
    public static final int MAX_REQUESTS_PER_CONN      = 100;   // safety cap

    private Main() {}

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

            client.setSoTimeout(KEEP_ALIVE_IDLE_TIMEOUT_MS);

            while(keepAlive && served < MAX_REQUESTS_PER_CONN){
                keepAlive = false; // Start pessimistic, set to true only on success

                try {
                    HttpRequest request = HttpRequest.parse(bufferedIn);
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


                    switch (request.getMethod()) {
                        case "GET" -> GetRoutes.handle(request.getTarget(), out, keepThisResponseAlive);
                        case "POST" -> PostRoutes.handle(request.getTarget(), out, request.getHeaders(), request.getBody(), keepThisResponseAlive);
                        default -> {
                            // 405 and close
                            HttpResponses.writeText(out, 405, "Method Not Allowed", "Only GET/POST supported\n", false);
                            keepThisResponseAlive = false;
                        }
                    }
                    // Next iteration: keep the loop only if we kept this response alive
                    keepAlive = keepThisResponseAlive;
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
        }
    }
}

/*
 * STEP 6: keep-alive handling
 * 
 * 
 * Test keep-alive functionality:
 * # Single connection with multiple requests
 * printf 'GET /hello HTTP/1.1\r\nHost: x\r\n\r\nGET /hello HTTP/1.1\r\nHost: x\r\n\r\n' | nc localhost 8080
 * 
 * # Test with curl (easier to read)
 * curl http://localhost:8080/hello
 * 
 * # Test error handling (should close connection on errors)
 * printf 'GET /hello HTTP/1.0\r\n\r\n' | nc localhost 8080
 */