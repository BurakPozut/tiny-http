package org.example.tinyhttp.routing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
  public Router head(String pattern, RouteHandler h) { routes.add(new Route("HEAD", pattern, h)); return this;}
  public Router options(String pattern, RouteHandler h) { routes.add(new Route("OPTIONS", pattern, h)); return this;}
  public Router put(String pattern, RouteHandler h){ routes.add(new Route("PUT", pattern, h)); return this;}


  public Optional<Match> find(String method, String path){
    String[] segs = Arrays.stream(path.split("/")).filter(s -> !s.isEmpty()).toArray(String[]::new);

    for(Route r : routes){
      Map<String, String> vars = new LinkedHashMap<>();
      if(r.matches(method, segs, vars)){
        return Optional.of(new Match(r.handler, vars));
      }

      if(isSpecialMatch(method, r, segs, vars)){
        return Optional.of(new Match(r.handler, vars));
      }
    }

    return Optional.empty();
  }

  private boolean isSpecialMatch(String method, Route route, String[] segs, Map<String, String> vars){
    if("HEAD".equals(method) && route.matches("GET", segs, vars)){
      return true;
    }
    return "*".equals(route.method) && "OPTIONS".equals(method);
  }

  public Set<String> allowedForPath(String path){
    String[] segs = Arrays.stream(path.split("/")).filter(s -> !s.isEmpty()).toArray(String[]::new);
    Set<String> allowed = new LinkedHashSet<>();

    for(Route r: routes){
      Map<String, String> tmp = new LinkedHashMap<>();
      if(r.matches(r.method, segs, tmp)){
        allowed.add(r.method);
      }
    }

    // If GET exists and HEAD not, many servers imply HEAD is allowed
    if(allowed.contains("GET")) allowed.add("HEAD");
    allowed.add("OPTIONS");
    return allowed;
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
