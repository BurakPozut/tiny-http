package org.example.tinyhttp;

import java.io.IOException;

public final class HttpExceptions {
  private HttpExceptions() {
  }

  public static class BadRequest extends IOException {
    public BadRequest(String msg) {
      super(msg);
    }
  }

  public static class HeaderTooLarge extends IOException {
    public HeaderTooLarge(String msg) {
      super(msg);
    }
  }

  public static class NotImplemented extends IOException {
    public NotImplemented(String msg) {
      super(msg);
    }
  }

  public static class HttpVersionNotSupported extends IOException {
    public HttpVersionNotSupported(String msg) {
      super(msg);
    }
  }

  public static class LineTooLong extends IOException {
    public LineTooLong(String msg) {
      super(msg);
    }
  }
}
