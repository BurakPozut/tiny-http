package org.example.tinyhttp.http.response;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.example.tinyhttp.http.request.HttpHeaders;

public final class Cors {
  private Cors(){}
  // Policy
  // If we set the allowCredentails = true, we must not use * for origin
  // we must echo the request Origin when allowed
  public static final boolean ALLOW_CREDENTIANLS = false;

  // Allow all origins by default
  private static final Set<String> allowedOrigins = Set.of("*");

  // Methods supported
  private static final String ALLOWED_METHODS_CSV = "GET,POST,HEAD,OPTIONS";
  
  // Which request headers we will accept from browsers (case-insenstive)
  private static final Set<String> allowedRequestHeaders = Set.of(
    "content-type", "authorization", "x-request-id"
  );

  // Cache preflight result for this many seconds
  private static final int MAX_AGE_SECONDS = 600;

  public static boolean isCorsPreflight(String method, HttpHeaders headers){
    if(!"OPTIONS".equalsIgnoreCase(method)) return false;
    String origin = headers.first("origin");
    String acrm = headers.first("access-control-request-method");
    return origin != null && !origin.isBlank() && acrm != null && !acrm.isBlank();
  }

  //Build Headers for the preflight
  public static String[][] preflightHeaders(HttpHeaders headers){
    String origin = headers.first("origin");
    String reqHeaders = headers.first("access-control-request-headers"); // raw, comma seperated

    String allowOrigin = resolveAllowOrigin(origin);
    List<String[]> list = new ArrayList<>();
    list.add(new String[]{"Vary", "Origin, Accept, Access-Control-Request-Headers, Access-Control-Request-Method"});
    list.add(new String[]{"Access-Control-Allow-Origin", allowOrigin});
    list.add(new String[]{"Access-Control-Allow-Methods", ALLOWED_METHODS_CSV});
    list.add(new String[]{"Access-Control-Max-Age", String.valueOf(MAX_AGE_SECONDS)});

    // If client asked to send specific headers, allow the intersection with our policy
    if(reqHeaders != null && !reqHeaders.isBlank()){
      String allowReq = filterRequestHeaders(reqHeaders);
      if(allowReq.isBlank()){
        list.add(new String[]{"Access-Control-Allow-Headers", allowReq});
      } else {
        // Or advertise the common set
        list.add(new String[]{"Access-Control-Allow-Headers", String.join(",", allowedRequestHeaders)});
      }
    }
    if(ALLOW_CREDENTIANLS) list.add(new String[]{"Access-Control-Allow-Credentials", "true"});
    return list.toArray(String[][]::new);
  }
  
  // Build headers for the actual response (non-OPTIONS)
  public static String[][] actualResponseHeaders(HttpHeaders headers) {
    String origin = headers.first("origin");
    if (origin == null || origin.isBlank()) return null; // not a CORS request

    String allowOrigin = resolveAllowOrigin(origin);
    List<String[]> list = new ArrayList<>();
    list.add(new String[]{"Vary", "Origin, Accept"});
    list.add(new String[]{"Access-Control-Allow-Origin", allowOrigin});
    if(ALLOW_CREDENTIANLS) list.add(new String[]{"Access-Control-Allow-Credentials", "true"});
    return list.toArray(String[][]::new);
  }

  private static String resolveAllowOrigin(String origin){
    if(allowedOrigins.contains("*")) return "*";

    return allowedOrigins.contains(origin) ? origin : null;
  }

  private static String filterRequestHeaders(String reqHeadersCsv){
    // Keep only those we allow; normalize to lowercase tokens
    StringBuilder out = new StringBuilder();
    for(String h: reqHeadersCsv.split(",")){
      String t = h.trim().toLowerCase(Locale.ROOT);
      if(t.isEmpty()) continue;
      if(allowedRequestHeaders.contains(t)){
        if(out.length() > 0) out.append(",");
        out.append(t);
      }
    }
    return out.toString();
  }


  public static String[][] combinewithExtraHeaders(String[][] extraHeaders, HttpHeaders headers){
    return combinewithExtraHeaders(extraHeaders, headers, false);
  }

  public static String[][] combinewithExtraHeaders(String[][] extraHeaders, HttpHeaders headers, boolean isPreflight){
    String[][] corsHeaders = isPreflight ? preflightHeaders(headers) : actualResponseHeaders(headers);
    
    // Both present - combine them
    if(corsHeaders != null && extraHeaders != null){
        String[][] combined = new String[corsHeaders.length + extraHeaders.length][2];
        System.arraycopy(corsHeaders, 0, combined, 0, corsHeaders.length);
        System.arraycopy(extraHeaders, 0, combined, corsHeaders.length, extraHeaders.length);
        return combined;
    }
    
    // Only CORS present - return CORS
    if(corsHeaders != null) return corsHeaders;
    
    // Only extra headers present - return extra headers
    if(extraHeaders != null) return extraHeaders;
    
    // Neither present - return null
    return null;
}
}
