package com.example.http;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

public interface HttpResponse {
    void setHeader(String name, String value);
    void setContentType(String contentType);
    Writer getWriter() throws IOException;
    OutputStream getOutputStream() throws IOException;
} 