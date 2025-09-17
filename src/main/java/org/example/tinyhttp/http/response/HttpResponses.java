package org.example.tinyhttp.http.response;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.example.tinyhttp.http.request.RequestMetrics;
import org.example.tinyhttp.parsing.Json;
import org.example.tinyhttp.server.HttpServerConstants;

public final class HttpResponses {
  private HttpResponses() {}

  private static final String SERVER_NAME = "tiny-http/0.1";

  private static String rfc1123Now(){
    ZonedDateTime z = ZonedDateTime.now(ZoneOffset.UTC);
    return DateTimeFormatter.RFC_1123_DATE_TIME.format(z);
  }

  // original convenience
  public static void writeText(OutputStream out, int status, String reason, String text, boolean keepAlive) throws IOException {
    writeText(out, status, reason, text, keepAlive, null); // delegate
  }

  public static void writeText(OutputStream out, int status, String reason, String text, 
    boolean  keepAlive, String[][] extraHeaders) throws IOException {

    byte[] body = text.getBytes(StandardCharsets.UTF_8);
    StringBuilder sb = new StringBuilder(128);
    sb.append("HTTP/1.1 ").append(status).append(' ').append(reason).append("\r\n")
      .append("Date: ").append(rfc1123Now()).append("\r\n")
      .append("Server: ").append(SERVER_NAME).append("\r\n")
      .append("Content-Type: text/plain; charset=utf-8\r\n")
      .append("Content-Length: ").append(body.length).append("\r\n");

      var m = RequestMetrics.get();
      if(m != null && m.requestId != null){
        sb.append("X-REQUEST-ID: ").append(m.requestId).append("\r\n");
      }

      if(extraHeaders != null){
        for(String[] h : extraHeaders){
          if(h != null && h.length == 2 && h[0] != null){
            sb.append(h[0]).append(": ").append(h[1] == null ? "" : h[1]).append("\r\n");
          }
        }
      }

    if(keepAlive){
      sb.append("Connection: keep-alive\r\n")
      .append("Keep-Alive: timeout=").append(HttpServerConstants.KEEP_ALIVE_IDLE_TIMEOUT_MS / 1000)
      .append(", max=").append(HttpServerConstants.MAX_REQUESTS_PER_CONN).append("\r\n");
    } else{
      sb.append("Connection: close\r\n");
    }
    sb.append("\r\n");

    out.write(sb.toString().getBytes(StandardCharsets.US_ASCII));
    out.write(body);
    out.flush();
  }

  public static void writeRaw(OutputStream out, int status, String reason, String contentType, byte[] body,
    boolean  keepAlive) throws IOException {

    if(body == null) body = new byte[0];

    StringBuilder sb = new StringBuilder(128);
    sb.append("HTTP/1.1 ").append(status).append(' ').append(reason).append("\r\n")
      .append("Date: ").append(rfc1123Now()).append("\r\n")
      .append("Server: ").append(SERVER_NAME).append("\r\n")
      .append("Content-Type: ").append(contentType).append("\r\n")
      .append("Content-Length: ").append(body.length).append("\r\n");

      var m = RequestMetrics.get();
      if(m != null && m.requestId != null){
        sb.append("X-REQUEST-ID: ").append(m.requestId).append("\r\n");
      }

    if(keepAlive){
      sb.append("Connection: keep-alive\r\n")
        .append("Keep-Alive: timeout=").append(HttpServerConstants.KEEP_ALIVE_IDLE_TIMEOUT_MS / 1000)
        .append(", max=").append(HttpServerConstants.MAX_REQUESTS_PER_CONN).append("\r\n");
    } else{
      sb.append("Connection: close\r\n");
    }
    sb.append("\r\n");
    
    out.write(sb.toString().getBytes(StandardCharsets.US_ASCII));
    out.write(body);
    out.flush();
  }

  public static void writeHEAD(OutputStream out, int status, String reason, String contentType, int length, 
  boolean  keepAlive) throws  IOException{
    StringBuilder sb = new StringBuilder(128);
    sb.append("HTTP/1.1 ").append(status).append(' ').append(reason).append("\r\n")
    .append("Date: ").append(rfc1123Now()).append("\r\n")
    .append("Server: ").append(SERVER_NAME).append("\r\n")
    .append("Content-Type: ").append(contentType).append("\r\n")
    .append("Content-Length: ").append(length).append("\r\n");

    var m = RequestMetrics.get();
    if(m != null && m.requestId != null){
      sb.append("X-REQUEST-ID: ").append(m.requestId).append("\r\n");
    }

    if (keepAlive) {
      sb.append("Connection: keep-alive\r\n")
        .append("Keep-Alive: timeout=").append(HttpServerConstants.KEEP_ALIVE_IDLE_TIMEOUT_MS / 1000)
        .append(", max=").append(HttpServerConstants.MAX_REQUESTS_PER_CONN).append("\r\n");
    } else {
        sb.append("Connection: close\r\n");
    }
    sb.append("\r\n");
    out.write(sb.toString().getBytes(StandardCharsets.US_ASCII));
    out.flush();
  }

  public static void writeJson(OutputStream out, int status, String reason, Object bodyObj,
    boolean keepAlive, String[][] extraHeaders) throws IOException {
      
    byte[] body = Json.mapper.writeValueAsBytes(bodyObj);

    StringBuilder sb = new StringBuilder(256);
    sb.append("HTTP/1.1 ").append(status).append(' ').append(reason).append("\r\n")
    .append("Date: ").append(rfc1123Now()).append("\r\n")
    .append("Server: ").append(SERVER_NAME).append("\r\n")
    .append("Content-Type: application/json; charset=utf-8\r\n")
    .append("Content-Length: ").append(body.length).append("\r\n");

    var m = RequestMetrics.get();
    if(m != null && m.requestId != null){
      sb.append("X-REQUEST-ID: ").append(m.requestId).append("\r\n");
    }

    if (extraHeaders != null) {
     for (String[] h : extraHeaders) {
        if (h != null && h.length == 2 && h[0] != null) {
          sb.append(h[0]).append(": ").append(h[1] == null ? "" : h[1]).append("\r\n");
        }
      }
    }
    
    sb.append(keepAlive ? "Connection: keep-alive\r\n" : "Connection: close\r\n").append("\r\n");

    out.write(sb.toString().getBytes(StandardCharsets.US_ASCII));
    out.write(body);
    out.flush();

    if (m != null) { m.status = status; m.contentLength = body.length; }
  }
}
