package org.example.tinyhttp.server;

import java.io.IOException;

import org.example.tinyhttp.http.request.Accepts;
import org.example.tinyhttp.http.response.Cors;
import org.example.tinyhttp.http.response.HttpResponses;
import org.example.tinyhttp.parsing.Json;
import org.example.tinyhttp.routing.Router;



public final class HttpServer {
  private static final int PORT = 8080;

  private HttpServer() {}

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
        String msg = (name == null) ? "Hello World" : ("Hello " + name);
        String[][] corsExtraHeaders = Cors.combinewithExtraHeaders(null, ctx.request().getHeaders());
        if(Accepts.wantsJson(ctx.request().getHeaders())) {
          var json = Json.createResponse("message", msg);
          // System.out.println("in GET method JSON msg: " + msg);
          HttpResponses.writeJson(out, 200, "OK", json, keepAlive, corsExtraHeaders);
        }
        else{
          HttpResponses.writeText(out, 200, "OK", msg + "\n", keepAlive, corsExtraHeaders);
        }
      })
      .get("/users/:id", (ctx, out, keepAlive) -> {
        String id = ctx.pathVars("id"); // Already decoded
        var json = Json.createResponse("id", id);
        // String[][] cors = Cors.actualResponseHeaders(ctx.request().getHeaders());
        String[][] corsExtraHeaders = Cors.combinewithExtraHeaders(null, ctx.request().getHeaders());

        HttpResponses.writeJson(out, 200, "OK", json, keepAlive, corsExtraHeaders);
      })
      .post("/echo", (ctx, out, keepAlive) -> {
        String ct = ctx.request().getHeaders().first("content-type", "application/octet-stream");
        // String[][] cors = Cors.actualResponseHeaders(ctx.request().getHeaders());
        String[][] corsExtraHeaders = Cors.combinewithExtraHeaders(null, ctx.request().getHeaders());


        if(Accepts.wantsJson(ctx.request().getHeaders()) && ct.contains("application/json")){
          var node = Json.mapper.readTree(ctx.request().getBody());
          HttpResponses.writeJson(out, 200, "OK", node, keepAlive, corsExtraHeaders);
        } 
        else{
          HttpResponses.writeRaw(out, 200, "OK", ct, ctx.request().getBody(), keepAlive, corsExtraHeaders); // TODO: Add Extra headers to Raw
        }
      });
      // .options("*", (ctx, out, keepAlive) -> {
      //   // Advertise what you generally support
      //   System.out.println("in the * OPTIONS for path: " + ctx.url().path());

      //   var allowed = allowedForPath(ctx.url().path());

      //   String allow = "GET,HEAD,POST,OPTIONS";
      //   String[][] allowHeaders = {{"Allow", allow}};
      //   String[][] corsExtraHeaders = Cors.combinewithExtraHeaders(allowHeaders, ctx.request().getHeaders(), true);

      //   HttpResponses.writeText(out, 204, "No Content", "", keepAlive, corsExtraHeaders);
      // });
  } 
}

/*
 * Step 12 - CORS
 * The server returns JSON as defualt now only in /hello endpoint has a text response
 * 
 * To Test json retun:
 * curl -i -H 'Accept: application/json' 'http://localhost:8080/hello?name=Burak'
 * 
 * To test text return just delete accept
 * 
 * curl -i 'http://localhost:8080/hello?name=Burak'
 * 
 * 
 */