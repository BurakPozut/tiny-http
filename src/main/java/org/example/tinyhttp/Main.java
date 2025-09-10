package org.example.tinyhttp;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

// import javax.imageio.IIOException;

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

    private Main() {
    }

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
            try {
                HttpRequest request = HttpRequest.parse(bufferedIn);
                switch (request.getMethod()) {
                    case "GET" -> GetRoutes.handle(request.getTarget(), out);
                    case "POST" -> PostRoutes.handle(request.getTarget(), out, request.getHeaders(), request.getBody());
                    default -> throw new AssertionError();
                }
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
        } catch(IOException e){
            // Socket-level errors (connection issues, etc.)
            System.err.println("[tiny-http] socket error: " + e.getMessage());
        }
    }
}

/*
 * Commands to test
 * 
 * BASIC TESTS:
 * # GET /hello
 * curl http://localhost:8080/hello
 * 
 * # POST /echo with small text
 * curl -X POST http://localhost:8080/echo -H "Content-Type: text/plain" -d "hello"
 * 
 * # Test with netcat
 * printf 'GET /hello HTTP/1.1\r\nHost: x\r\n\r\n' | nc -w 1 localhost 8080
 * 
 * STRESS TESTS (to see backpressure in action):
 * 
 * # 1. Reduce server capacity first:
 * #    - Set WORKER_THREADS = 1
 *    - Set QUEUE_CAPACITY = 2
 *    - Set ACCEPT_BACKLOG = 1
 * 
 * # 2. Send requests faster than server can handle:
 * for i in {1..50}; do curl -s http://localhost:8080/hello & done; wait
 * 
 * # 3. Use Apache Bench (if available):
 * ab -n 1000 -c 10 http://localhost:8080/hello
 * 
 * # 4. Test with slow requests (add delay to GetRoutes.handle):
 * #    Add: Thread.sleep(2000); to simulate slow processing
 * 
 * # 5. Memory exhaustion test:
 * #    - Set MAX_BODY_BYTES = 1000 in HttpRequest.java
 *    - Send large POST requests:
 * curl -X POST http://localhost:8080/echo -H "Content-Type: text/plain" -d "$(python3 -c 'print("a" * 2000)')"
 * 
 * # 6. Connection exhaustion:
 * #    - Set ACCEPT_BACKLOG = 1
 *    - Send many concurrent connections:
 * for i in {1..100}; do (curl -s http://localhost:8080/hello &) done; wait
 * 
 * # 7. Malformed requests (should return 400):
 * printf 'GET /hello HTTP/1.0\r\n\r\n' | nc localhost 8080  # Wrong HTTP version
 * printf 'GET /hello HTTP/1.1\r\n\r\n' | nc localhost 8080  # Missing Host header
 * printf 'POST /echo HTTP/1.1\r\nHost: x\r\nContent-Length: 10\r\n\r\nshort' | nc localhost 8080  # Wrong content length
 * 
 * # 8. Header bomb attack:
 * printf 'GET /hello HTTP/1.1\r\nHost: x\r\n%s\r\n\r\n' "$(python3 -c 'print("X-Header: " + "A" * 10000)')" | nc localhost 8080
 * 
 * EXPECTED BEHAVIOR:
 * - First few requests: 200 OK
 * - When queue full: 503 Service Unavailable
 * - Malformed requests: 400 Bad Request
 * - Large requests: 400 Bad Request (if limits set)
 * - Server should NOT crash, just reject requests gracefully
 * 
 * TO ACTUALLY CRASH THE SERVER (DON'T DO THIS IN PRODUCTION):
 * # 1. Out of memory attack:
 * #    - Remove MAX_BODY_BYTES limit
 *    - Send massive requests: curl -X POST http://localhost:8080/echo -d "$(python3 -c 'print("a" * 100000000)')"
 * 
 * # 2. Thread exhaustion:
 * #    - Set WORKER_THREADS = 1, QUEUE_CAPACITY = 0
 *    - Send requests that never complete (infinite loop in route handler)
 * 
 * # 3. Socket exhaustion:
 * #    - Set ACCEPT_BACKLOG = 0
 *    - Open thousands of connections without closing them
 */