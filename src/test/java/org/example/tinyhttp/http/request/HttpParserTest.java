package org.example.tinyhttp.http.request;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;

import org.example.tinyhttp.http.HttpExceptions;
import static org.example.tinyhttp.http.request.HttpParser.readLineCRLF;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class HttpParserTest {
  //#region readCRLF
  @Test
  void testReadLineCRLF_normalLine() throws Exception{
    String input = "GET /hello HTTP/1.1\r\n";
    BufferedInputStream in = new BufferedInputStream(new ByteArrayInputStream(input.getBytes()));

    String line = HttpParser.readLineCRLF(in, 1024);
    assertEquals("GET /hello HTTP/1.1", line);
  }

  @Test
  void testReadLineCRLF_emptyLine() throws Exception {
    String input = "\r\n";
    BufferedInputStream in = new BufferedInputStream(new ByteArrayInputStream(input.getBytes()));

    String line = HttpParser.readLineCRLF(in, 1024);
    assertEquals("", line);
  }

  @Test
  void testReadLineCRLF_eofBeforeAnyBytes_returnNull() throws Exception {
    String input = "";
    BufferedInputStream in = new BufferedInputStream(new ByteArrayInputStream(input.getBytes()));

    String line = readLineCRLF(in, 1024);
    assertNull(line);
  }

  @Test
  void testReadLineCRLF_lineTooLong(){
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < 2000; i++) {
        sb.append("a");
    }
    sb.append("\r\n");
    BufferedInputStream in = new BufferedInputStream(new ByteArrayInputStream(sb.toString().getBytes()));

    HttpExceptions.LineTooLong ex = assertThrows(HttpExceptions.LineTooLong.class,
      () -> readLineCRLF(in, 1000));
    
      assertTrue(ex.getMessage().contains("line too long"));
  }

  @Test
  void testReadLineCRLF_unixLineEnding() throws Exception {
    String input = "POST /api/data HTTP/1.1\n";
    BufferedInputStream in = new BufferedInputStream(new ByteArrayInputStream(input.getBytes()));
    
    String line = readLineCRLF(in, 1024);
    assertEquals("POST /api/data HTTP/1.1", line);
  }
  //#endregion
  //#region readFixedBytes
  @Test
  void testReadFixedBytes_normalCase() throws Exception {
    String input = "Hello World!";
    BufferedInputStream in = new BufferedInputStream(new ByteArrayInputStream(input.getBytes()));
    
    byte[] result = HttpParser.readFixedBytes(in, 12);
    assertEquals("Hello World!", new String(result));
  }

  @Test
  void testReadFixedBytes_bodyTooLarge() {
    String input = "test";
    BufferedInputStream in = new BufferedInputStream(new ByteArrayInputStream(input.getBytes()));
    
    HttpExceptions.BadRequest ex = assertThrows(HttpExceptions.BadRequest.class, () -> 
        HttpParser.readFixedBytes(in, Long.MAX_VALUE));
    
    assertTrue(ex.getMessage().contains("Body too large"));
  }
  @Test
  void testReadFixedBytes_incompleteBody() {
    String input = "short";
    BufferedInputStream in = new BufferedInputStream(new ByteArrayInputStream(input.getBytes()));
    
    HttpExceptions.BadRequest ex = assertThrows(HttpExceptions.BadRequest.class, () -> 
        HttpParser.readFixedBytes(in, 10));
    
    assertTrue(ex.getMessage().contains("Incomplete Request Body"));
  }
  //#endregion

  //#region readChunkedBody
  @Test
  void testReadChunkedBody_simpleChunk() throws Exception {
    String input = "5\r\nHello\r\n0\r\n\r\n";
    BufferedInputStream in = new BufferedInputStream(new ByteArrayInputStream(input.getBytes()));
    
    byte[] result = HttpParser.readChunkedBody(in, 1000);
    assertEquals("Hello", new String(result));
  }

  @Test
  void testReadChunkedBody_multipleChunks() throws Exception {
    String input = "5\r\nHello\r\n6\r\n World\r\n0\r\n\r\n";
    BufferedInputStream in = new BufferedInputStream(new ByteArrayInputStream(input.getBytes()));
    
    byte[] result = HttpParser.readChunkedBody(in, 1000);
    assertEquals("Hello World", new String(result));
  }

  @Test
  void testReadChunkedBody_withExtensions() throws Exception {
    String input = "5;chunk-extension\r\nHello\r\n0\r\n\r\n";
    BufferedInputStream in = new BufferedInputStream(new ByteArrayInputStream(input.getBytes()));
    
    byte[] result = HttpParser.readChunkedBody(in, 1000);
    assertEquals("Hello", new String(result));
  }

  @Test
  void testReadChunkedBody_chunkSizeTooLarge() {
    String input = "80000000\r\nHello\r\n";
    BufferedInputStream in = new BufferedInputStream(new ByteArrayInputStream(input.getBytes()));
    
    HttpExceptions.BadRequest ex = assertThrows(HttpExceptions.BadRequest.class, () -> 
        HttpParser.readChunkedBody(in, 1000));

    assertTrue(ex.getMessage().contains("Chunk size too large"));

  }

  @Test
  void testReadChunkedBody_emptyChunkSize() {
    String input = "\r\nHello\r\n";
    BufferedInputStream in = new BufferedInputStream(new ByteArrayInputStream(input.getBytes()));
    
    HttpExceptions.BadRequest ex = assertThrows(HttpExceptions.BadRequest.class, () -> 
        HttpParser.readChunkedBody(in, 1000));

    assertTrue(ex.getMessage().contains("Empty chunk size"));
  }
  //#endregion
}
