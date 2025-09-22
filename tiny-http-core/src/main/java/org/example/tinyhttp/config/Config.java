package org.example.tinyhttp.config;

import java.util.Locale;
// import java.util.Properties;

public final class Config {
  public final int port;
  public final int acceptBacklog;
  public final int workerThreads;
  public final int queueCapacity;
  public final int headerReadTimeoutMs;
  public final int keepAliveIdleTimeoutMs;
  public final int socketReadTimeoutMs;
  public final int maxRequestsPerConn;
  public final int shutdownGraceSeconds; // drain window
  public final String logFormat; // "plain" | "json"

  private Config(int port, int acceptBacklog, int workerThreads, int queueCapacity,
                 int headerReadTimeoutMs, int keepAliveIdleTimeoutMs, int socketReadTimeoutMs,
                 int maxRequestsPerConn, int shutdownGraceSeconds, String logFormat) {
    this.port = port;
    this.acceptBacklog = acceptBacklog;
    this.workerThreads = workerThreads;
    this.queueCapacity = queueCapacity;
    this.headerReadTimeoutMs = headerReadTimeoutMs;
    this.keepAliveIdleTimeoutMs = keepAliveIdleTimeoutMs;
    this.socketReadTimeoutMs = socketReadTimeoutMs;
    this.maxRequestsPerConn = maxRequestsPerConn;
    this.shutdownGraceSeconds = shutdownGraceSeconds;
    this.logFormat = logFormat;
  }

  public static Config load(String[] args) {
    // defaults
    int cpu = Math.max(2, Runtime.getRuntime().availableProcessors());
    // Properties p = System.getProperties();

    int port                    = intOf(envOrProp("PORT", "tiny.port", "8080"));
    int backlog                 = intOf(envOrProp("ACCEPT_BACKLOG", "tiny.acceptBacklog", "128"));
    int workerThreads           = intOf(envOrProp("WORKER_THREADS", "tiny.workerThreads", String.valueOf(cpu)));
    int queueCapacity           = intOf(envOrProp("QUEUE_CAPACITY", "tiny.queueCapacity", "256"));
    int headerReadTimeoutMs     = intOf(envOrProp("HEADER_READ_TIMEOUT_MS", "tiny.headerReadTimeoutMs", "3000"));
    int keepAliveIdleTimeoutMs  = intOf(envOrProp("KEEP_ALIVE_IDLE_TIMEOUT_MS", "tiny.keepAliveIdleTimeoutMs", "5000"));
    int socketReadTimeoutMs     = intOf(envOrProp("SOCKET_READ_TIMEOUT_MS", "tiny.socketReadTimeoutMs", "10000"));
    int maxReqPerConn           = intOf(envOrProp("MAX_REQUESTS_PER_CONN", "tiny.maxRequestsPerConn", "100"));
    int shutdownGraceSeconds    = intOf(envOrProp("SHUTDOWN_GRACE_SECONDS", "tiny.shutdownGraceSeconds", "10"));
    String logFormat            = envOrProp("LOG_FORMAT", "tiny.logFormat", "plain").toLowerCase(Locale.ROOT);

    for (String a : args) {
      if ("--help".equals(a) || "-h".equals(a)) {
        printHelpAndExit();
      }
    }

    return new Config(port, backlog, workerThreads, queueCapacity,
        headerReadTimeoutMs, keepAliveIdleTimeoutMs, socketReadTimeoutMs,
        maxReqPerConn, shutdownGraceSeconds, logFormat);
  }

  private static String envOrProp(String env, String prop, String def) {
    String v = System.getenv(env);
    if (v == null || v.isBlank()) v = System.getProperty(prop);
    return (v == null || v.isBlank()) ? def : v.trim();
  }
  private static int intOf(String s) { return Integer.parseInt(s); }

  private static void printHelpAndExit() {
    System.out.println("""
      tiny-http configuration (env or -Dprop):
        PORT / -Dtiny.port                      (default 8080)
        ACCEPT_BACKLOG / -Dtiny.acceptBacklog   (default 128)
        WORKER_THREADS / -Dtiny.workerThreads   (default = CPU cores or 2)
        QUEUE_CAPACITY / -Dtiny.queueCapacity   (default 256)
        HEADER_READ_TIMEOUT_MS / -Dtiny.headerReadTimeoutMs (default 3000)
        KEEP_ALIVE_IDLE_TIMEOUT_MS / -Dtiny.keepAliveIdleTimeoutMs (default 5000)
        SOCKET_READ_TIMEOUT_MS / -Dtiny.socketReadTimeoutMs (default 10000)
        MAX_REQUESTS_PER_CONN / -Dtiny.maxRequestsPerConn (default 100)
        SHUTDOWN_GRACE_SECONDS / -Dtiny.shutdownGraceSeconds (default 10)
        LOG_FORMAT / -Dtiny.logFormat           (plain | json) (default plain)
      Usage: java -jar tiny-http.jar [--help]
    """);
    System.exit(0);
  }

  public static Config forTesting(int port) {
    // int cpu = Math.max(2, Runtime.getRuntime().availableProcessors());
    return new Config(port, 128, 2, 256, 3000, 5000, 10000, 100, 10, "plain");
}
}