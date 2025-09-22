package org.example.tinyhttp.http.request;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.example.tinyhttp.http.HttpExceptions;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

@SuppressWarnings("StringConcatenation")
public class HttpRequestTest {

  //#region Request-line validation tests
  @Test
  void testParse_validRequestLine() throws IOException {
    String request = """
                     GET /hello HTTP/1.1\r
                     Host: example.com\r
                     \r
                     """;
    BufferedInputStream in = new BufferedInputStream(new ByteArrayInputStream(request.getBytes()));
    
    HttpRequest req = HttpRequest.parse(in);
    assertEquals("GET", req.getMethod());
    assertEquals("/hello", req.getTarget());
    assertEquals("HTTP/1.1", req.getVersion());
  }

  @Test
  void testParse_emptyRequestLine() {
    String request = "\r\n";
    BufferedInputStream in = new BufferedInputStream(new ByteArrayInputStream(request.getBytes()));
    
    HttpExceptions.BadRequest ex = assertThrows(HttpExceptions.BadRequest.class, () -> 
        HttpRequest.parse(in));
    
    assertTrue(ex.getMessage().contains("Empty Request Line"));
  }

  @Test
  void testParse_malformedRequestLine_missingParts() {
    String request = """
                     GET /hello\r
                     Host: example.com\r
                     \r
                     """;
    BufferedInputStream in = new BufferedInputStream(new ByteArrayInputStream(request.getBytes()));
    
    HttpExceptions.BadRequest ex = assertThrows(HttpExceptions.BadRequest.class, () -> 
        HttpRequest.parse(in));
    
    assertTrue(ex.getMessage().contains("Malformed Request Line"));
  }

  @Test
  void testParse_malformedRequestLine_tooManyParts() {
    String request = """
        GET /hello HTTP/1.1 extra\r
        Host: example.com\r
        \r
        """;
    BufferedInputStream in = new BufferedInputStream(new ByteArrayInputStream(request.getBytes()));
    
    HttpExceptions.BadRequest ex = assertThrows(HttpExceptions.BadRequest.class, () -> 
        HttpRequest.parse(in));
    
    assertTrue(ex.getMessage().contains("Malformed Request Line"));
  }

  @Test
  void testParse_unsupportedHttpVersion() {
    String request = """
                     GET /hello HTTP/1.0\r
                     Host: example.com\r
                     \r
                     """;
    BufferedInputStream in = new BufferedInputStream(new ByteArrayInputStream(request.getBytes()));
    
    HttpExceptions.HttpVersionNotSupported ex = assertThrows(HttpExceptions.HttpVersionNotSupported.class, () -> 
        HttpRequest.parse(in));
    
    assertTrue(ex.getMessage().contains("Only HTTP/1.1 supported"));
  }

  @Test
  void testParse_invalidTarget_notStartingWithSlash() {
    String request = """
                     GET hello HTTP/1.1\r
                     Host: example.com\r
                     \r
                     """;
    BufferedInputStream in = new BufferedInputStream(new ByteArrayInputStream(request.getBytes()));
    
    HttpExceptions.BadRequest ex = assertThrows(HttpExceptions.BadRequest.class, () -> 
        HttpRequest.parse(in));
    
    assertTrue(ex.getMessage().contains("Target must start with '/' or be '*'"));
  }

  @Test
  void testParse_validTarget_wildcard() throws IOException {
    String request = """
                     OPTIONS * HTTP/1.1\r
                     Host: example.com\r
                     \r
                     """;
    BufferedInputStream in = new BufferedInputStream(new ByteArrayInputStream(request.getBytes()));
    
    HttpRequest req = HttpRequest.parse(in);
    assertEquals("*", req.getTarget());
  }

  @Test
  void testParse_targetTooLong() {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < 5000; i++) {
      sb.append("a");
    }
    String longTarget = "/" + sb.toString();
    String request = "GET " + longTarget + " HTTP/1.1\r\n" +
                    "Host: example.com\r\n" +
                    "\r\n";
    BufferedInputStream in = new BufferedInputStream(new ByteArrayInputStream(request.getBytes()));
    
    HttpExceptions.HeaderTooLarge ex = assertThrows(HttpExceptions.HeaderTooLarge.class, () -> 
        HttpRequest.parse(in));
    
    assertTrue(ex.getMessage().contains("Target too long"));
  }
  //#endregion

  //#region Host header required tests
  @Test
  void testParse_missingHostHeader() {
    String request = """
                     GET /hello HTTP/1.1\r
                     \r
                     """;
    BufferedInputStream in = new BufferedInputStream(new ByteArrayInputStream(request.getBytes()));
    
    HttpExceptions.BadRequest ex = assertThrows(HttpExceptions.BadRequest.class, () -> 
        HttpRequest.parse(in));
    
    assertTrue(ex.getMessage().contains("Missing Host header"));
  }

  @Test
  void testParse_multipleDifferingHostHeaders() {
    String request = """
                     GET /hello HTTP/1.1\r
                     Host: example.com\r
                     Host: other.com\r
                     \r
                     """;
    BufferedInputStream in = new BufferedInputStream(new ByteArrayInputStream(request.getBytes()));
    
    HttpExceptions.BadRequest ex = assertThrows(HttpExceptions.BadRequest.class, () -> 
        HttpRequest.parse(in));
    
    assertTrue(ex.getMessage().contains("Multiple differing Host headers"));
  }

  @Test
  void testParse_multipleSameHostHeaders() throws IOException {
    String request = """
                     GET /hello HTTP/1.1\r
                     Host: example.com\r
                     Host: example.com\r
                     \r
                     """;
    BufferedInputStream in = new BufferedInputStream(new ByteArrayInputStream(request.getBytes()));
    
    HttpRequest req = HttpRequest.parse(in);
    assertEquals("example.com", req.getHeaders().first("Host"));
  }

  @Test
  void testParse_hostHeaderCaseInsensitive() throws IOException {
    String request = """
                     GET /hello HTTP/1.1\r
                     HOST: example.com\r
                     \r
                     """;
    BufferedInputStream in = new BufferedInputStream(new ByteArrayInputStream(request.getBytes()));
    
    HttpRequest req = HttpRequest.parse(in);
    assertEquals("example.com", req.getHeaders().first("Host"));
  }
  //#endregion

  //#region Content-Length vs Transfer-Encoding mutual exclusion tests
  @Test
  void testParse_bothContentLengthAndTransferEncoding() {
    String request = """
                     POST /hello HTTP/1.1\r
                     Host: example.com\r
                     Content-Length: 5\r
                     Transfer-Encoding: chunked\r
                     \r
                     """;
    BufferedInputStream in = new BufferedInputStream(new ByteArrayInputStream(request.getBytes()));
    
    HttpExceptions.BadRequest ex = assertThrows(HttpExceptions.BadRequest.class, () -> 
        HttpRequest.parse(in));
    
    assertTrue(ex.getMessage().contains("Content-Length and Transfer-Encoding both present"));
  }

  @Test
  void testParse_contentLengthOnly() throws IOException {
    String request = """
                     POST /hello HTTP/1.1\r
                     Host: example.com\r
                     Content-Length: 5\r
                     \r
                     hello""";
    BufferedInputStream in = new BufferedInputStream(new ByteArrayInputStream(request.getBytes()));
    
    HttpRequest req = HttpRequest.parse(in);
    assertEquals("hello", new String(req.getBody()));
  }

  @Test
  void testParse_transferEncodingOnly() throws IOException {
    String request = """
                     POST /hello HTTP/1.1\r
                     Host: example.com\r
                     Transfer-Encoding: chunked\r
                     \r
                     5\r
                     hello\r
                     0\r
                     \r
                     """;
    BufferedInputStream in = new BufferedInputStream(new ByteArrayInputStream(request.getBytes()));
    
    HttpRequest req = HttpRequest.parse(in);
    assertEquals("hello", new String(req.getBody()));
  }

  @Test
  void testParse_unsupportedTransferEncoding() {
    String request = """
                     POST /hello HTTP/1.1\r
                     Host: example.com\r
                     Transfer-Encoding: gzip\r
                     \r
                     """;
    BufferedInputStream in = new BufferedInputStream(new ByteArrayInputStream(request.getBytes()));
    
    HttpExceptions.NotImplemented ex = assertThrows(HttpExceptions.NotImplemented.class, () -> 
        HttpRequest.parse(in));
    
    assertTrue(ex.getMessage().contains("Transfer-Encoding not supported: gzip"));
  }
  //#endregion

  //#region Body reading - exact Content-Length tests
  @Test
  void testParse_contentLengthExact() throws IOException {
    String request = """
                     POST /hello HTTP/1.1\r
                     Host: example.com\r
                     Content-Length: 11\r
                     \r
                     hello world""";
    BufferedInputStream in = new BufferedInputStream(new ByteArrayInputStream(request.getBytes()));
    
    HttpRequest req = HttpRequest.parse(in);
    assertEquals("hello world", new String(req.getBody()));
  }

  @Test
  void testParse_contentLengthZero() throws IOException {
    String request = """
                     GET /hello HTTP/1.1\r
                     Host: example.com\r
                     Content-Length: 0\r
                     \r
                     """;
    BufferedInputStream in = new BufferedInputStream(new ByteArrayInputStream(request.getBytes()));
    
    HttpRequest req = HttpRequest.parse(in);
    assertEquals(0, req.getBody().length);
  }

  @Test
  void testParse_contentLengthNegative() {
    String request = """
                     POST /hello HTTP/1.1\r
                     Host: example.com\r
                     Content-Length: -1\r
                     \r
                     """;
    BufferedInputStream in = new BufferedInputStream(new ByteArrayInputStream(request.getBytes()));
    
    HttpExceptions.BadRequest ex = assertThrows(HttpExceptions.BadRequest.class, () -> 
        HttpRequest.parse(in));
    
    assertTrue(ex.getMessage().contains("Invalid Content-Length"));
  }

  @Test
  void testParse_contentLengthTooLarge() {
    String request = """
                     POST /hello HTTP/1.1\r
                     Host: example.com\r
                     Content-Length: 2000000\r
                     \r
                     """;
    BufferedInputStream in = new BufferedInputStream(new ByteArrayInputStream(request.getBytes()));
    
    HttpExceptions.BadRequest ex = assertThrows(HttpExceptions.BadRequest.class, () -> 
        HttpRequest.parse(in));
    
    assertTrue(ex.getMessage().contains("Invalid Content-Length"));
  }

  @Test
  void testParse_contentLengthInvalidFormat() {
    String request = """
                     POST /hello HTTP/1.1\r
                     Host: example.com\r
                     Content-Length: abc\r
                     \r
                     """;
    BufferedInputStream in = new BufferedInputStream(new ByteArrayInputStream(request.getBytes()));
    
    HttpExceptions.BadRequest ex = assertThrows(HttpExceptions.BadRequest.class, () -> 
        HttpRequest.parse(in));
    
    assertTrue(ex.getMessage().contains("Invalid Content-Length"));
  }

  @Test
  void testParse_contentLengthIncompleteBody() {
    String request = """
                     POST /hello HTTP/1.1\r
                     Host: example.com\r
                     Content-Length: 20\r
                     \r
                     short""";
    BufferedInputStream in = new BufferedInputStream(new ByteArrayInputStream(request.getBytes()));
    
    HttpExceptions.BadRequest ex = assertThrows(HttpExceptions.BadRequest.class, () -> 
        HttpRequest.parse(in));
    
    assertTrue(ex.getMessage().contains("Incomplete Request Body"));
  }
  //#endregion

  //#region Body reading - chunked happy path tests
  @Test
  void testParse_chunkedBodySimple() throws IOException {
    String request = """
                     POST /hello HTTP/1.1\r
                     Host: example.com\r
                     Transfer-Encoding: chunked\r
                     \r
                     5\r
                     hello\r
                     0\r
                     \r
                     """;
    BufferedInputStream in = new BufferedInputStream(new ByteArrayInputStream(request.getBytes()));
    
    HttpRequest req = HttpRequest.parse(in);
    assertEquals("hello", new String(req.getBody()));
  }

  @Test
  void testParse_chunkedBodyMultipleChunks() throws IOException {
    String request = """
                     POST /hello HTTP/1.1\r
                     Host: example.com\r
                     Transfer-Encoding: chunked\r
                     \r
                     5\r
                     hello\r
                     6\r
                      world\r
                     0\r
                     \r
                     """;
    BufferedInputStream in = new BufferedInputStream(new ByteArrayInputStream(request.getBytes()));
    
    HttpRequest req = HttpRequest.parse(in);
    assertEquals("hello world", new String(req.getBody()));
  }

  @Test
  void testParse_chunkedBodyWithExtensions() throws IOException {
    String request = """
                     POST /hello HTTP/1.1\r
                     Host: example.com\r
                     Transfer-Encoding: chunked\r
                     \r
                     5;chunk-extension\r
                     hello\r
                     0\r
                     \r
                     """;
    BufferedInputStream in = new BufferedInputStream(new ByteArrayInputStream(request.getBytes()));
    
    HttpRequest req = HttpRequest.parse(in);
    assertEquals("hello", new String(req.getBody()));
  }

  @Test
  void testParse_chunkedBodyEmpty() throws IOException {
    String request = """
                     POST /hello HTTP/1.1\r
                     Host: example.com\r
                     Transfer-Encoding: chunked\r
                     \r
                     0\r
                     \r
                     """;
    BufferedInputStream in = new BufferedInputStream(new ByteArrayInputStream(request.getBytes()));
    
    HttpRequest req = HttpRequest.parse(in);
    assertEquals(0, req.getBody().length);
  }
  //#endregion

  //#region Body reading - chunked malformed tests
  @Test
  void testParse_chunkedBodyBadHex() {
    String request = """
                     POST /hello HTTP/1.1\r
                     Host: example.com\r
                     Transfer-Encoding: chunked\r
                     \r
                     5G\r
                     hello\r
                     0\r
                     \r
                     """;
    BufferedInputStream in = new BufferedInputStream(new ByteArrayInputStream(request.getBytes()));
    
    HttpExceptions.BadRequest ex = assertThrows(HttpExceptions.BadRequest.class, () -> 
        HttpRequest.parse(in));
    
    assertTrue(ex.getMessage().contains("Invalid chunk size"));
  }

  @Test
  void testParse_chunkedBodyEmptyChunkSize() {
    String request = """
                     POST /hello HTTP/1.1\r
                     Host: example.com\r
                     Transfer-Encoding: chunked\r
                     \r
                     \r
                     hello\r
                     0\r
                     \r
                     """;
    BufferedInputStream in = new BufferedInputStream(new ByteArrayInputStream(request.getBytes()));
    
    HttpExceptions.BadRequest ex = assertThrows(HttpExceptions.BadRequest.class, () -> 
        HttpRequest.parse(in));
    
    assertTrue(ex.getMessage().contains("Empty chunk size"));
  }

  @Test
  void testParse_chunkedBodyMissingCrlfAfterChunk() {
    String request = """
                     POST /hello HTTP/1.1\r
                     Host: example.com\r
                     Transfer-Encoding: chunked\r
                     \r
                     5\r
                     hello0\r
                     \r
                     """;
    BufferedInputStream in = new BufferedInputStream(new ByteArrayInputStream(request.getBytes()));
    
    HttpExceptions.BadRequest ex = assertThrows(HttpExceptions.BadRequest.class, () -> 
        HttpRequest.parse(in));
    
    assertTrue(ex.getMessage().contains("Missing CRLF after chunk"));
  }

  @Test
  void testParse_chunkedBodyEofMidChunk() {
    String request = """
                     POST /hello HTTP/1.1\r
                     Host: example.com\r
                     Transfer-Encoding: chunked\r
                     \r
                     10\r
                     hello""";
    BufferedInputStream in = new BufferedInputStream(new ByteArrayInputStream(request.getBytes()));
    
    IOException ex = assertThrows(IOException.class, () -> 
        HttpRequest.parse(in));
    
    assertTrue(ex.getMessage().contains("EOF in chunked data"));
  }

  @Test
  void testParse_chunkedBodyChunkSizeTooLarge() {
    String request = """
                     POST /hello HTTP/1.1\r
                     Host: example.com\r
                     Transfer-Encoding: chunked\r
                     \r
                     80000000\r
                     hello\r
                     0\r
                     \r
                     """;
    BufferedInputStream in = new BufferedInputStream(new ByteArrayInputStream(request.getBytes()));
    
    HttpExceptions.BadRequest ex = assertThrows(HttpExceptions.BadRequest.class, () -> 
        HttpRequest.parse(in));
    
    assertTrue(ex.getMessage().contains("Chunk size too large"));
  }

  @Test
  void testParse_chunkedBodyEofBeforeChunkSize() {
    String request = """
                     POST /hello HTTP/1.1\r
                     Host: example.com\r
                     Transfer-Encoding: chunked\r
                     \r
                     """;
    BufferedInputStream in = new BufferedInputStream(new ByteArrayInputStream(request.getBytes()));
    
    IOException ex = assertThrows(IOException.class, () -> 
        HttpRequest.parse(in));
    
    assertTrue(ex.getMessage().contains("EOF before chunk size"));
  }
  //#endregion

  //#region Header parsing edge cases
  @Test
  void testParse_headerLineTooLarge() {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < 10000; i++) {
      sb.append("a");
    }
    String longHeader = "Header: " + sb.toString();
    String request = "GET /hello HTTP/1.1\r\n" +
                    longHeader + "\r\n" +
                    "Host: example.com\r\n" +
                    "\r\n";
    BufferedInputStream in = new BufferedInputStream(new ByteArrayInputStream(request.getBytes()));
    
    HttpExceptions.HeaderTooLarge ex = assertThrows(HttpExceptions.HeaderTooLarge.class, () -> 
        HttpRequest.parse(in));
    
    assertTrue(ex.getMessage().contains("Header line too large"));
  }

  @Test
  void testParse_tooManyHeaders() {
    StringBuilder request = new StringBuilder("GET /hello HTTP/1.1\r\n");
    for (int i = 0; i < 101; i++) {
      request.append("Header").append(i).append(": value").append(i).append("\r\n");
    }
    request.append("Host: example.com\r\n");
    request.append("\r\n");
    
    BufferedInputStream in = new BufferedInputStream(new ByteArrayInputStream(request.toString().getBytes()));
    
    HttpExceptions.HeaderTooLarge ex = assertThrows(HttpExceptions.HeaderTooLarge.class, () -> 
        HttpRequest.parse(in));
    
    assertTrue(ex.getMessage().contains("Too many header fields"));
  }

  @Test
  void testParse_malformedHeader_missingColon() {
    String request = """
                     GET /hello HTTP/1.1\r
                     MalformedHeader\r
                     Host: example.com\r
                     \r
                     """;
    BufferedInputStream in = new BufferedInputStream(new ByteArrayInputStream(request.getBytes()));
    
    HttpExceptions.BadRequest ex = assertThrows(HttpExceptions.BadRequest.class, () -> 
        HttpRequest.parse(in));
    
    assertTrue(ex.getMessage().contains("Malformed header (missing colon)"));
  }

  @Test
  void testParse_malformedHeader_emptyName() {
    String request = """
                     GET /hello HTTP/1.1\r
                     : value\r
                     Host: example.com\r
                     \r
                     """;
    BufferedInputStream in = new BufferedInputStream(new ByteArrayInputStream(request.getBytes()));
    
    HttpExceptions.BadRequest ex = assertThrows(HttpExceptions.BadRequest.class, () -> 
        HttpRequest.parse(in));
    
    assertTrue(ex.getMessage().contains("Malformed header (missing name)"));
  }

  @Test
  void testParse_obsoleteHeaderFolding() {
    String request = """
                     GET /hello HTTP/1.1\r
                     Header: value\r
                      continuation\r
                     Host: example.com\r
                     \r
                     """;
    BufferedInputStream in = new BufferedInputStream(new ByteArrayInputStream(request.getBytes()));
    
    HttpExceptions.BadRequest ex = assertThrows(HttpExceptions.BadRequest.class, () -> 
        HttpRequest.parse(in));
    
    assertTrue(ex.getMessage().contains("Obsolete header folding not allowed"));
  }
  //#endregion

  //#region Integration tests
  // @Test
  // void testParse_completeRequest() throws IOException {
  //   String request = "POST /api/users HTTP/1.1\r\n" +
  //                   "Host: api.example.com\r\n" +
  //                   "Content-Type: application/json\r\n" +
  //                   "Content-Length: 17\r\n" +
  //                   "User-Agent: Mozilla/5.0\r\n" +
  //                   "\r\n" +
  //                   "{\"name\": \"John\"}";
  //   BufferedInputStream in = new BufferedInputStream(new ByteArrayInputStream(request.getBytes()));
    
  //   HttpRequest req = HttpRequest.parse(in);
  //   assertEquals("POST", req.getMethod());
  //   assertEquals("/api/users", req.getTarget());
  //   assertEquals("HTTP/1.1", req.getVersion());
  //   assertEquals("api.example.com", req.getHeaders().first("Host"));
  //   assertEquals("application/json", req.getHeaders().first("Content-Type"));
  //   assertEquals("16", req.getHeaders().first("Content-Length"));
  //   assertEquals("Mozilla/5.0", req.getHeaders().first("User-Agent"));
  //   assertEquals("{\"name\": \"John\"}", new String(req.getBody()));
  // }

  @Test
  void testParse_getRequestNoBody() throws IOException {
    String request = """
                     GET /hello HTTP/1.1\r
                     Host: example.com\r
                     Accept: text/html\r
                     \r
                     """;
    BufferedInputStream in = new BufferedInputStream(new ByteArrayInputStream(request.getBytes()));
    
    HttpRequest req = HttpRequest.parse(in);
    assertEquals("GET", req.getMethod());
    assertEquals("/hello", req.getTarget());
    assertEquals(0, req.getBody().length);
  }
  //#endregion
}