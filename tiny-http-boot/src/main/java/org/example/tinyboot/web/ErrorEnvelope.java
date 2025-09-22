package org.example.tinyboot.web;

import java.time.OffsetDateTime;

public record ErrorEnvelope(Error error) {
  public static ErrorEnvelope of(String code, String message, String requestId){
    return new ErrorEnvelope(new Error(code, message, requestId, OffsetDateTime.now().toString()));
  }
  public record Error(String code, String message, String requestId, String timestamp) {}
}
