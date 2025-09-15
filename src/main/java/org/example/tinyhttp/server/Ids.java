package org.example.tinyhttp.server;

import java.util.UUID;

public final class Ids {
  private Ids(){}

  public static String requestId(){
    return UUID.randomUUID().toString().replace("-", "");
  }
}
