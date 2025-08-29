package org.example.tinyhttp;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public final class HttpResponses {
  private HttpResponses() {
  }

  public static void writeText(OutputStream out, int status, String reason, String text) throws IOException {
    byte[] body = text.getBytes(StandardCharsets.UTF_8);
    String headers = "HTTP/1.1 " + status + " " + reason + "\r\n" +
        "Content-Type: text/plain; charset=utf-8\r\n" +
        "Content-Length: " + body.length + "\r\n" +
        "Connection: close\r\n" +
        "\r\n";
    out.write(headers.getBytes(StandardCharsets.US_ASCII));
    out.write(body);
    out.flush();
  }
}
