package org.example.tinyhttp.routing;

public class ResponseMetaData {
  public final String contentType;
  public final int contentLength;

  public ResponseMetaData(String contentType, int contentLength) {
    this.contentType = contentType;
    this.contentLength = contentLength;
  }
}