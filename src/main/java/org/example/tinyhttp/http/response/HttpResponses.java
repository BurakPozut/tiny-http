package org.example.tinyhttp.http.response;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import org.example.tinyhttp.server.HttpServer;

public final class HttpResponses {
  private HttpResponses() {
  }

  public static void writeText(OutputStream out, int status, String reason, String text, 
    boolean  keepAlive) throws IOException {

    byte[] body = text.getBytes(StandardCharsets.UTF_8);
    StringBuilder sb = new StringBuilder(128);
    sb.append("HTTP/1.1 ").append(status).append(' ').append(reason).append("\r\n")
      .append("Content-Type: text/plain; charset=utf-8\r\n")
      .append("Content-Length: ").append(body.length).append("\r\n");

    if(keepAlive){
      sb.append("Connection: keep-alive\r\n")
      .append("Keep-Alive: timeout=").append(HttpServer.KEEP_ALIVE_IDLE_TIMEOUT_MS / 1000)
      .append(", max=").append(HttpServer.MAX_REQUESTS_PER_CONN).append("\r\n");
    } else{
      sb.append("Connection: close\r\n");
    }
    sb.append("\r\n");

    out.write(sb.toString().getBytes(StandardCharsets.US_ASCII));
    out.write(body);
    out.flush();
  }

  // convenience: default to close (so old calls keep working)
  // public static void writeText(OutputStream out, int status, String reason, String text) throws IOException {
  //   writeText(out, status, reason, text, false, 0, 0);
  // }
  public static void writeRaw(OutputStream out, int status, String reason, String contentType, byte[] body,
    boolean  keepAlive) throws IOException {

    if(body == null) body = new byte[0];

    StringBuilder sb = new StringBuilder(128);
    sb.append("HTTP/1.1 ").append(status).append(' ').append(reason).append("\r\n")
      .append("Content-Type: ").append(contentType).append("\r\n")
      .append("Content-Length: ").append(body.length).append("\r\n");

    if(keepAlive){
      sb.append("Connection: keep-alive\r\n")
        .append("Keep-Alive: timeout=").append(HttpServer.KEEP_ALIVE_IDLE_TIMEOUT_MS / 1000)
        .append(", max=").append(HttpServer.MAX_REQUESTS_PER_CONN).append("\r\n");
    } else{
      sb.append("Connection: close\r\n");
    }
    sb.append("\r\n");
    
    out.write(sb.toString().getBytes(StandardCharsets.US_ASCII));
    out.write(body);
    out.flush();
  }

  // convenience: default to close (so old calls keep working)
  // public static void writeRaw(OutputStream out, int status, String reason, String contentType, byte[] body) throws IOException {
  //   writeRaw(out, status, reason, contentType, body, false, 0, 0);
  // }
}
