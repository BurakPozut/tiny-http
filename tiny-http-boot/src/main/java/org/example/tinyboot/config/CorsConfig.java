package org.example.tinyboot.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {
  
  @Bean
  public UrlBasedCorsConfigurationSource corsConfigurationSource(){
    var cfg = new CorsConfiguration();
    cfg.setAllowedOrigins(List.of("*"));
    cfg.setAllowedMethods(List.of("GET","HEAD","POST","OPTIONS"));
    cfg.setAllowedHeaders(List.of("Content-Type","Authorization","X-Request-ID"));
    cfg.setAllowCredentials(false);
    cfg.setMaxAge(600L);

    var source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", cfg);
    return source;
  }
}
