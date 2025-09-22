package org.example.tinyhttp.http.request;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class HttpHeadersTest {

  //#region Case-insensitivity tests
  @Test
  void testAdd_caseInsensitive() {
    HttpHeaders headers = new HttpHeaders();
    headers.add("Content-Type", "application/json");
    headers.add("CONTENT-TYPE", "text/html");
    headers.add("content-type", "text/plain");
    
    List<String> values = headers.all("Content-Type");
    assertEquals(3, values.size());
    assertEquals("application/json", values.get(0));
    assertEquals("text/html", values.get(1));
    assertEquals("text/plain", values.get(2));
  }

  @Test
  void testAll_caseInsensitive() {
    HttpHeaders headers = new HttpHeaders();
    headers.add("Content-Type", "application/json");
    
    List<String> values1 = headers.all("Content-Type");
    List<String> values2 = headers.all("CONTENT-TYPE");
    List<String> values3 = headers.all("content-type");
    
    assertEquals(values1, values2);
    assertEquals(values2, values3);
    assertEquals(1, values1.size());
    assertEquals("application/json", values1.get(0));
  }

  @Test
  void testFirst_caseInsensitive() {
    HttpHeaders headers = new HttpHeaders();
    headers.add("Content-Type", "application/json");
    
    assertEquals("application/json", headers.first("Content-Type"));
    assertEquals("application/json", headers.first("CONTENT-TYPE"));
    assertEquals("application/json", headers.first("content-type"));
  }

  @Test
  void testHas_caseInsensitive() {
    HttpHeaders headers = new HttpHeaders();
    headers.add("Content-Type", "application/json");
    
    assertTrue(headers.has("Content-Type"));
    assertTrue(headers.has("CONTENT-TYPE"));
    assertTrue(headers.has("content-type"));
    assertFalse(headers.has("Content-Length"));
  }

  @Test
  void testNames_caseInsensitive() {
    HttpHeaders headers = new HttpHeaders();
    headers.add("Content-Type", "application/json");
    headers.add("CONTENT-LENGTH", "123");
    headers.add("User-Agent", "Mozilla/5.0");
    
    Set<String> names = headers.names();
    assertEquals(3, names.size());
    assertTrue(names.contains("content-type"));
    assertTrue(names.contains("content-length"));
    assertTrue(names.contains("user-agent"));
  }
  //#endregion

  //#region Multiple values per key tests
  @Test
  void testAdd_multipleValues() {
    HttpHeaders headers = new HttpHeaders();
    headers.add("Accept", "text/html");
    headers.add("Accept", "application/json");
    headers.add("Accept", "text/plain");
    
    List<String> values = headers.all("Accept");
    assertEquals(3, values.size());
    assertEquals("text/html", values.get(0));
    assertEquals("application/json", values.get(1));
    assertEquals("text/plain", values.get(2));
  }

  @Test
  void testFirst_multipleValues() {
    HttpHeaders headers = new HttpHeaders();
    headers.add("Accept", "text/html");
    headers.add("Accept", "application/json");
    headers.add("Accept", "text/plain");
    
    assertEquals("text/html", headers.first("Accept"));
  }

  @Test
  void testFirstWithFallback_multipleValues() {
    HttpHeaders headers = new HttpHeaders();
    headers.add("Accept", "text/html");
    headers.add("Accept", "application/json");
    
    assertEquals("text/html", headers.first("Accept", "default"));
    assertEquals("default", headers.first("NonExistent", "default"));
  }

  @Test
  void testAll_emptyList() {
    HttpHeaders headers = new HttpHeaders();
    List<String> values = headers.all("NonExistent");
    assertTrue(values.isEmpty());
  }

  @Test
  void testFirst_emptyList() {
    HttpHeaders headers = new HttpHeaders();
    assertNull(headers.first("NonExistent"));
  }
  //#endregion

  //#region Header count limits tests
  @Test
  void testAdd_manyHeaders() {
    HttpHeaders headers = new HttpHeaders();
    
    // Add 1000 headers (should be within reasonable limits)
    for (int i = 0; i < 1000; i++) {
      headers.add("Header" + i, "Value" + i);
    }
    
    assertEquals(1000, headers.names().size());
    assertEquals("Value0", headers.first("Header0"));
    assertEquals("Value999", headers.first("Header999"));
  }

  @Test
  void testAdd_manyValuesPerHeader() {
    HttpHeaders headers = new HttpHeaders();
    
    // Add 100 values to the same header
    for (int i = 0; i < 100; i++) {
      headers.add("Multi-Value-Header", "Value" + i);
    }
    
    List<String> values = headers.all("Multi-Value-Header");
    assertEquals(100, values.size());
    assertEquals("Value0", values.get(0));
    assertEquals("Value99", values.get(99));
  }
  //#endregion

  //#region Single line length limits tests
  @Test
  void testAsciiLenWithCrlf_simpleLine() {
    int length = HttpHeaders.asciiLenWithCrlf("Content-Type: application/json");
    assertEquals(32, length); // "Content-Type: application/json" (33) + CRLF (2)
  }

  @Test
  void testAsciiLenWithCrlf_emptyLine() {
    int length = HttpHeaders.asciiLenWithCrlf("");
    assertEquals(2, length); // Empty string + CRLF
  }

  @Test
  void testAsciiLenWithCrlf_unicodeCharacters() {
    // Unicode characters that take more bytes in ASCII encoding
    int length = HttpHeaders.asciiLenWithCrlf("Header: café");
    assertEquals(14, length); // "Header: caf" (11) + CRLF (2) - 'é' becomes '?' in ASCII
  }

  @Test
  void testAsciiLenWithCrlf_longLine() {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < 100; i++) {
      sb.append("a");
    }
    String longLine = "Header: " + sb.toString();
    int length = HttpHeaders.asciiLenWithCrlf(longLine);
    assertEquals(110, length); // "Header: " (8) + 100 'a's + CRLF (2)
  }

  @Test
  void testApproxAsciiBytes_singleHeader() {
    HttpHeaders headers = new HttpHeaders();
    headers.add("Content-Type", "application/json");
    
    int bytes = headers.approxAsciiBytes();
    assertEquals(32, bytes); // "Content-Type: application/json" (30) + CRLF (2)
  }

  @Test
  void testApproxAsciiBytes_multipleHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.add("Content-Type", "application/json");
    headers.add("Content-Length", "123");
    headers.add("User-Agent", "Mozilla/5.0");
    
    int bytes = headers.approxAsciiBytes();
    // "Content-Type: application/json\r\n" (32) + 
    // "Content-Length: 123\r\n" (21) + 
    // "User-Agent: Mozilla/5.0\r\n" (25) = 78
    assertEquals(78, bytes);
  }

  @Test
  void testApproxAsciiBytes_multipleValues() {
    HttpHeaders headers = new HttpHeaders();
    headers.add("Accept", "text/html");
    headers.add("Accept", "application/json");
    
    int bytes = headers.approxAsciiBytes();
    // "Accept: text/html\r\n" (19) + "Accept: application/json\r\n" (26) = 45
    assertEquals(45, bytes);
  }

  @Test
  void testApproxAsciiBytes_emptyHeaders() {
    HttpHeaders headers = new HttpHeaders();
    int bytes = headers.approxAsciiBytes();
    assertEquals(0, bytes);
  }
  //#endregion

  //#region Edge cases and boundary tests
  @Test
  void testAdd_emptyValue() {
    HttpHeaders headers = new HttpHeaders();
    headers.add("Empty-Header", "");
    
    assertEquals("", headers.first("Empty-Header"));
    assertEquals(1, headers.all("Empty-Header").size());
  }

  @Test
  void testAdd_nullValue() {
    HttpHeaders headers = new HttpHeaders();
    headers.add("Null-Header", null);
    
    assertNull(headers.first("Null-Header"));
    assertEquals(1, headers.all("Null-Header").size());
    assertNull(headers.all("Null-Header").get(0));
  }

  @Test
  void testAdd_whitespaceValue() {
    HttpHeaders headers = new HttpHeaders();
    headers.add("Whitespace-Header", "  value  ");
    
    assertEquals("  value  ", headers.first("Whitespace-Header"));
  }

  @Test
  void testAdd_specialCharacters() {
    HttpHeaders headers = new HttpHeaders();
    headers.add("Special-Header", "value with spaces, commas, and; semicolons");
    
    assertEquals("value with spaces, commas, and; semicolons", headers.first("Special-Header"));
  }

  @Test
  void testToString() {
    HttpHeaders headers = new HttpHeaders();
    headers.add("Content-Type", "application/json");
    headers.add("Content-Length", "123");
    
    String result = headers.toString();
    assertTrue(result.contains("content-type"));
    assertTrue(result.contains("content-length"));
    assertTrue(result.contains("application/json"));
    assertTrue(result.contains("123"));
  }
  //#endregion

  //#region Performance and stress tests
  @Test
  void testAdd_largeNumberOfHeaders() {
    HttpHeaders headers = new HttpHeaders();
    
    // Test with a large number of headers
    for (int i = 0; i < 10000; i++) {
      headers.add("Header" + i, "Value" + i);
    }
    
    assertEquals(10000, headers.names().size());
    assertTrue(headers.has("Header0"));
    assertTrue(headers.has("Header9999"));
    assertFalse(headers.has("Header10000"));
  }

  @Test
  void testAdd_largeValues() {
    HttpHeaders headers = new HttpHeaders();
    
    // Test with large header values
    StringBuilder largeValue = new StringBuilder();
    for (int i = 0; i < 1000; i++) {
      largeValue.append("x");
    }
    
    headers.add("Large-Header", largeValue.toString());
    assertEquals(1000, headers.first("Large-Header").length());
  }
  //#endregion
}