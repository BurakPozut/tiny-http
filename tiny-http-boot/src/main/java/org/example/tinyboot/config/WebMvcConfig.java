package org.example.tinyboot.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer{
  
  @Override
  public void addCorsMappings(CorsRegistry registry){
    registry.addMapping("/**")
    .allowedOrigins("*")
    .allowedMethods("GET", "HEAD", "POST", "OPTIONS")
    .allowedHeaders("Content-Type","Authorization","X-Request-ID") 
    .allowCredentials(false)
    .maxAge(600L);
   }
}
