package com.example;

import com.example.config.Config;
import com.example.server.HttpServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.*;

public class WebServerTest {
    private HttpServer server;
    private Config config;
    private ExecutorService executorService;

    @Before
    public void setUp() throws Exception {
        config = Config.load("server-config.json");
        server = new HttpServer(config);
        executorService = Executors.newSingleThreadExecutor();
        executorService.submit(() -> {
            try {
                server.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        // 서버가 시작될 때까지 잠시 대기
        Thread.sleep(500);
    }

    @After
    public void tearDown() throws Exception {
        if (server != null && server.isRunning) {
            server.stop();
            executorService.shutdown();
            // 서버가 완전히 종료될 때까지 대기
            int maxAttempts = 10;
            int attempts = 0;
            while (server.isRunning && attempts < maxAttempts) {
                Thread.sleep(100);
                attempts++;
            }
        }
    }

    @Test
    public void testServerStart() throws Exception {
        assertTrue(server.isRunning);
    }

    @Test
    public void testServerStop() throws Exception {
        server.stop();
        Thread.sleep(100); // 서버가 중지될 때까지 잠시 대기
        assertFalse(server.isRunning);
    }

    @Test
    public void testServerPort() throws Exception {
        assertEquals(8080, config.getPort());
    }

    @Test
    public void testServerConnection() throws Exception {
        try (Socket socket = new Socket("localhost", config.getPort())) {
            assertTrue(socket.isConnected());
        }
    }

    @Test(expected = IOException.class)
    public void testServerAlreadyRunning() throws Exception {
        HttpServer anotherServer = new HttpServer(config);
        try {
            anotherServer.start(); // Should throw IOException
        } finally {
            if (anotherServer != null && anotherServer.isRunning) {
                anotherServer.stop();
            }
        }
    }

    // 1. Host 헤더 처리 테스트
    @Test
    public void testHostHeader() throws Exception {
        // localhost 테스트
        try (Socket socket = new Socket("localhost", config.getPort())) {
            String request = "GET / HTTP/1.1\r\n" +
                           "Host: localhost\r\n" +
                           "Connection: close\r\n\r\n";
            sendRequest(socket, request);
            String response = readResponse(socket);
            System.out.println("Localhost Response:\n" + response);
            assertTrue("Response should contain HTTP status line", response.contains("HTTP/1.1"));
            assertTrue("Response should contain HTML content", response.contains("<html"));
        }

        // example.com 테스트
        try (Socket socket = new Socket("localhost", config.getPort())) {
            String request = "GET / HTTP/1.1\r\n" +
                           "Host: example.com\r\n" +
                           "Connection: close\r\n\r\n";
            sendRequest(socket, request);
            String response = readResponse(socket);
            System.out.println("Example.com Response:\n" + response);
            assertTrue("Response should contain HTTP status line", response.contains("HTTP/1.1"));
            assertTrue("Response should contain HTML content", response.contains("<html"));
        }
    }

    // 2. 설정 파일 관리 테스트
    @Test
    public void testConfigFile() throws Exception {
        assertEquals(8080, config.getPort());
        assertTrue(config.isHostConfigured("localhost"));
        assertTrue(config.isHostConfigured("example.com"));
        assertEquals("webapp/www/localhost", config.getDocBase("localhost"));
        assertEquals("webapp/www/example", config.getDocBase("example.com"));
    }

    // 3. 오류 처리 테스트
    @Test
    public void testErrorHandling() throws Exception {
        // 404 테스트
        try (Socket socket = new Socket("localhost", config.getPort())) {
            String request = "GET /nonexistent HTTP/1.1\r\n" +
                           "Host: localhost\r\n" +
                           "Connection: close\r\n\r\n";
            sendRequest(socket, request);
            String response = readResponse(socket);
            System.out.println("404 Response:\n" + response);
            assertTrue("Response should contain 404 status", response.contains("HTTP/1.1 404 Not Found"));
            assertTrue("Response should contain HTML content", response.contains("<html"));
        }

        // 403 테스트 (상위 디렉토리 접근)
        try (Socket socket = new Socket("localhost", config.getPort())) {
            String request = "GET /../../etc/passwd HTTP/1.1\r\n" +
                           "Host: localhost\r\n" +
                           "Connection: close\r\n\r\n";
            sendRequest(socket, request);
            String response = readResponse(socket);
            System.out.println("403 Response:\n" + response);
            assertTrue("Response should contain 403 status", response.contains("HTTP/1.1 403 Forbidden"));
            assertTrue("Response should contain HTML content", response.contains("<html"));
        }

        // 500 테스트
        try (Socket socket = new Socket("localhost", config.getPort())) {
            String request = "GET /time?error=true HTTP/1.1\r\n" +
                           "Host: localhost\r\n" +
                           "Connection: close\r\n\r\n";
            sendRequest(socket, request);
            String response = readResponse(socket);
            System.out.println("500 Response:\n" + response);
            assertTrue("Response should contain 500 status", response.contains("HTTP/1.1 500 Internal Server Error"));
            assertTrue("Response should contain HTML content", response.contains("<html"));
        }
    }

    // 4. 보안 규칙 테스트
    @Test
    public void testSecurityRules() throws Exception {
        // .exe 파일 접근 테스트
        try (Socket socket = new Socket("localhost", config.getPort())) {
            String request = "GET /test.exe HTTP/1.1\r\n" +
                           "Host: localhost\r\n" +
                           "Connection: close\r\n\r\n";
            sendRequest(socket, request);
            String response = readResponse(socket);
            System.out.println("Security Rules Response:\n" + response);
            assertTrue("Response should contain 403 status", response.contains("HTTP/1.1 403 Forbidden"));
            assertTrue("Response should contain HTML content", response.contains("<html"));
        }
    }

    // 6. WAS 기능 테스트
    @Test
    public void testWAS() throws Exception {
        // TimeServlet 테스트
        try (Socket socket = new Socket("localhost", config.getPort())) {
            String request = "GET /time HTTP/1.1\r\n" +
                    "Host: localhost\r\n" +
                    "Connection: close\r\n\r\n";
            sendRequest(socket, request);
            String response = readResponse(socket);
            System.out.println("TimeServlet Response:\n" + response);
            assertTrue("Response should contain 200 status", response.contains("HTTP/1.1 200 OK"));
            assertTrue("Response should contain HTML content", response.contains("<html"));
            assertTrue("Response should contain time information", response.contains("Current Time"));
        }
    }

    private void sendRequest(Socket socket, String request) throws IOException {
        OutputStream out = socket.getOutputStream();
        out.write(request.getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    private String readResponse(Socket socket) throws IOException {
        BufferedReader in = new BufferedReader(
            new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
            response.append(line).append("\n");
        }
        return response.toString();
    }
} 