package com.example.exception;

import com.example.config.Config;
import com.example.http.HttpStatusError;
import com.example.http.SimpleHttpResponse;
import com.example.server.ResponseHandler;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
public class ExceptionHandler {
    private final Config config;
    private final ResponseHandler responseHandler;

    public ExceptionHandler(Config config, ResponseHandler responseHandler) {
        this.config = config;
        this.responseHandler = responseHandler;
    }

    public void handleException(Exception e, SimpleHttpResponse response, String host) throws IOException {
        try {
            HttpStatusError error = determineError(e);
            sendErrorPage(error, response, host);
        } catch (IOException ex) {
            log.error("Error handling exception: {}", ex.getMessage());
            sendDefaultError(response, HttpStatusError.INTERNAL_SERVER_ERROR);
        }
    }

    private HttpStatusError determineError(Exception e) {
        if (e instanceof WebServerException we) {
            return HttpStatusError.fromStatusCode(we.getStatusCode());
        }
        return HttpStatusError.INTERNAL_SERVER_ERROR;
    }

    private void sendErrorPage(HttpStatusError error, SimpleHttpResponse response, String host) throws IOException {
        String errorPagePath = config.getErrorPage(host, error.getStatusCode());
        Path errorPage = Paths.get("webapp", "www", host, errorPagePath);
        
        if (Files.exists(errorPage)) {
            log.info("Sending custom error page for host {}: {}", host, errorPage);
            response.setStatus(error.getStatusCode(), error.getMessage());
            response.setContentType("text/html");
            Files.copy(errorPage, response.getOutputStream());
        } else {
            sendDefaultError(response, error);
        }
    }

    private void sendDefaultError(SimpleHttpResponse response, HttpStatusError error) throws IOException {
        response.setStatus(error.getStatusCode(), error.getMessage());
        response.setContentType("text/html");
        String html = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>%d %s</title>
                <style>
                    body { font-family: Arial, sans-serif; text-align: center; margin-top: 50px; }
                    h1 { color: #333; }
                    .error-info { color: #666; margin: 20px; }
                </style>
            </head>
            <body>
                <h1>%d %s</h1>
                <div class="error-info">
                    <p>%s</p>
                </div>
            </body>
            </html>
            """.formatted(
                error.getStatusCode(),
                error.getMessage(),
                error.getStatusCode(),
                error.getMessage(),
                error.getMessage()
            );
        response.getWriter().write(html);
    }
}