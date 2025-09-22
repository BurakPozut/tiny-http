package org.example.tinyboot.web;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;


@RestController
@RequestMapping("/echo")
public class EchoController {
  
  // If json echo json; else echo bytes as-is
  @PostMapping(consumes=MediaType.ALL_VALUE)
  public ResponseEntity<?> echo(@RequestBody(required=false) byte[] body,
    @RequestHeader(value = "Content-Type", required = false) String contentType ) {
    if(contentType != null && contentType.toLowerCase().startsWith(MediaType.APPLICATION_JSON_VALUE)){
      try {
          JsonNode node = new ObjectMapper().readTree(body == null ? new byte[0] : body);
          return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(node);
      } catch (IOException e) {
        // malformed JSON -> 400 with error envelope (handled by @ControllerAdvice below)
        throw new IllegalArgumentException("Malformed Json");
      }
    }
      
      return ResponseEntity.ok().contentType(contentType == null ? MediaType.APPLICATION_OCTET_STREAM 
        : MediaType.parseMediaType(contentType)).body(body == null ? new byte[0] : body);
  }
  
}
