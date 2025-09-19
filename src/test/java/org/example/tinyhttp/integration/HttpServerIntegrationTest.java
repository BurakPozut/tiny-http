package org.example.tinyhttp.integration;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URL;

import org.example.tinyhttp.http.request.Accepts;
import org.example.tinyhttp.http.response.Cors;
import org.example.tinyhttp.http.response.HttpResponses;
import org.example.tinyhttp.parsing.Json;
import org.example.tinyhttp.routing.Router;
import org.example.tinyhttp.server.HttpServerInstance;
import org.junit.jupiter.api.AfterAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class HttpServerIntegrationTest {
    
    private static HttpServerInstance server;
    private static int serverPort;
    private static String baseUrl;
    
    @BeforeAll
    @SuppressWarnings("unused")
    static void startServer() throws IOException {
        // Find a free port
        try (ServerSocket socket = new ServerSocket(0)) {
            serverPort = socket.getLocalPort();
        }
        
        baseUrl = "http://localhost:" + serverPort;
        
        // Create test router
        Router testRouter = createTestRouter();
        
        // Start the REAL server instance
        server = new HttpServerInstance(serverPort, testRouter);
        server.start();
        
        // Give the server a moment to start
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    @AfterAll
    @SuppressWarnings("unused")
    static void stopServer() {
        if (server != null) {
            server.stop();
        }
    }
    
    private static Router createTestRouter() {
        return new Router()
            .get("/hello", (ctx, out, keepAlive) -> {
                String name = ctx.query("name");
                String msg = (name == null) ? "Hello World" : ("Hello " + name);
                String[][] corsExtraHeaders = Cors.combinewithExtraHeaders(null, ctx.request().getHeaders());
                if(Accepts.wantsJson(ctx.request().getHeaders())) {
                  var json = Json.createResponse("message", msg);
                  // System.out.println("in GET method JSON msg: " + msg);
                  HttpResponses.writeJson(out, 200, "OK", json, keepAlive, corsExtraHeaders);
                }
                else{
                  HttpResponses.writeText(out, 200, "OK", msg + "\n", keepAlive, corsExtraHeaders);
                }
            })
            .get("/users/:id", (ctx, out, keepAlive) -> {
                String id = ctx.pathVars("id");
                
                // Match your main server exactly
                String[][] corsExtraHeaders = org.example.tinyhttp.http.response.Cors.combinewithExtraHeaders(null, ctx.request().getHeaders());
                
                var m = org.example.tinyhttp.http.request.RequestMetrics.get();
                boolean prefersJson = (m != null) ? m.prefersJson : false;
                
                if (prefersJson) {
                    java.util.Map<String, String> response = org.example.tinyhttp.parsing.Json.createResponse("id", id);
                    HttpResponses.writeJson(out, 200, "OK", response, keepAlive, corsExtraHeaders);
                } else {
                    HttpResponses.writeText(out, 200, "OK", "user " + id + "\n", keepAlive, corsExtraHeaders);
                }
            })
            .post("/echo", (ctx, out, keepAlive) -> {
                String ct = ctx.request().getHeaders().first("content-type", "application/octet-stream");
                
                // Match your main server exactly
                String[][] corsExtraHeaders = org.example.tinyhttp.http.response.Cors.combinewithExtraHeaders(null, ctx.request().getHeaders());
                
                var m = org.example.tinyhttp.http.request.RequestMetrics.get();
                boolean prefersJson = (m != null) ? m.prefersJson : false;
                
                if (prefersJson && ct.contains("application/json")) {
                    var node = org.example.tinyhttp.parsing.Json.mapper.readTree(ctx.request().getBody());
                    HttpResponses.writeJson(out, 200, "OK", node, keepAlive, corsExtraHeaders);
                } else {
                    HttpResponses.writeRaw(out, 200, "OK", ct, ctx.request().getBody(), keepAlive, corsExtraHeaders);
                }
            });
    }

    @Test
    void testGetHello() throws IOException {
        URL url = URI.create(baseUrl + "/hello").toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "text/plain");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

        
        int responseCode = connection.getResponseCode();
        assertEquals(200, responseCode);
        
        String responseBody = new String(connection.getInputStream().readAllBytes());
        assertEquals("Hello World\n", responseBody);
        
        // Check headers
        String serverHeader = connection.getHeaderField("Server");
        assertNotNull(serverHeader);
        
        String dateHeader = connection.getHeaderField("Date");
        assertNotNull(dateHeader);
        
        String connectionHeader = connection.getHeaderField("Connection");
        assertNotNull(connectionHeader);
    }
    
    @Test
    void testGetHelloWithName() throws IOException {
        URL url = URI.create(baseUrl + "/hello?name=TestUser").toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "text/plain");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        
        int responseCode = connection.getResponseCode();
        assertEquals(200, responseCode);
        
        String responseBody = new String(connection.getInputStream().readAllBytes());
        assertEquals("Hello TestUser\n", responseBody);
    }
    
    @Test
    void testGetUsersWithId() throws IOException {
        URL url = URI.create(baseUrl + "/users/123").toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "text/plain");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        
        int responseCode = connection.getResponseCode();
        assertEquals(200, responseCode);
        
        String responseBody = new String(connection.getInputStream().readAllBytes());
        assertEquals("user 123\n", responseBody);
    }
    
    @Test
    void testPostEcho() throws IOException {
        URL url = URI.create(baseUrl + "/echo").toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        // connection.setRequestProperty("Content-Type", "text/plain");
        connection.setDoOutput(true);
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        
        String testData = "Hello from integration test!";
        connection.getOutputStream().write(testData.getBytes());
        
        int responseCode = connection.getResponseCode();
        assertEquals(200, responseCode);
        
        String responseBody = new String(connection.getInputStream().readAllBytes());
        assertEquals(testData, responseBody);
    }
    
    @Test
    void testNotFound() throws IOException {
        URL url = URI.create(baseUrl + "/nonexistent").toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        
        int responseCode = connection.getResponseCode();
        assertEquals(404, responseCode);
    }
    
    @Test
    void testMethodNotAllowed() throws IOException {
        URL url = URI.create(baseUrl + "/hello").toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("DELETE");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        
        int responseCode = connection.getResponseCode();
        assertEquals(405, responseCode);
        
        String allowHeader = connection.getHeaderField("Allow");
        assertNotNull(allowHeader);
        assertTrue(allowHeader.contains("GET"));
    }

    @Test
    void testGetHelloWithJsonAccept() throws IOException {
        URL url = URI.create(baseUrl + "/hello?name=TestUser").toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/json");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        
        int responseCode = connection.getResponseCode();
        assertEquals(200, responseCode);
        
        String contentType = connection.getHeaderField("Content-Type");
        assertTrue(contentType.contains("application/json"));
        
        String responseBody = new String(connection.getInputStream().readAllBytes());
        assertTrue(responseBody.contains("\"message\""));
        assertTrue(responseBody.contains("Hello TestUser"));
    }

    @Test
    void testGetHelloWithJsonAcceptNoName() throws IOException {
        URL url = URI.create(baseUrl + "/hello").toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/json");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        
        int responseCode = connection.getResponseCode();
        assertEquals(200, responseCode);
        
        String contentType = connection.getHeaderField("Content-Type");
        assertTrue(contentType.contains("application/json"));
        
        String responseBody = new String(connection.getInputStream().readAllBytes());
        assertTrue(responseBody.contains("\"message\""));
        assertTrue(responseBody.contains("Hello World"));
    }

    @Test
    void testGetUsersWithJsonAccept() throws IOException {
        URL url = URI.create(baseUrl + "/users/123").toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/json");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        
        int responseCode = connection.getResponseCode();
        assertEquals(200, responseCode);
        
        String contentType = connection.getHeaderField("Content-Type");
        assertTrue(contentType.contains("application/json"));
        
        String responseBody = new String(connection.getInputStream().readAllBytes());
        assertTrue(responseBody.contains("\"id\""));
        assertTrue(responseBody.contains("123"));
    }

    @Test
    void testPostEchoWithJsonAccept() throws IOException {
        URL url = URI.create(baseUrl + "/echo").toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "application/json");
        connection.setDoOutput(true);
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        
        String testData = "{\"foo\":42,\"bar\":\"baz\"}";
        connection.getOutputStream().write(testData.getBytes());
        
        int responseCode = connection.getResponseCode();
        assertEquals(200, responseCode);
        
        String contentType = connection.getHeaderField("Content-Type");
        assertTrue(contentType.contains("application/json"));
        
        String responseBody = new String(connection.getInputStream().readAllBytes());
        assertEquals(testData, responseBody);
    }

    @Test
    void testPostEchoWithJsonAcceptComplex() throws IOException {
        URL url = URI.create(baseUrl + "/echo").toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "application/json");
        connection.setDoOutput(true);
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        
        String testData = "{\"name\":\"John\",\"age\":30,\"city\":\"New York\"}";
        connection.getOutputStream().write(testData.getBytes());
        
        int responseCode = connection.getResponseCode();
        assertEquals(200, responseCode);
        
        String contentType = connection.getHeaderField("Content-Type");
        assertTrue(contentType.contains("application/json"));
        
        String responseBody = new String(connection.getInputStream().readAllBytes());
        assertEquals(testData, responseBody);
    }

    @Test
    void testJsonVsTextResponse() throws IOException {
        // Test same endpoint with different Accept headers
        URL url = URI.create(baseUrl + "/hello?name=Test").toURL();
        
        // Test JSON response
        HttpURLConnection jsonConn = (HttpURLConnection) url.openConnection();
        jsonConn.setRequestMethod("GET");
        jsonConn.setRequestProperty("Accept", "application/json");
        jsonConn.setConnectTimeout(5000);
        jsonConn.setReadTimeout(5000);
        
        int jsonCode = jsonConn.getResponseCode();
        assertEquals(200, jsonCode);
        
        String jsonContentType = jsonConn.getHeaderField("Content-Type");
        assertTrue(jsonContentType.contains("application/json"));
        
        String jsonBody = new String(jsonConn.getInputStream().readAllBytes());
        assertTrue(jsonBody.contains("\"message\""));
        
        // Test text response
        HttpURLConnection textConn = (HttpURLConnection) url.openConnection();
        textConn.setRequestMethod("GET");
        textConn.setRequestProperty("Accept", "text/plain");
        textConn.setConnectTimeout(5000);
        textConn.setReadTimeout(5000);
        
        int textCode = textConn.getResponseCode();
        assertEquals(200, textCode);
        
        String textContentType = textConn.getHeaderField("Content-Type");
        assertTrue(textContentType.contains("text/plain"));
        
        String textBody = new String(textConn.getInputStream().readAllBytes());
        assertEquals("Hello Test\n", textBody);
    }

    @Test
    void testCorsPreflight() throws IOException {
        URL url = URI.create(baseUrl + "/hello").toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("OPTIONS");
        connection.setRequestProperty("Origin", "http://localhost:3000");
        connection.setRequestProperty("Access-Control-Request-Method", "GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        
        int responseCode = connection.getResponseCode();
        assertEquals(204, responseCode);
        
        // Check CORS headers
        String allowOrigin = connection.getHeaderField("Access-Control-Allow-Origin");
        assertNotNull(allowOrigin);
        
        String allowMethods = connection.getHeaderField("Access-Control-Allow-Methods");
        assertNotNull(allowMethods);
        
        String allowHeader = connection.getHeaderField("Allow");
        assertNotNull(allowHeader);
        assertTrue(allowHeader.contains("GET"));
    }

    // @Test
    // void testCorsActualRequest() throws IOException {
    //     URL url = URI.create(baseUrl + "/hello").toURL();
    //     HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    //     connection.setRequestMethod("GET");
    //     connection.setRequestProperty("Origin", "http://localhost:3000");
    //     connection.setConnectTimeout(5000);
    //     connection.setReadTimeout(5000);
        
    //     int responseCode = connection.getResponseCode();
    //     assertEquals(200, responseCode);
        
    //     // Check CORS headers are present
    //     String allowOrigin = connection.getHeaderField("Access-Control-Allow-Origin");
    //     assertNotNull(allowOrigin);
        
    //     String responseBody = new String(connection.getInputStream().readAllBytes());
    //     assertTrue(responseBody.contains("\"message\""));
    //     assertTrue(responseBody.contains("Hello World"));
    // }

    @Test
    void testOptionsWildcard() throws IOException {
        URL url = URI.create(baseUrl + "/").toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("OPTIONS");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        
        int responseCode = connection.getResponseCode();
        assertEquals(204, responseCode);
        
        // Should return all server methods
        String allowHeader = connection.getHeaderField("Allow");
        assertNotNull(allowHeader);
        assertTrue(allowHeader.contains("GET"));
        assertTrue(allowHeader.contains("POST"));
        assertTrue(allowHeader.contains("OPTIONS"));
    }
}