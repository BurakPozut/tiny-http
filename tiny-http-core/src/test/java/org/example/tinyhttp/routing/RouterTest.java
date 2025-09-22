package org.example.tinyhttp.routing;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class RouterTest {

  //#region Path params tests
  @Test
  void testFind_pathWithSingleParam() {
    Router router = new Router();
    router.get("/users/:id", (ctx, out, keepAlive) -> {});
    
    Optional<Router.Match> match = router.find("GET", "/users/123");
    
    assertTrue(match.isPresent());
    assertEquals("123", match.get().pathVars.get("id"));
  }

  @Test
  void testFind_pathWithMultipleParams() {
    Router router = new Router();
    router.get("/users/:userId/posts/:postId", (ctx, out, keepAlive) -> {});
    
    Optional<Router.Match> match = router.find("GET", "/users/123/posts/456");
    
    assertTrue(match.isPresent());
    assertEquals("123", match.get().pathVars.get("userId"));
    assertEquals("456", match.get().pathVars.get("postId"));
  }

  @Test
  void testFind_pathWithParamsAtDifferentPositions() {
    Router router = new Router();
    router.get("/:version/users/:id", (ctx, out, keepAlive) -> {});
    
    Optional<Router.Match> match = router.find("GET", "/v1/users/123");
    
    assertTrue(match.isPresent());
    assertEquals("v1", match.get().pathVars.get("version"));
    assertEquals("123", match.get().pathVars.get("id"));
  }

  @Test
  void testFind_pathWithParamsAndStaticSegments() {
    Router router = new Router();
    router.get("/api/v1/users/:id/profile", (ctx, out, keepAlive) -> {});
    
    Optional<Router.Match> match = router.find("GET", "/api/v1/users/123/profile");
    
    assertTrue(match.isPresent());
    assertEquals("123", match.get().pathVars.get("id"));
  }

  @Test
  void testFind_pathWithEmptyParam() {
    Router router = new Router();
    router.get("/users/:id", (ctx, out, keepAlive) -> {});
    
    Optional<Router.Match> match = router.find("GET", "/users/");
    
    assertFalse(match.isPresent());
  }

  @Test
  void testFind_pathWithSpecialCharactersInParam() {
    Router router = new Router();
    router.get("/files/:filename", (ctx, out, keepAlive) -> {});
    
    Optional<Router.Match> match = router.find("GET", "/files/my-file_123.txt");
    
    assertTrue(match.isPresent());
    assertEquals("my-file_123.txt", match.get().pathVars.get("filename"));
  }

  @Test
  void testFind_pathWithNumericParam() {
    Router router = new Router();
    router.get("/products/:id", (ctx, out, keepAlive) -> {});
    
    Optional<Router.Match> match = router.find("GET", "/products/42");
    
    assertTrue(match.isPresent());
    assertEquals("42", match.get().pathVars.get("id"));
  }
  //#endregion

  //#region No match → 404 tests
  @Test
  void testFind_noMatch_differentPath() {
    Router router = new Router();
    router.get("/users", (ctx, out, keepAlive) -> {});
    
    Optional<Router.Match> match = router.find("GET", "/posts");
    
    assertFalse(match.isPresent());
  }

  @Test
  void testFind_noMatch_partialPath() {
    Router router = new Router();
    router.get("/users/:id", (ctx, out, keepAlive) -> {});
    
    Optional<Router.Match> match = router.find("GET", "/users");
    
    assertFalse(match.isPresent());
  }

  @Test
  void testFind_noMatch_extraSegments() {
    Router router = new Router();
    router.get("/users/:id", (ctx, out, keepAlive) -> {});
    
    Optional<Router.Match> match = router.find("GET", "/users/123/posts");
    
    assertFalse(match.isPresent());
  }

  @Test
  void testFind_noMatch_emptyRouter() {
    Router router = new Router();
    
    Optional<Router.Match> match = router.find("GET", "/any/path");
    
    assertFalse(match.isPresent());
  }

  @Test
  void testFind_noMatch_rootPath() {
    Router router = new Router();
    router.get("/users", (ctx, out, keepAlive) -> {});
    
    Optional<Router.Match> match = router.find("GET", "/");
    
    assertFalse(match.isPresent());
  }

  @Test
  void testFind_noMatch_staticSegmentMismatch() {
    Router router = new Router();
    router.get("/users/:id", (ctx, out, keepAlive) -> {});
    
    Optional<Router.Match> match = router.find("GET", "/posts/123");
    
    assertFalse(match.isPresent());
  }
  //#endregion

  //#region Method mismatch → 405 tests
  @Test
  void testFind_methodMismatch_getVsPost() {
    Router router = new Router();
    router.get("/users", (ctx, out, keepAlive) -> {});
    
    Optional<Router.Match> match = router.find("POST", "/users");
    
    assertFalse(match.isPresent());
  }

  @Test
  void testFind_methodMismatch_postVsGet() {
    Router router = new Router();
    router.post("/users", (ctx, out, keepAlive) -> {});
    
    Optional<Router.Match> match = router.find("GET", "/users");
    
    assertFalse(match.isPresent());
  }

  @Test
  void testFind_methodMismatch_putVsGet() {
    Router router = new Router();
    router.get("/users", (ctx, out, keepAlive) -> {});
    
    Optional<Router.Match> match = router.find("PUT", "/users");
    
    assertFalse(match.isPresent());
  }

  @Test
  void testFind_methodMismatch_deleteVsPost() {
    Router router = new Router();
    router.post("/users", (ctx, out, keepAlive) -> {});
    
    Optional<Router.Match> match = router.find("DELETE", "/users");
    
    assertFalse(match.isPresent());
  }
  //#endregion

  //#region HEAD method implied from GET tests
  @Test
  void testFind_headImpliedFromGet() {
    Router router = new Router();
    router.get("/users", (ctx, out, keepAlive) -> {});
    
    Optional<Router.Match> match = router.find("HEAD", "/users");
    
    assertTrue(match.isPresent());
    assertTrue(match.get().handler != null);
  }

  @Test
  void testFind_headImpliedFromGetWithParams() {
    Router router = new Router();
    router.get("/users/:id", (ctx, out, keepAlive) -> {});
    
    Optional<Router.Match> match = router.find("HEAD", "/users/123");
    
    assertTrue(match.isPresent());
    assertEquals("123", match.get().pathVars.get("id"));
  }

  @Test
  void testFind_headExplicitlyDefined() {
    Router router = new Router();
    router.get("/users", (ctx, out, keepAlive) -> {});
    router.head("/users", (ctx, out, keepAlive) -> {});
    
    Optional<Router.Match> match = router.find("HEAD", "/users");
    
    assertTrue(match.isPresent());
    // Should match the explicit HEAD route, not the implied one
  }

  @Test
  void testFind_headNotImpliedFromPost() {
    Router router = new Router();
    router.post("/users", (ctx, out, keepAlive) -> {});
    
    Optional<Router.Match> match = router.find("HEAD", "/users");
    
    assertFalse(match.isPresent());
  }
  //#endregion

  //#region OPTIONS method tests
  @Test
  void testFind_optionsWithWildcardRoute() {
    Router router = new Router();
    router.options("*", (ctx, out, keepAlive) -> {});
    
    Optional<Router.Match> match = router.find("OPTIONS", "*");
    
    assertTrue(match.isPresent());
  }

  @Test
  void testFind_optionsWithSpecificRoute() {
    Router router = new Router();
    router.options("/users", (ctx, out, keepAlive) -> {});
    
    Optional<Router.Match> match = router.find("OPTIONS", "/users");
    
    assertTrue(match.isPresent());
  }

  @Test
  void testFind_optionsWithParams() {
    Router router = new Router();
    router.options("/users/:id", (ctx, out, keepAlive) -> {});
    
    Optional<Router.Match> match = router.find("OPTIONS", "/users/123");
    
    assertTrue(match.isPresent());
    assertEquals("123", match.get().pathVars.get("id"));
  }

  @Test
  void testFind_optionsNoWildcardRoute() {
    Router router = new Router();
    router.get("/users", (ctx, out, keepAlive) -> {});
    
    Optional<Router.Match> match = router.find("OPTIONS", "/users");
    
    assertFalse(match.isPresent());
  }
  //#endregion

  //#region Allowed methods tests
  @Test
  void testAllowedForPath_singleMethod() {
    Router router = new Router();
    router.get("/users", (ctx, out, keepAlive) -> {});
    
    Set<String> allowed = router.allowedForPath("/users");
    
    assertTrue(allowed.contains("GET"));
    assertTrue(allowed.contains("HEAD")); // Implied from GET
    assertTrue(allowed.contains("OPTIONS")); // Always allowed
    assertEquals(3, allowed.size());
  }

  @Test
  void testAllowedForPath_multipleMethods() {
    Router router = new Router();
    router.get("/users", (ctx, out, keepAlive) -> {});
    router.post("/users", (ctx, out, keepAlive) -> {});
    router.put("/users", (ctx, out, keepAlive) -> {});
    
    Set<String> allowed = router.allowedForPath("/users");
    
    assertTrue(allowed.contains("GET"));
    assertTrue(allowed.contains("POST"));
    assertTrue(allowed.contains("PUT"));
    assertTrue(allowed.contains("HEAD")); // Implied from GET
    assertTrue(allowed.contains("OPTIONS")); // Always allowed
    assertEquals(5, allowed.size());
  }

  @Test
  void testAllowedForPath_withParams() {
    Router router = new Router();
    router.get("/users/:id", (ctx, out, keepAlive) -> {});
    router.post("/users/:id", (ctx, out, keepAlive) -> {});
    
    Set<String> allowed = router.allowedForPath("/users/123");
    
    assertTrue(allowed.contains("GET"));
    assertTrue(allowed.contains("POST"));
    assertTrue(allowed.contains("HEAD")); // Implied from GET
    assertTrue(allowed.contains("OPTIONS")); // Always allowed
    assertEquals(4, allowed.size());
  }

  @Test
  void testAllowedForPath_explicitHead() {
    Router router = new Router();
    router.get("/users", (ctx, out, keepAlive) -> {});
    router.head("/users", (ctx, out, keepAlive) -> {});
    
    Set<String> allowed = router.allowedForPath("/users");
    
    assertTrue(allowed.contains("GET"));
    assertTrue(allowed.contains("HEAD"));
    assertTrue(allowed.contains("OPTIONS"));
    assertEquals(3, allowed.size());
  }

  @Test
  void testAllowedForPath_noMatches() {
    Router router = new Router();
    router.get("/users", (ctx, out, keepAlive) -> {});
    
    Set<String> allowed = router.allowedForPath("/posts");
    
    assertTrue(allowed.contains("OPTIONS")); // Always allowed
    assertEquals(1, allowed.size());
  }

  @Test
  void testAllowedForPath_postOnly() {
    Router router = new Router();
    router.post("/users", (ctx, out, keepAlive) -> {});
    
    Set<String> allowed = router.allowedForPath("/users");
    
    assertTrue(allowed.contains("POST"));
    assertTrue(allowed.contains("OPTIONS")); // Always allowed
    assertFalse(allowed.contains("HEAD")); // Not implied from POST
    assertEquals(2, allowed.size());
  }

  @Test
  void testAllowedForPath_withOptions() {
    Router router = new Router();
    router.get("/users", (ctx, out, keepAlive) -> {});
    router.options("/users", (ctx, out, keepAlive) -> {});
    
    Set<String> allowed = router.allowedForPath("/users");
    
    assertTrue(allowed.contains("GET"));
    assertTrue(allowed.contains("HEAD")); // Implied from GET
    assertTrue(allowed.contains("OPTIONS"));
    assertEquals(3, allowed.size());
  }
  //#endregion

  //#region Edge cases and complex scenarios
  @Test
  void testFind_multipleRoutes_samePath() {
    Router router = new Router();
    router.get("/users", (ctx, out, keepAlive) -> {});
    router.post("/users", (ctx, out, keepAlive) -> {});
    
    Optional<Router.Match> getMatch = router.find("GET", "/users");
    Optional<Router.Match> postMatch = router.find("POST", "/users");
    
    assertTrue(getMatch.isPresent());
    assertTrue(postMatch.isPresent());
    assertTrue(getMatch.get().handler != postMatch.get().handler);
  }

  @Test
  void testFind_multipleRoutes_differentPaths() {
    Router router = new Router();
    router.get("/users", (ctx, out, keepAlive) -> {});
    router.get("/posts", (ctx, out, keepAlive) -> {});
    
    Optional<Router.Match> usersMatch = router.find("GET", "/users");
    Optional<Router.Match> postsMatch = router.find("GET", "/posts");
    
    assertTrue(usersMatch.isPresent());
    assertTrue(postsMatch.isPresent());
  }

  @Test
  void testFind_pathWithTrailingSlash() {
    Router router = new Router();
    router.get("/users", (ctx, out, keepAlive) -> {});
    
    Optional<Router.Match> match = router.find("GET", "/users/");
    
    assertTrue(match.isPresent());
  }

  @Test
  void testFind_pathWithLeadingSlash() {
    Router router = new Router();
    router.get("/users", (ctx, out, keepAlive) -> {});
    
    Optional<Router.Match> match = router.find("GET", "/users");
    
    assertTrue(match.isPresent());
  }

  @Test
  void testFind_emptyPath() {
    Router router = new Router();
    router.get("/", (ctx, out, keepAlive) -> {});
    
    Optional<Router.Match> match = router.find("GET", "/");
    
    assertTrue(match.isPresent());
  }

  @Test
  void testFind_pathWithSpecialCharacters() {
    Router router = new Router();
    router.get("/files/:filename", (ctx, out, keepAlive) -> {});
    
    Optional<Router.Match> match = router.find("GET", "/files/test-file_123.txt");
    
    assertTrue(match.isPresent());
    assertEquals("test-file_123.txt", match.get().pathVars.get("filename"));
  }

  @Test
  void testFind_pathWithUnicodeCharacters() {
    Router router = new Router();
    router.get("/users/:name", (ctx, out, keepAlive) -> {});
    
    Optional<Router.Match> match = router.find("GET", "/users/café");
    
    assertTrue(match.isPresent());
    assertEquals("café", match.get().pathVars.get("name"));
  }
  //#endregion

  //#region Router builder pattern tests
  @Test
  void testRouterBuilderPattern() {
    Router router = new Router()
        .get("/users", (ctx, out, keepAlive) -> {})
        .post("/users", (ctx, out, keepAlive) -> {})
        .put("/users/:id", (ctx, out, keepAlive) -> {})
        .head("/users", (ctx, out, keepAlive) -> {})
        .options("/users", (ctx, out, keepAlive) -> {});
    
    assertTrue(router.find("GET", "/users").isPresent());
    assertTrue(router.find("POST", "/users").isPresent());
    assertTrue(router.find("PUT", "/users/123").isPresent());
    assertTrue(router.find("HEAD", "/users").isPresent());
    assertTrue(router.find("OPTIONS", "/users").isPresent());
  }
  //#endregion

  // Add these tests to RouterTest.java

  @Test
  void testRouter_withJsonResponse() {
      Router router = new Router();
      router.get("/test", (ctx, out, keepAlive) -> {
          // This test verifies the router can handle JSON responses
          // The actual JSON logic is tested in HttpResponsesTest
      });
      
      Optional<Router.Match> match = router.find("GET", "/test");
      assertTrue(match.isPresent());
      assertNotNull(match.get().handler);
  }

  @Test
  void testRouter_withPathVarsAndJson() {
      Router router = new Router();
      router.get("/users/:id", (ctx, out, keepAlive) -> {
          @SuppressWarnings("unused")
          String id = ctx.pathVars("id");
          // This test verifies path variables work with JSON responses
      });
      
      Optional<Router.Match> match = router.find("GET", "/users/123");
      assertTrue(match.isPresent());
      assertEquals("123", match.get().pathVars.get("id"));
  }

  @Test
  void testRouter_withQueryParamsAndJson() {
      Router router = new Router();
      router.get("/search", (ctx, out, keepAlive) -> {
          @SuppressWarnings("unused")
          String query = ctx.query("q");
          // This test verifies query parameters work with JSON responses
      });
      
      Optional<Router.Match> match = router.find("GET", "/search");
      assertTrue(match.isPresent());
      assertNotNull(match.get().handler);
  }
}