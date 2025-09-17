package org.example.tinyhttp.parsing;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public final class Json {
  private Json(){}
  public static final ObjectMapper mapper = new ObjectMapper()
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

  public static Map<String, String> createResponse(String key, String value) {
    Map<String, String> response = new HashMap<>();
    response.put(key, value);
    return response;
  }

}
