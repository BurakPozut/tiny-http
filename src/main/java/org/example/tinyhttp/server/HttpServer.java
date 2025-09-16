package org.example.tinyhttp.server;

import java.io.IOException;

import org.example.tinyhttp.http.response.HttpResponses;
import org.example.tinyhttp.routing.Router;



public final class HttpServer {
  private static final int PORT = 8080;

  // Concurrency / socket tuning
  // private static final int ACCEPT_BACKLOG         = 128; // pending connections OS queue
  // private static final int WORKER_THREADS         = Math.max(2, Runtime.getRuntime().availableProcessors());
  // private static final int QUEUE_CAPACITY         = 256; // backpressure; reject beyond this
  // private static final int SOCKET_READ_TIMEOUT_MS = 10_000; // per-connection read timeout

  // public static final int KEEP_ALIVE_IDLE_TIMEOUT_MS = 5000;  // idle gap between requests
  // public static final int MAX_REQUESTS_PER_CONN      = 100;   // safety cap

  // public static final int HEADER_READ_TIMEOUT_MS = 3000;

  private HttpServer() {}

  // private static volatile boolean running = true;

  public static void main(String[] args) {
    System.out.println("[tiny-http] listening on http://localhost:" + PORT);
    Router router = createDefaultRouter();
    HttpServerInstance server = new HttpServerInstance(PORT, router);
       
    try {
      server.start();
      
      // Add shutdown hook
      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        System.out.println("[tiny-http] Shut down requested");
        // running = false;
        server.stop();
      }, "tiny-http-shutdown"));
      
      // Keep main thread alive
      try {
        server.getServerThread().join(); // Wait for server thread to finish
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    } catch (IOException e) {
      System.err.println("[tiny-http] server error: " + e.getMessage());
    }
      
    System.out.println("[tiny-http] done");
  }

  private static Router createDefaultRouter(){
    return new Router()
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