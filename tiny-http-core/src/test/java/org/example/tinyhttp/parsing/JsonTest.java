package org.example.tinyhttp.parsing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import java.util.Map;

public class JsonTest {

    @Test
    void testCreateResponse_singleKeyValue() {
        Map<String, String> response = Json.createResponse("message", "Hello World");
        
        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals("Hello World", response.get("message"));
    }

    @Test
    void testCreateResponse_withId() {
        Map<String, String> response = Json.createResponse("id", "123");
        
        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals("123", response.get("id"));
    }

    @Test
    void testCreateResponse_withEmptyValue() {
        Map<String, String> response = Json.createResponse("status", "");
        
        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals("", response.get("status"));
    }

    @Test
    void testCreateResponse_withNullValue() {
        Map<String, String> response = Json.createResponse("data", null);
        
        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals(null, response.get("data"));
    }

    @Test
    void testMapper_isNotNull() {
        assertNotNull(Json.mapper);
    }

    @Test
    void testMapper_serialization() throws Exception {
        Map<String, String> data = Json.createResponse("message", "test");
        String json = Json.mapper.writeValueAsString(data);
        
        assertTrue(json.contains("\"message\""));
        assertTrue(json.contains("\"test\""));
    }

    @Test
    void testMapper_deserialization() throws Exception {
        String json = "{\"message\":\"Hello World\"}";
        @SuppressWarnings("unchecked")
        Map<String, String> data = Json.mapper.readValue(json, Map.class);
        
        assertEquals("Hello World", data.get("message"));
    }
}
