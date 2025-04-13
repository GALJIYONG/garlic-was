package com.example.server;

import com.example.config.Config;
import com.example.exception.ExceptionHandler;
import com.example.exception.WebServerException;
import com.example.http.SimpleHttpRequest;
import com.example.http.SimpleHttpResponse;
import com.example.servlet.ServletMapping;
import com.example.servlet.SimpleServlet;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Slf4j
public class RequestHandler implements Runnable {
    private static final Map<String, String> CONTENT_TYPES = Map.of(
        ".html", "text/html; charset=UTF-8",
        ".css", "text/css",
        ".js", "application/javascript",
        ".png", "image/png",
        ".jpg", "image/jpeg",
        ".jpeg", "image/jpeg"
    );
    private final Socket clientSocket;
    private final Config config;
    private final ExceptionHandler exceptionHandler;
    private final ServletMapping servletMapping;

    public RequestHandler(Socket clientSocket, Config config) {
        this.clientSocket = clientSocket;
        this.config = config;
        ResponseHandler responseHandler = new ResponseHandler(config, null);
        this.exceptionHandler = new ExceptionHandler(config, responseHandler);
        this.servletMapping = new ServletMapping();
        
        // Load servlets from config
        loadServlets();
    }

    private void loadServlets() {
        log.info("Loading servlets from config...");
        List<Config.ServletConfig> servletConfigs = config.getServlets();
        log.info("Found {} servlet configurations", servletConfigs.size());
        
        for (Config.ServletConfig servletConfig : servletConfigs) {
            try {
                log.info("Loading servlet: {} -> {}", servletConfig.name(), servletConfig.className());
                log.info("URL patterns: {}", servletConfig.urlPatterns());
                
                Class<?> servletClass = Class.forName(servletConfig.className());
                SimpleServlet servlet = (SimpleServlet) servletClass.getDeclaredConstructor().newInstance();
                
                for (String urlPattern : servletConfig.urlPatterns()) {
                    String cleanPattern = urlPattern.replaceFirst("^/", "").replaceAll("/$", "");
                    log.info("Adding mapping: '{}' -> {}", cleanPattern, servletConfig.name());
                    servletMapping.addMapping(cleanPattern, servlet);
                }
            } catch (Exception e) {
                log.error("Failed to load servlet {}: {}", servletConfig.name(), e.getMessage());
                e.fillInStackTrace();
            }
        }
        
        log.info("Servlet loading completed. Current mappings: {}", servletMapping);
    }

    @Override
    public void run() {
        try (Socket socket = clientSocket;
             SimpleHttpResponse response = new SimpleHttpResponse(socket.getOutputStream(), config, null)) {
            
            SimpleHttpRequest request = new SimpleHttpRequest(socket);
            log.info("Request received - Method: {}, Path: {}, Host: {}", 
                    request.getMethod(), request.getPath(), request.getHost());
            
            log.info("Current servlet mappings: {}", servletMapping);
            
            try {
                handleRequest(request, response);
            } catch (Exception e) {
                log.error("Request handling failed: {}", e.getMessage());
                exceptionHandler.handleException(e, response, request.getHost());
            }
        } catch (IOException e) {
            log.error("Socket error: {}", e.getMessage());
        }
    }

    private void handleRequest(SimpleHttpRequest request, SimpleHttpResponse response) throws IOException {
        String host = request.getHost();
        String path = request.getPath();
        
        // Host 검증
        if (!config.isHostConfigured(host)) {
            throw new WebServerException(400, "Invalid host: " + host);
        }

        SimpleServlet servlet = servletMapping.getServlet(path);
        if (servlet != null) {
            try {
                log.info("Handling servlet request: {} -> {}", path, servlet.getClass().getName());
                servlet.service(request, response);
                return;
            } catch (Exception e) {
                log.error("Servlet execution failed: {}", e.getMessage());
                throw new WebServerException(500, "Servlet execution failed: " + e.getMessage());
            }
        }
        
        // 보안 검사
        if (isForbiddenPath(path)) {
            throw new WebServerException(403, "Access forbidden: " + path);
        }
        
        // 파일 처리
        Path docBase = Path.of(config.getDocBase(host));
        Path filePath = docBase.resolve(path.substring(1)).normalize();
        
        // 디렉터리 트래버설 방지
        if (!isPathSafe(docBase, filePath)) {
            log.warn("Directory traversal attempt detected - Host: {}, Path: {}", host, path);
            throw new WebServerException(403, "Access denied: Path traversal attempt");
        }
        
        if (Files.exists(filePath)) {
            if (Files.isDirectory(filePath)) {
                serveWelcomeFile(response, host);
            } else {
                serveFile(response, filePath);
            }
        } else {
            throw new WebServerException(404, "File not found: " + path);
        }
    }
    
    private void serveWelcomeFile(SimpleHttpResponse response, String host) throws IOException {
        String welcomeFile = config.getWelcomeFile(host);
        Path docBase = Path.of(config.getDocBase(host));
        Path welcomeFilePath = docBase.resolve(welcomeFile);
        
        if (!Files.exists(welcomeFilePath)) {
            createDefaultWelcomeFile(welcomeFilePath, host);
        }
        
        response.setContentType("text/html; charset=UTF-8");
        Files.copy(welcomeFilePath, response.getOutputStream());
    }
    
    private void createDefaultWelcomeFile(Path welcomeFilePath, String host) throws IOException {
        String html = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Welcome to %s</title>
                <style>
                    body { font-family: Arial, sans-serif; text-align: center; margin-top: 50px; }
                    h1 { color: #333; }
                    .host-info { color: #666; margin: 20px; }
                </style>
            </head>
            <body>
                <h1>Welcome to %s</h1>
                <div class="host-info">
                    <p>You are accessing: %s</p>
                    <p><a href="/time">Check current time</a></p>
                    <p><a href="/time?error=true">Test 500 error</a></p>
                </div>
            </body>
            </html>
            """.formatted(host, host, host);
        
        Files.createDirectories(welcomeFilePath.getParent());
        Files.writeString(welcomeFilePath, html);
    }
    
    private void serveFile(SimpleHttpResponse response, Path filePath) throws IOException {
        String contentType = determineContentType(filePath);
        response.setContentType(contentType);
        Files.copy(filePath, response.getOutputStream());
    }
    
    private String determineContentType(Path filePath) {
        return CONTENT_TYPES.entrySet().stream()
                .filter(entry -> filePath.toString().endsWith(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElseGet(() -> {
                    try {
                        String probed = Files.probeContentType(filePath);
                        return probed != null ? probed : "application/octet-stream";
                    } catch (IOException e) {
                        log.warn("Failed to probe content type for {}: {}", filePath, e.getMessage());
                        return "application/octet-stream";
                    }
                });
    }
    
    private boolean isForbiddenPath(String path) {
        return config.isPathForbidden(path) ||
               config.getSecurity().forbiddenExtensions().stream().anyMatch(path::endsWith) ||
               config.getSecurity().forbiddenPaths().stream().anyMatch(path::equals);
    }

    private boolean isPathSafe(Path docBase, Path requestedPath) {
        // 1. 정규화된 경로가 docBase를 벗어나는지 확인
        if (!requestedPath.startsWith(docBase)) {
            return false;
        }

        // 2. 심볼릭 링크 검사
        try {
            if (Files.isSymbolicLink(requestedPath)) {
                Path realPath = requestedPath.toRealPath();
                return realPath.startsWith(docBase);
            }
        } catch (IOException e) {
            log.warn("Failed to resolve symbolic link: {}", e.getMessage());
            return false;
        }

        // 3. 상위 디렉터리 참조 검사
        String normalizedPath = requestedPath.toString();
        if (normalizedPath.contains("..") || normalizedPath.contains("~")) {
            return false;
        }

        // 4. 파일 권한 검사
        try {
            if (Files.exists(requestedPath) && !Files.isReadable(requestedPath)) {
                return false;
            }
        } catch (SecurityException e) {
            log.warn("Security exception while checking file permissions: {}", e.getMessage());
            return false;
        }

        return true;
    }

} 