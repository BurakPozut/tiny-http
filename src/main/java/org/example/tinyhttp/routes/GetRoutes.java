package org.example.tinyhttp.routes;

import java.io.IOException;
import java.io.OutputStream;

import org.example.tinyhttp.HttpResponses;

public class GetRoutes {
  private GetRoutes() {}
    
    public static void handle(String target, OutputStream out) throws IOException {
        if ("/hello".equals(target)) {
            HttpResponses.writeText(out, 200, "OK", "hello world\n");
        } else {
          HttpResponses.writeText(out, 404, "Not Found", "No Route: " + target + "\n");
        }
    }
}
