package org.example.tinyhttp;

import java.util.Map;

public class RequestContext {
  private final HttpRequest req;
  private final Url url;
  private final Map<String, String> pathVars;

  public RequestContext(HttpRequest req, Url url, Map<String, String> pathVars) {
    this.req = req; this.url = url; this.pathVars = pathVars;
  }

  public HttpRequest request(){ return req; }
  public Url url(){ return url; }
  public Map<String, String> pathVars(){ return pathVars; }

  public String pathVars(String name) { return pathVars.get(name);}
  public String query(String key) { return url.q1(key);}
}
