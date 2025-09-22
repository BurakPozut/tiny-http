// File: RouteHandler.java
package org.example.tinyhttp.routing;

import java.io.IOException;
import java.io.OutputStream;

import org.example.tinyhttp.context.RequestContext;

@FunctionalInterface
public interface RouteHandler{
  void handle(RequestContext ctx, OutputStream out, boolean keepAlive) throws IOException;
  default ResponseMetaData getMetaData(RequestContext ctx){
    return new ResponseMetaData("text/plain", 0);
  }
}