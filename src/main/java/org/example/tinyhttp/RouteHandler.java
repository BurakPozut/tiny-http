// File: RouteHandler.java
package org.example.tinyhttp;

import java.io.IOException;
import java.io.OutputStream;

@FunctionalInterface
public interface RouteHandler{
  void handle(RequestContext ctx, OutputStream out, boolean keepAlive) throws IOException;
}