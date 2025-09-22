package org.example.tinyhttp.logging;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import org.example.tinyhttp.http.request.RequestMetrics;

public final class AccessLog {
  private AccessLog(){}
  private static final DateTimeFormatter TS = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
  private static final boolean JSON = "json".equalsIgnoreCase(System.getenv("LOG-FORMAT"))
                                    || "json".equalsIgnoreCase(System.getProperty("tiny.logFormat"));


  public static void log(RequestMetrics m){
    if(m == null) return;
    long durMs = Math.max(0, (System.nanoTime() - m.startNs) / 1_000_000);
    String ts = TS.format(Instant.now().atOffset(ZoneOffset.UTC));
    int status = (m.status < 0 ? 0 : m.status);
    long bytes = (m.contentLength < 0 ? 0 : m.contentLength);

    if (JSON) {
      // minimal escaping for quotes/backslashes in path
      String path = m.path == null ? "" : m.path.replace("\\", "\\\\").replace("\"","\\\"");
      String method = m.method == null ? "" : m.method;
      String remote = m.remote == null ? "" : m.remote;
      String rid = m.requestId == null ? "" : m.requestId;
      System.out.printf("{\"ts\":\"%s\",\"remote\":\"%s\",\"method\":\"%s\",\"path\":\"%s\",\"status\":%d,\"bytes\":%d,\"durMs\":%d,\"requestId\":\"%s\"}%n",
          ts, remote, method, path, status, bytes, durMs, rid);
    } else {
      System.out.printf("[ACCESS] %s %s \"%s %s\" %d %d %dms reqId=%s%n",
          ts, m.remote, m.method, m.path, status, bytes, durMs, m.requestId);
    }
  }
}
