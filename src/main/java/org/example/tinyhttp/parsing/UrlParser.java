package org.example.tinyhttp.parsing;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class UrlParser {
  private UrlParser(){}

  // 1) Split target into path + query (no decoding yet)
  public static String[] splitPathAndQuery(String target){
    int q = target.indexOf('?');
    if(q < 0) return  new String[]{target,""};
    return new String[]{target.substring(0,q), target.substring(q + 1)};
  }

  // 2) Normalize path. Remove duplicate '/' reject '..'
  public static String normalizePath(String path) throws IOException{
    if(!path.startsWith("/")) throw new IOException("Path must start with '/'");
    // collapse '/'
    while(path.contains("//")) path = path.replace("//", "/");

    // split and rebuild, reject ".."
    String[] parts = path.split("/");
    Deque<String> safe = new ArrayDeque<>();

    for(String p: parts){
      if(p.isEmpty() || p.equals(".")) continue;
      if(p.equals("..")) throw new IOException("Path traversal not allowed");
      safe.addLast(p);
    }

    StringBuilder sb = new StringBuilder("/");
    var it = safe.iterator();
    while(it.hasNext()) { 
      sb.append(it.next()); 
      if(it.hasNext()) sb.append("/");
    }
    return sb.toString();
  }

  // 3) Percent decoder (strict): %HH where H is hex; leaves '+' as plus (not a space)
  // Use UTF-8 for decoded bytes
  public static String pctDecode(String s) throws IOException{
    byte[] out = new byte[s.length()];
    int oi = 0;

    for(int i = 0; i < s.length();){
      char c = s.charAt(i);
      if(c == '%'){
        if(i + 2 >= s.length()) throw new IOException("Bad precent-escape");
        int hi = hex(s.charAt(i + 1));
        int lo = hex(s.charAt(i + 2));
        if(hi < 0 || lo < 0) throw new IOException("Bad precent-escape");
        out[oi++] = (byte)((hi << 4) | lo); // This combines two 4-bit hexadecimal values into a single 8-bit byte
        i += 3;
      } else{
        out[oi++] = (byte)c;
        i++;
      }
    }
    return new String(out, 0, oi, StandardCharsets.UTF_8);
  }

  private static int hex(char c) {
    if (c >= '0' && c <= '9') return c - '0';
    if (c >= 'A' && c <= 'F') return 10 + (c - 'A');
    if (c >= 'a' && c <= 'f') return 10 + (c - 'a');
    return -1;
  }

  // 4) Parse query into Map<String, List<String>> with decoding
  public static Map<String, List<String>> parseQuery(String raw) throws IOException{
    Map<String, List<String>> map = new LinkedHashMap<>();
    if(raw == null || raw.isEmpty()) return map;
    int pairs = 0;
    for(String part: raw.split("&", -1)){
      if(pairs++ > 1000) throw new IOException("too many request params");
      String k,v;
      int eq = part.indexOf("=");
      if(eq < 0) { k = part; v = "";}
      else { k = part.substring(0, eq); v = part.substring(eq + 1);}
      k = pctDecode(k);
      v = pctDecode(v);
      map.computeIfAbsent(k, key -> new ArrayList<>()).add(v);
    }
    return map;
  }
}
