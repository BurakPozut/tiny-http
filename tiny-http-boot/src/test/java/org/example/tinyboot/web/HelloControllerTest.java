package org.example.tinyboot.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(controllers = HelloController.class)
class HelloControllerTest {
  @Autowired MockMvc mvc;

  @Test
  void hello_ok() throws Exception {
    mvc.perform(get("/hello"))
       .andExpect(status().isOk())
       .andExpect(jsonPath("$.message").value("Hello World"));
  }
}
