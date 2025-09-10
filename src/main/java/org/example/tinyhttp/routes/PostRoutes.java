package org.example.tinyhttp.routes;

import java.io.IOException;
import java.io.OutputStream;

import org.example.tinyhttp.HttpHeaders;
import org.example.tinyhttp.HttpResponses;

public class PostRoutes {
  private PostRoutes() {}
    
  public static void handle(String target, OutputStream out, HttpHeaders headers, byte[] body, boolean  keepAlive) throws IOException {
      if ("/echo".equals(target)) {
          String ct = headers.first("content-type", "application/octet-stream");
          HttpResponses.writeRaw(out, 200, "OK", ct, body, keepAlive);
      } else {
          HttpResponses.writeText(out, 404, "Not Found", "No route: " + target + "\n", keepAlive);
      }
  }
}
