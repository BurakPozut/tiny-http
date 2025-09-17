package org.example.tinyhttp.parsing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public final class Json {
  private Json(){}
  public static final ObjectMapper mapper = new ObjectMapper()
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

}
