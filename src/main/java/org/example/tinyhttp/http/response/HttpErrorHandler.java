package org.example.tinyhttp.http.response;

import java.io.IOException;
import java.net.Socket;

public final class HttpErrorHandler {
  private HttpErrorHandler(){}

  private static String[][] headers(String k, String v) { return new String[][] { {k, v} }; }

  public static void sendErrorResponse(Socket client, int status, String reason, String message){
    sendErrorResponse(client, status, reason, message, null);
  }

  public static void sendErrorResponse(Socket client, int status, String reason, String message, String[][] extraHeaders){
    try {
      if (client.isClosed() || client.isOutputShutdown()) {
        System.err.println("[DEBUG] Socket closed, skipping error response");
        return;
      }
      HttpResponses.writeText(client.getOutputStream(), status, reason, message, false, extraHeaders);
    } catch (IOException ignored) {
      System.err.println("[DEBUG] IOException in sendErrorResponse: " + ignored.getMessage());
      // ignored.printStackTrace();
    }
  }

  public static void sendBadRequest(Socket client, String message){
    sendErrorResponse(client, 400, "Bad Request", message);
  }

  public static void sendHeaderTooLarge(Socket client, String message){
    sendErrorResponse(client, 431, "Request Header Fields Too Large", message);
  }

  public static void sendNotImplemented(Socket client, String message){
    sendErrorResponse(client, 501, "Not Implemented", message);
  }

  public static void sendHttpVersionNotSupported(Socket client, String message){
    sendErrorResponse(client, 505, "HTTP Version Not Supported", message);
  }

  public static void sendLineTooLong(Socket client, String message){
    sendErrorResponse(client, 430, "", message);
  }

  public static void sendInternalServerError(Socket client, String message){
    sendErrorResponse(client, 500, "Internal Server Error", message);
  }

  public static void sendMethodNotAllowed(Socket client, String allowCsv){
    sendErrorResponse(client, 405, "Method Not Allowed", "Method Not Allowed\n", headers("Allow", allowCsv));
  }
}
