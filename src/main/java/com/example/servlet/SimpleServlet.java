package com.example.servlet;

import com.example.http.SimpleHttpRequest;
import com.example.http.SimpleHttpResponse;

public interface SimpleServlet {
    void service(SimpleHttpRequest request, SimpleHttpResponse response) throws Exception;
} 