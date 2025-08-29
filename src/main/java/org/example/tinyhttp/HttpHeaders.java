package org.example.tinyhttp;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Case-insensitive multi-map for HTTP headers. */

public class HttpHeaders {

  private final Map<String, List<String>> map = new LinkedHashMap<>();

  public void add(String name, String value) {
    String k = name.toLowerCase(Locale.ROOT);
    map.computeIfAbsent(k, key -> new ArrayList<>()).add(value);
  }

  public List<String> all(String name) {
    List<String> v = map.get(name.toLowerCase(Locale.ROOT));
    return v == null ? Collections.emptyList() : Collections.unmodifiableList(v);
  }

  public String first(String name) {
    List<String> v = map.get(name.toLowerCase(Locale.ROOT));
    return (v == null || v.isEmpty()) ? null : v.get(0);
  }

  public String first(String name, String fallback) {
    String v = first(name);
    return v == null ? fallback : v;
  }

  public boolean has(String name) {
    return map.containsKey(name.toLowerCase(Locale.ROOT));
  }

  public Set<String> names() {
    return Collections.unmodifiableSet(map.keySet());
  }

  /**
   * Approximate byte size (ASCII) of all header lines including CRLF per line.
   */
  public int approxAsciiBytes() {
    int total = 0;
    for (Map.Entry<String, List<String>> e : map.entrySet()) {
      String k = e.getKey();
      for (String v : e.getValue()) {
        // "Name: value\r\n"
        total += (k.length() + 2 + v.length() + 2);
      }
    }
    return total;
  }

  @Override
  public String toString() {
    return map.toString();
  }

  /* ---------- Static helpers for parsing loop ---------- */

  public static int asciiLenWithCrlf(String line) {
    return line.getBytes(StandardCharsets.US_ASCII).length + 2;
  }
}
