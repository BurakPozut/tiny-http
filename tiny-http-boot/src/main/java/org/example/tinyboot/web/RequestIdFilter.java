package org.example.tinyboot.web;

import java.io.IOException;
import java.util.UUID;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;

public class RequestIdFilter implements Filter {

  @Override public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) 
  throws IOException, ServletException{
    String requestId = UUID.randomUUID().toString().replace("-", "");
    req.setAttribute("X-Request-ID", requestId);
    ((HttpServletResponse) res).setHeader("X-Request-ID", requestId);
    chain.doFilter(req, res);
  }
}
