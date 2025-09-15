package org.example.tinyhttp.logging;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import org.example.tinyhttp.http.request.RequestMetrics;

public final class AccessLog {
  private AccessLog(){}
  private static final DateTimeFormatter TS = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

  public static void log(RequestMetrics m){
    if(m == null) return;
    long durMS = Math.max(0, (System.nanoTime() - m.startNs) / 1_000_000);
    String ts = TS.format(Instant.now().atOffset(ZoneOffset.UTC));
    int status = (m.status < 0 ? 0 : m.status);
    long bytes = (m.contentLength < 0 ? 0 : m.contentLength);

    // Common style one-liner
    System.out.printf(
      "[ACCESS] %s %s \"%s %s\" %d %d %dms reqId=%s\n",
      ts, m.remote, m.method, m.path, status, bytes, durMS, m.requestId
    );
  }
}
