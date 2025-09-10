package org.example.tinyhttp;

import java.io.IOException;
import java.net.Socket;

public final class HttpErrorHandler {
  private HttpErrorHandler(){}

  public static void sendErrorResponse(Socket client, int status, String reason, String message){
    try {
      HttpResponses.writeText(client.getOutputStream(), status, reason, message + '\n');
    } catch (IOException ignored) {
      System.err.println("[DEBUG] IOException in sendErrorResponse: " + ignored.getMessage());
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
    sendErrorResponse(client, 404, "HTTP Version Not Supported", message);
  }

  public static void sendLineTooLong(Socket client, String message){
    sendErrorResponse(client, 430, "", message);
  }

  public static void sendInternalServerError(Socket client, String message){
    sendErrorResponse(client, 500, "Internal Server Error", message);
  }
}
