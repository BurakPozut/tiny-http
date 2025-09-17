package org.example.tinyhttp.http.request;

import java.util.Locale;

public final class Accepts {
  private Accepts(){}

  public static boolean wantsJson(HttpHeaders h){
    String accept = h.first("accept");
    if(accept == null) return false;
    accept = accept.toLowerCase(Locale.ROOT);
    return accept.contains("application/json");
  }
}
