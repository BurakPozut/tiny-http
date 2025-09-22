package org.example.tinyhttp.parsing;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class UrlParserTest {
  
  //#region splitPathAndQuery
  @Test
  void testSplitPathAndQuery_withQuery() {
    String[] result = UrlParser.splitPathAndQuery("/users/42?tab=activity&sort=name");
    assertEquals("/users/42", result[0]);
    assertEquals("tab=activity&sort=name", result[1]);
  }

  @Test
  void testSplitPathAndQuery_withoutQuery() {
    String[] result = UrlParser.splitPathAndQuery("/users/42");
    assertEquals("/users/42", result[0]);
    assertEquals("", result[1]);
  }

  @Test
  void testSplitPathAndQuery_emptyQuery() {
    String[] result = UrlParser.splitPathAndQuery("/users/42?");
    assertEquals("/users/42", result[0]);
    assertEquals("", result[1]);
  }

  @Test
  void testSplitPathAndQuery_multipleQuestionMarks() {
    String[] result = UrlParser.splitPathAndQuery("/path?query=value?extra");
    assertEquals("/path", result[0]);
    assertEquals("query=value?extra", result[1]);
  }
  //#endregion

  //#region normalizePath
  @Test
  void testNormalizePath_simplePath() throws IOException {
    String result = UrlParser.normalizePath("/users/42");
    assertEquals("/users/42", result);
  }

  @Test
  void testNormalizePath_rootPath() throws IOException {
    String result = UrlParser.normalizePath("/");
    assertEquals("/", result);
  }

  @Test
  void testNormalizePath_wildcard() throws IOException {
    String result = UrlParser.normalizePath("*");
    assertEquals("*", result);
  }

  @Test
  void testNormalizePath_removeDuplicateSlashes() throws IOException {
    String result = UrlParser.normalizePath("/users//42///profile");
    assertEquals("/users/42/profile", result);
  }

  @Test
  void testNormalizePath_removeCurrentDirectory() throws IOException {
    String result = UrlParser.normalizePath("/users/./42");
    assertEquals("/users/42", result);
  }

  @Test
  void testNormalizePath_removeEmptySegments() throws IOException {
    String result = UrlParser.normalizePath("/users//42");
    assertEquals("/users/42", result);
  }

  @Test
  void testNormalizePath_pathTraversalNotAllowed() {
    IOException ex = assertThrows(IOException.class, () -> 
        UrlParser.normalizePath("/users/../admin"));
    
    assertTrue(ex.getMessage().contains("Path traversal not allowed"));
  }

  @Test
  void testNormalizePath_mustStartWithSlash() {
    IOException ex = assertThrows(IOException.class, () -> 
        UrlParser.normalizePath("users/42"));
    
    assertTrue(ex.getMessage().contains("Path must start with '/'"));
  }

  @Test
  void testNormalizePath_complexPath() throws IOException {
    String result = UrlParser.normalizePath("/api/v1/users/./profile//settings");
    assertEquals("/api/v1/users/profile/settings", result);
  }
  //#endregion

  //#region pctDecode
  @Test
  void testPctDecode_simpleString() throws IOException {
    String result = UrlParser.pctDecode("hello");
    assertEquals("hello", result);
  }

  @Test
  void testPctDecode_singlePercent() throws IOException {
    String result = UrlParser.pctDecode("hello%20world");
    assertEquals("hello world", result);
  }

  @Test
  void testPctDecode_multiplePercents() throws IOException {
    String result = UrlParser.pctDecode("hello%20world%21");
    assertEquals("hello world!", result);
  }

  @Test
  void testPctDecode_unicode() throws IOException {
    String result = UrlParser.pctDecode("caf%C3%A9");
    assertEquals("café", result);
  }

  @Test
  void testPctDecode_plusSign() throws IOException {
    String result = UrlParser.pctDecode("hello+world");
    assertEquals("hello+world", result);
  }

  @Test
  void testPctDecode_mixedCase() throws IOException {
    String result = UrlParser.pctDecode("hello%2Bworld");
    assertEquals("hello+world", result);
  }

  @Test
  void testPctDecode_incompletePercent() {
    IOException ex = assertThrows(IOException.class, () -> 
        UrlParser.pctDecode("hello%2"));
    
    assertTrue(ex.getMessage().contains("Bad precent-escape"));
  }

  @Test
  void testPctDecode_invalidHex() {
    IOException ex = assertThrows(IOException.class, () -> 
        UrlParser.pctDecode("hello%2G"));
    
    assertTrue(ex.getMessage().contains("Bad precent-escape"));
  }

  @Test
  void testPctDecode_percentAtEnd() {
    IOException ex = assertThrows(IOException.class, () -> 
        UrlParser.pctDecode("hello%"));
    
    assertTrue(ex.getMessage().contains("Bad precent-escape"));
  }
  //#endregion

  //#region parseQuery
  @Test
  void testParseQuery_emptyString() throws IOException {
    Map<String, List<String>> result = UrlParser.parseQuery("");
    assertTrue(result.isEmpty());
  }

  @Test
  void testParseQuery_nullString() throws IOException {
    Map<String, List<String>> result = UrlParser.parseQuery(null);
    assertTrue(result.isEmpty());
  }

  @Test
  void testParseQuery_singlePair() throws IOException {
    Map<String, List<String>> result = UrlParser.parseQuery("name=john");
    assertEquals(1, result.size());
    assertEquals("john", result.get("name").get(0));
  }

  @Test
  void testParseQuery_multiplePairs() throws IOException {
    Map<String, List<String>> result = UrlParser.parseQuery("name=john&age=25&city=newyork");
    assertEquals(3, result.size());
    assertEquals("john", result.get("name").get(0));
    assertEquals("25", result.get("age").get(0));
    assertEquals("newyork", result.get("city").get(0));
  }

  @Test
  void testParseQuery_withoutValue() throws IOException {
    Map<String, List<String>> result = UrlParser.parseQuery("name=john&age&city=newyork");
    assertEquals(3, result.size());
    assertEquals("john", result.get("name").get(0));
    assertEquals("", result.get("age").get(0));
    assertEquals("newyork", result.get("city").get(0));
  }

  @Test
  void testParseQuery_duplicateKeys() throws IOException {
    Map<String, List<String>> result = UrlParser.parseQuery("color=red&color=blue&color=green");
    assertEquals(1, result.size());
    assertEquals(3, result.get("color").size());
    assertEquals("red", result.get("color").get(0));
    assertEquals("blue", result.get("color").get(1));
    assertEquals("green", result.get("color").get(2));
  }

  @Test
  void testParseQuery_withPercentEncoding() throws IOException {
    Map<String, List<String>> result = UrlParser.parseQuery("name=john%20doe&city=new%20york");
    assertEquals(2, result.size());
    assertEquals("john doe", result.get("name").get(0));
    assertEquals("new york", result.get("city").get(0));
  }

  @Test
  void testParseQuery_withPlusSign() throws IOException {
    Map<String, List<String>> result = UrlParser.parseQuery("name=john+doe&city=new+york");
    assertEquals(2, result.size());
    assertEquals("john+doe", result.get("name").get(0));
    assertEquals("new+york", result.get("city").get(0));
  }

  @Test
  void testParseQuery_tooManyParams() {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < 1001; i++) {
      if (i > 0) sb.append("&");
      sb.append("param").append(i).append("=value").append(i);
    }
    
    IOException ex = assertThrows(IOException.class, () -> 
        UrlParser.parseQuery(sb.toString()));
    
    assertTrue(ex.getMessage().contains("too many request params"));
  }

  @Test
  void testParseQuery_emptyPairs() throws IOException {
    Map<String, List<String>> result = UrlParser.parseQuery("name=john&&age=25");
    assertEquals(3, result.size());
    assertEquals("john", result.get("name").get(0));
    assertEquals("", result.get("").get(0));
    assertEquals("25", result.get("age").get(0));
  }
  //#endregion

  //#region Url class tests
  @Test
  void testUrl_constructorAndGetters() {
    Map<String, List<String>> query = Map.of("tab", List.of("activity"), "sort", List.of("name"));
    Url url = new Url("/users/42?tab=activity&sort=name", "/users/42", query);
    
    assertEquals("/users/42?tab=activity&sort=name", url.rawTarget());
    assertEquals("/users/42", url.path());
    assertEquals(query, url.query());
  }

  @Test
  void testUrl_q1Method() {
    Map<String, List<String>> query = Map.of("tab", List.of("activity", "profile"), "sort", List.of("name"));
    Url url = new Url("/users/42?tab=activity&tab=profile&sort=name", "/users/42", query);
    
    assertEquals("activity", url.q1("tab"));
    assertEquals("name", url.q1("sort"));
    assertEquals(null, url.q1("nonexistent"));
  }

  @Test
  void testUrl_q1Method_emptyList() {
    Map<String, List<String>> query = Map.of("empty", List.of());
    Url url = new Url("/users/42?empty", "/users/42", query);
    
    assertEquals(null, url.q1("empty"));
  }
  //#endregion
}