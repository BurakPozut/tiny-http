package org.example.tinyboot.web;

import org.example.tinyboot.dto.UserResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/users")
public class UserController {
  
  @GetMapping("/{id}")
  public UserResponse get(@PathVariable String id) {
    return new UserResponse(id);
  }
  
}
