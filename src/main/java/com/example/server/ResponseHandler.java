package com.example.server;

import com.example.config.Config;
import com.example.http.SimpleHttpResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
public class ResponseHandler {
    private final Config config;
    private String host;

    public ResponseHandler(Config config, String host) {
        this.config = config;
        this.host = host;
    }

    public void sendFile(SimpleHttpResponse response, Path filePath, String contentType) throws IOException {
        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            log.error("File not found: {}", filePath);
            throw new IOException("File not found: " + filePath);
        }

        response.setContentType(contentType);
        Files.copy(filePath, response.getOutputStream());
    }
} 