package org.example.tinyhttp.http.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class RequestMetricsTest {

    @Test
    void testSetAndGet() {
        RequestMetrics metrics = new RequestMetrics("test-id", "GET", "/test", "127.0.0.1", System.nanoTime());
        RequestMetrics.set(metrics);
        
        RequestMetrics retrieved = RequestMetrics.get();
        assertEquals(metrics, retrieved);
        assertEquals("test-id", retrieved.requestId);
        assertEquals("GET", retrieved.method);
        assertEquals("/test", retrieved.path);
        assertEquals("127.0.0.1", retrieved.remote);
    }

    @Test
    void testClear() {
        RequestMetrics metrics = new RequestMetrics("test-id", "GET", "/test", "127.0.0.1", System.nanoTime());
        RequestMetrics.set(metrics);
        
        RequestMetrics.clear();
        
        assertNull(RequestMetrics.get());
    }

    @Test
    void testPrefersJson_defaultFalse() {
        RequestMetrics metrics = new RequestMetrics("test-id", "GET", "/test", "127.0.0.1", System.nanoTime());
        
        assertFalse(metrics.prefersJson);
    }

    @Test
    void testPrefersJson_setTrue() {
        RequestMetrics metrics = new RequestMetrics("test-id", "GET", "/test", "127.0.0.1", System.nanoTime());
        metrics.prefersJson = true;
        
        assertTrue(metrics.prefersJson);
    }

    @Test
    void testThreadLocalIsolation() throws InterruptedException {
        RequestMetrics metrics1 = new RequestMetrics("id1", "GET", "/test1", "127.0.0.1", System.nanoTime());
        RequestMetrics metrics2 = new RequestMetrics("id2", "POST", "/test2", "127.0.0.1", System.nanoTime());
        
        RequestMetrics.set(metrics1);
        
        Thread thread = new Thread(() -> {
            RequestMetrics.set(metrics2);
            assertEquals(metrics2, RequestMetrics.get());
        });
        
        thread.start();
        thread.join();
        
        // Original thread should still have metrics1
        assertEquals(metrics1, RequestMetrics.get());
    }
}