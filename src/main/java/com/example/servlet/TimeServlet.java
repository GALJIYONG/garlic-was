package com.example.servlet;

import com.example.http.SimpleHttpRequest;
import com.example.http.SimpleHttpResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
public class TimeServlet implements SimpleServlet {
    @Override
    public void service(SimpleHttpRequest request, SimpleHttpResponse response) throws Exception {
        log.info("TimeServlet: Processing time request");

        String error = request.getQueryString();
        if (error != null && error.contains("error=true")) {
            log.info("TimeServlet: Simulating error");
            throw new Exception("500 에러 테스트");
        }
        
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        response.setContentType("text/html; charset=UTF-8");
        
        String html = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Current Time</title>
                <style>
                    body { font-family: Arial, sans-serif; text-align: center; margin-top: 50px; }
                    h1 { color: #333; }
                    .time { color: #666; font-size: 24px; margin: 20px; }
                </style>
            </head>
            <body>
                <h1>Current Time</h1>
                <div class="time">%s</div>
                <p>Current Time: %s</p>
            </body>
            </html>
            """.formatted(time, time);
        
        try (OutputStreamWriter writer = new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8)) {
            writer.write(html);
            writer.flush();
        }
        
        log.info("TimeServlet: Response sent with time: {}", time);
    }
} 