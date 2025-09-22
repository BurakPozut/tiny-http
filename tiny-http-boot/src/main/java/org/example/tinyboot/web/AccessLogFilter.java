package org.example.tinyboot.web;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


public class AccessLogFilter implements Filter {

  @Override public void doFilter(ServletRequest r, ServletResponse s, FilterChain chain)
      throws IOException, ServletException{

    long start = System.nanoTime();
    HttpServletRequest req = (HttpServletRequest) r;
    HttpServletResponse res = (HttpServletResponse) s;
    chain.doFilter(r, s);

    long durMs = (System.nanoTime() - start)/ 1_000_000;
    String reqId = (String) req.getAttribute("X-Request-ID");
    int status = res.getStatus();
    String method = req.getMethod();
    String path = req.getRequestURI();

    System.out.printf("[ACCESS] %s \"%s %s\" %d %dms reqId=%s%n",
      req.getRemoteAddr(), method, path, status, durMs, reqId);
  }
}
