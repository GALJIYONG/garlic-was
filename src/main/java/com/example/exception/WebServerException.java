package com.example.exception;

import lombok.Getter;

@Getter
public class WebServerException extends RuntimeException {
    private final int statusCode;
    private final String statusMessage;

    public WebServerException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
        this.statusMessage = message;
    }
}