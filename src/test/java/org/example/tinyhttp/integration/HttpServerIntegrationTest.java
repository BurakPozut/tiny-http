package org.example.tinyhttp.integration;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URL;

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
    static void stopServer() {
        if (server != null) {
            server.stop();
        }
    }
    
    private static Router createTestRouter() {
        return new Router()
            .get("/hello", (ctx, out, keepAlive) -> {
                String name = ctx.query("name");
                String msg = (name == null) ? "Hello World\n" : ("Hello " + name + "\n");
                org.example.tinyhttp.http.response.HttpResponses.writeText(out, 200, "OK", msg, keepAlive);
            })
            .get("/users/:id", (ctx, out, keepAlive) -> {
                String id = ctx.pathVars("id");
                org.example.tinyhttp.http.response.HttpResponses.writeText(out, 200, "OK", "user " + id + "\n", keepAlive);
            })
            .post("/echo", (ctx, out, keepAlive) -> {
                String ct = ctx.request().getHeaders().first("content-type", "application/octet-stream");
                org.example.tinyhttp.http.response.HttpResponses.writeRaw(out, 200, "OK", ct, ctx.request().getBody(), keepAlive);
            })
            .options("*", (ctx, out, keepAlive) -> {
                String allow = "GET,HEAD,POST,OPTIONS";
                org.example.tinyhttp.http.response.HttpResponses.writeText(out, 204, "No Content", "", keepAlive, new String[][]{{"Allow", allow}});
            });
    }

    @Test
    void testGetHello() throws IOException {
        URL url = URI.create(baseUrl + "/hello").toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
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
        connection.setRequestProperty("Content-Type", "text/plain");
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
}