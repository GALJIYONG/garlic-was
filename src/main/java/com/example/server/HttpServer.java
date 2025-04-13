package com.example.server;

import com.example.config.Config;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class HttpServer {
    private static final int THREAD_POOL_SIZE = 10;
    private final ServerSocket serverSocket;
    private final ExecutorService executorService;
    private final Config config;
    public volatile boolean isRunning;

    public HttpServer(Config config) throws IOException {
        this.config = config;
        this.serverSocket = new ServerSocket(config.getPort());
        this.executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        this.isRunning = true;
        log.info("Server started on port {}", config.getPort());
        log.info("Thread pool size: {}", THREAD_POOL_SIZE);
    }

    public void start() {
        while (isRunning) {
            try {
                Socket clientSocket = serverSocket.accept();
                executorService.execute(new RequestHandler(clientSocket, config));
            } catch (IOException e) {
                if (isRunning) {
                    log.error("Error accepting connection: {}", e.getMessage());
                    e.fillInStackTrace();
                }
            }
        }
    }

    public void stop() {
        isRunning = false;
        try {
            serverSocket.close();
            executorService.shutdown();
            log.info("Server stopped");
        } catch (IOException e) {
            log.error("Error closing server socket: {}", e.getMessage());
            e.fillInStackTrace();
        }
    }
} 