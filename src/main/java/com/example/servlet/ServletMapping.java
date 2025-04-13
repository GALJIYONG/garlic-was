package com.example.servlet;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class ServletMapping {
    private final Map<String, SimpleServlet> servletMap = new HashMap<>();

    public void addMapping(String urlPattern, SimpleServlet servlet) {
        log.info("Adding servlet mapping - Pattern: '{}', Servlet: {}", urlPattern, servlet.getClass().getName());
        servletMap.put(urlPattern, servlet);
    }

    public SimpleServlet getServlet(String path) {
        String cleanPath = path.replaceFirst("^/", "").split("\\?")[0];
        log.info("Looking for servlet mapping for path: '{}'", cleanPath);
        log.info("Available mappings: {}", servletMap.keySet());
        
        SimpleServlet servlet = servletMap.get(cleanPath);
        if (servlet != null) {
            log.info("Found servlet mapping for path: '{}'", cleanPath);
            return servlet;
        }

        log.info("No servlet mapping found for path: '{}'", cleanPath);
        return null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ServletMapping{");
        servletMap.forEach((pattern, servlet) -> 
            sb.append("\n  ").append(pattern).append(" -> ").append(servlet.getClass().getName()));
        sb.append("\n}");
        return sb.toString();
    }
} 