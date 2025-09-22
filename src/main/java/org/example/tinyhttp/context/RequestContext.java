package org.example.tinyhttp.context;

import java.util.Map;

import org.example.tinyhttp.config.Config;
import org.example.tinyhttp.http.request.HttpRequest;
import org.example.tinyhttp.parsing.Url;

public class RequestContext {
  private final HttpRequest req;
  private final Url url;
  private final Map<String, String> pathVars;
  private final Config config;

  public RequestContext(HttpRequest req, Url url, Map<String, String> pathVars, Config config) {
    this.req = req; this.url = url; this.pathVars = pathVars; this.config = config;
  }

  public HttpRequest request(){ return req; }
  public Url url(){ return url; }
  public Map<String, String> pathVars(){ return pathVars; }
  public Config config(){ return config; }

  public String pathVars(String name) { return pathVars.get(name);}
  public String query(String key) { return url.q1(key);}
}
