package org.example.tinyhttp.http.request;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class AcceptsTest {

    @Test
    void testWantsJson_withApplicationJson() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Accept", "application/json");
        
        assertTrue(Accepts.wantsJson(headers));
    }

    @Test
    void testWantsJson_withApplicationJsonAndOther() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Accept", "text/html, application/json, */*");
        
        assertTrue(Accepts.wantsJson(headers));
    }

    @Test
    void testWantsJson_withWildcard() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Accept", "*/*");
        
        assertTrue(Accepts.wantsJson(headers));
    }

    @Test
    void testWantsJson_withTextPlain() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Accept", "text/plain");
        
        assertFalse(Accepts.wantsJson(headers));
    }

    @Test
    void testWantsJson_withHtml() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Accept", "text/html");
        
        assertFalse(Accepts.wantsJson(headers));
    }

    @Test
    void testWantsJson_withNoAcceptHeader() {
        HttpHeaders headers = new HttpHeaders();
        
        assertFalse(Accepts.wantsJson(headers));
    }

    @Test
    void testWantsJson_withEmptyAcceptHeader() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Accept", "");
        
        assertFalse(Accepts.wantsJson(headers));
    }

    @Test
    void testWantsJson_caseInsensitive() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Accept", "APPLICATION/JSON");
        
        assertTrue(Accepts.wantsJson(headers));
    }

    @Test
    void testWantsJson_withJsonInMiddle() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Accept", "text/html, application/json, text/plain");
        
        assertTrue(Accepts.wantsJson(headers));
    }
}