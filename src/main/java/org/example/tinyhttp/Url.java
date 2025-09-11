package org.example.tinyhttp;

import java.util.List;
import java.util.Map;

public final class Url {
    private final String rawTarget;     // e.g., /users/42?tab=activity
    private final String path;          // normalized, no query
    private final Map<String,List<String>> query;  // decoded

    public Url(String rawTarget, String path, Map<String,List<String>> query) {
        this.rawTarget = rawTarget;
        this.path = path;
        this.query = query;
    }

    public String rawTarget() { return rawTarget; }
    public String path() { return path; }
    public Map<String,List<String>> query() { return query; }

    public String q1(String key) {       // first query value or null
        var v = query.get(key);
        return (v == null || v.isEmpty()) ? null : v.get(0);
    }
}