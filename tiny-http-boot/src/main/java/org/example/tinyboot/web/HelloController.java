package org.example.tinyboot.web;

import org.example.tinyboot.dto.HelloResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/hello")
public class HelloController {
  
  @GetMapping
  public HelloResponse hello(@RequestParam(required=false) String name) {
    String msg = (name == null) ? "Hello World" : ("Hello " + name);
    return new HelloResponse(msg);
  }
  // HEAD is auto-handled (no body) by Spring for GET endpoints 
}
