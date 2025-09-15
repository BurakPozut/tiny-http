package org.example.tinyhttp.http.request;

public final class RequestMetrics {
  public String requestId;
  public String method;
  public String path;
  public final String remote;
  public final long startNs;
  public final int status = -1; //Set by HttpResponses when writing
  public final long contentLength = -1; //Set by HttpResponses when writing

  public RequestMetrics(String requstId, String method, String path, String remote, long startNs) {
    this.requestId = requstId;
    this.method = method;
    this.path = path;
    this.remote = remote;
    this.startNs = startNs;
  }

  private static final ThreadLocal<RequestMetrics> TL = new ThreadLocal<>();
  public static void set(RequestMetrics m ) { TL.set(m); }
  public static RequestMetrics get(){ return TL.get(); }
  public static void cler() { TL.remove(); }
}
