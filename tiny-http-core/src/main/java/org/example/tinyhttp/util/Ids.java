package org.example.tinyhttp.util;

import java.util.UUID;

public final class Ids {
  private Ids(){}

  public static String requestId(){
    return UUID.randomUUID().toString().replace("-", "");
  }
}
