package com.example;

import com.example.config.Config;
import com.example.exception.WebServerException;
import com.example.server.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        try {
            Config config = Config.load("server-config.json");
            HttpServer server = new HttpServer(config);
            server.start();
        } catch (WebServerException | IOException e) {
            logger.error("Error starting server", e);
            e.printStackTrace();
        }
    }
}