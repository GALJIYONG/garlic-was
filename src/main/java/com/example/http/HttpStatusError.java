package com.example.http;

import lombok.Getter;

@Getter
public enum HttpStatusError {
    BAD_REQUEST(400, "Bad Request"),
    FORBIDDEN(403, "Forbidden"),
    NOT_FOUND(404, "Not Found"),
    INTERNAL_SERVER_ERROR(500, "Internal Server Error");

    private final int statusCode;
    private final String message;

    HttpStatusError(int statusCode, String message) {
        this.statusCode = statusCode;
        this.message = message;
    }

    public static HttpStatusError fromStatusCode(int statusCode) {
        for (HttpStatusError error : values()) {
            if (error.getStatusCode() == statusCode) {
                return error;
            }
        }
        return INTERNAL_SERVER_ERROR;
    }
} 