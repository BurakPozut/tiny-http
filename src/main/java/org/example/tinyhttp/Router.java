package org.example.tinyhttp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class Router {
  private static final class Route{
    final String method;
    final String[] segments;
    final RouteHandler handler;

    Route(String method, String pathPattern, RouteHandler handler) {
      this.method = method;
      this.handler = handler;

      this.segments = Arrays.stream(pathPattern.split("/")).filter(s -> !s.isEmpty()).toArray(String[]::new);

    }

    boolean matches(String method, String[] reqSegs, Map<String, String> pathVars){
      if(!this.method.equals(method)) return false;
      if(this.segments.length != reqSegs.length) return false;
      for(int i = 0; i < segments.length; i++){
        String pat = segments[i];
        String got = reqSegs[i];

        if(pat.startsWith(":")){
          pathVars.put(pat.substring(1), got);
        } else if(!pat.equals(got)){
          pathVars.clear();
          return false;
        } 
      }
      return true;
    }
  }

  private final List<Route> routes = new ArrayList<>();
  
  public Router get(String pattern, RouteHandler h){ routes.add(new Route("GET", pattern, h)); return this;}
  public Router post(String pattern, RouteHandler h){ routes.add(new Route("POST", pattern, h)); return this;}

  public Optional<Match> find(String method, String path){
    String[] segs = Arrays.stream(path.split("/")).filter(s -> !s.isEmpty()).toArray(String[]::new);

    for(Route r : routes){
      Map<String, String> vars = new LinkedHashMap<>();
      if(r.matches(method, segs, vars)){
        return Optional.of(new Match(r.handler, vars));
      }
    }

    return Optional.empty();
  }

  public static final class Match{
    public final RouteHandler handler;
    public final Map<String, String> pathVars;

    Match(RouteHandler h, Map<String, String> pathVars) {
        this.handler = h;
        this.pathVars = pathVars;
    }
    
  }
}
