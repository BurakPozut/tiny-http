package org.example.tinyhttp.http.response;

import java.time.OffsetDateTime;

public class ErrorEnvelope {
  public final Error error;

  public ErrorEnvelope(int code, String message, String requestId) {
    this.error = new Error(code, message, requestId);
  }

  public static final class Error{
    public final int code;
    public final String message;
    public final String requestId;
    public final String timestamp = OffsetDateTime.now().toString();

    public Error(int code, String message, String requestId) {
      this.code = code;
      this.message = message;
      this.requestId = requestId;
    }
  }
}
