package com.example.http;

public interface HttpRequest {
    String getMethod();
    String getPath();
    String getHost();
} 