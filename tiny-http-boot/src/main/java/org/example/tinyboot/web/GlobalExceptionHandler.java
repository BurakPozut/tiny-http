package org.example.tinyboot.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import jakarta.servlet.http.HttpServletRequest;

@ControllerAdvice
public class GlobalExceptionHandler {
  
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorEnvelope> badRequest(IllegalArgumentException ex , HttpServletRequest req){
    String reqId = (String) req.getAttribute("X-Request-ID");
    return ResponseEntity.badRequest().body(ErrorEnvelope.of("bad_request", ex.getMessage(), reqId));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorEnvelope> internal(Exception ex, HttpServletRequest req) {
    String reqId = (String) req.getAttribute("X-Request-ID");
    return ResponseEntity.status(500).body(ErrorEnvelope.of("internal_error", "oops", reqId));
  }  
}
