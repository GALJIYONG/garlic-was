package com.example.servlet;

import com.example.http.SimpleHttpRequest;
import com.example.http.SimpleHttpResponse;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
public class HelloServlet implements SimpleServlet {
    @Override
    public void service(SimpleHttpRequest request, SimpleHttpResponse response) throws Exception {
        log.info("HelloServlet: Processing hello request");
        
        response.setContentType("text/html; charset=UTF-8");
        
        try (var writer = response.getWriter()) {
            String html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>Hello Servlet</title>
                    <style>
                        body { 
                            font-family: Arial, sans-serif; 
                            text-align: center; 
                            margin-top: 50px;
                            background-color: #f0f0f0;
                        }
                        .container {
                            max-width: 600px;
                            margin: 0 auto;
                            padding: 20px;
                            background-color: white;
                            border-radius: 10px;
                            box-shadow: 0 0 10px rgba(0,0,0,0.1);
                        }
                        h1 { 
                            color: #333;
                            margin-bottom: 20px;
                        }
                        .message {
                            color: #666;
                            font-size: 1.2em;
                            margin: 20px 0;
                        }
                        .info {
                            color: #888;
                            font-size: 0.9em;
                            margin-top: 30px;
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <h1>Hello from SimpleServlet!</h1>
                        <div class="message">
                            Welcome to our simple web server!
                        </div>
                        <div class="info">
                            <p>Request URI: %s</p>
                            <p>Host: %s</p>
                            <p>Current Time: %s</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(
                    request.getRequestURI(),
                    request.getHost(),
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                );
            
            log.info("HelloServlet: Writing response");
            writer.write(html);
            writer.flush();
            log.info("HelloServlet: Response written and flushed");
        }
        
        log.info("HelloServlet: Response sent successfully");
    }
} 