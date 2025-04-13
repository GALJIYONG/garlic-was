package com.example.http;

import com.example.config.Config;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class SimpleHttpResponse implements HttpResponse, AutoCloseable {
    private static final String CRLF = "\r\n";
    private static final DateTimeFormatter HTTP_DATE_FORMATTER = 
        DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z");

    private final OutputStream outputStream;
    private final Map<String, String> headers;
    private final Config config;
    private String contentType;
    private int statusCode;
    private String statusMessage;
    private boolean committed;
    private String host;
    private boolean headersSent;

    public SimpleHttpResponse(OutputStream outputStream, Config config, String host) {
        this.outputStream = outputStream;
        this.config = config;
        this.host = host;
        this.headers = new HashMap<>();
        this.statusCode = 200;
        this.statusMessage = "OK";
        this.committed = false;
        this.headersSent = false;
    }

    public void setHost(String host) {
        this.host = host;
        headers.put("Host", host);
    }

    public void setStatus(int statusCode, String statusMessage) {
        this.statusCode = statusCode;
        this.statusMessage = statusMessage;
    }

    @Override
    public void close() throws IOException {
        if (!committed) {
            sendHeaders();
        }
        outputStream.close();
    }

    @Override
    public void setHeader(String name, String value) {
        headers.put(name, value);
    }

    @Override
    public void setContentType(String contentType) {
        this.contentType = contentType;
        setHeader("Content-Type", contentType);
    }

    @Override
    public Writer getWriter() throws IOException {
        if (!headersSent) {
            sendHeaders();
        }
        return new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        if (!headersSent) {
            sendHeaders();
        }
        return outputStream;
    }

    private void sendHeaders() throws IOException {
        if (headersSent) {
            return;
        }

        StringBuilder headerBuilder = new StringBuilder();
        headerBuilder.append("HTTP/1.1 ").append(statusCode).append(" ").append(statusMessage).append(CRLF);
        
        if (contentType != null) {
            headerBuilder.append("Content-Type: ").append(contentType).append(CRLF);
        }
        
        headerBuilder.append("Date: ").append(ZonedDateTime.now().format(HTTP_DATE_FORMATTER)).append(CRLF);
        
        for (Map.Entry<String, String> header : headers.entrySet()) {
            headerBuilder.append(header.getKey()).append(": ").append(header.getValue()).append(CRLF);
        }
        
        headerBuilder.append(CRLF);
        outputStream.write(headerBuilder.toString().getBytes(StandardCharsets.UTF_8));
        headersSent = true;
        committed = true;
    }
} 