package com.example.http;

import lombok.Getter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

@Getter
public class SimpleHttpRequest implements HttpRequest {
    private final String method;
    private final String path;
    private final String protocol;
    private final Map<String, String> headers;
    private final Map<String, String> parameters;
    private final String queryString;
    private String host;
    private int port;
    private String hostWithPort;

    public SimpleHttpRequest(Socket socket) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.headers = new HashMap<>();
        this.parameters = new HashMap<>();

        // Parse request line
        String requestLine = reader.readLine();
        if (requestLine == null || requestLine.isEmpty()) {
            throw new IOException("Invalid request: empty request line");
        }

        String[] parts = requestLine.split(" ");
        if (parts.length != 3) {
            throw new IOException("Invalid request line format");
        }

        this.method = parts[0];
        String fullPath = parts[1];
        
        this.protocol = parts[2];

        int queryIndex = fullPath.indexOf('?');
        if (queryIndex != -1) {
            this.path = fullPath.substring(0, queryIndex);
            this.queryString = fullPath.substring(queryIndex + 1);
            parseParameters(this.queryString);
        } else {
            this.path = fullPath;
            this.queryString = null;
        }

        String line;
        while ((line = reader.readLine()) != null && !line.isEmpty()) {
            int colonIndex = line.indexOf(':');
            if (colonIndex != -1) {
                String headerName = line.substring(0, colonIndex).trim();
                String headerValue = line.substring(colonIndex + 1).trim();
                headers.put(headerName, headerValue);
            }
        }

        String hostHeader = headers.get("Host");

        this.hostWithPort = hostHeader;
        parseHostAndPort(hostHeader);
    }

    private void parseHostAndPort(String hostHeader) {
        int portIndex = hostHeader.indexOf(':');
        if (portIndex != -1) {
            this.host = hostHeader.substring(0, portIndex);
            try {
                this.port = Integer.parseInt(hostHeader.substring(portIndex + 1));
            } catch (NumberFormatException e) {
                this.port = 80;
            }
        } else {
            this.host = hostHeader;
            this.port = 80;
        }
    }

    private void parseParameters(String queryString) {
        String[] pairs = queryString.split("&");
        for (String pair : pairs) {
            int equalIndex = pair.indexOf('=');
            if (equalIndex != -1) {
                String name = pair.substring(0, equalIndex);
                String value = pair.substring(equalIndex + 1);
                parameters.put(name, value);
            }
        }
    }

    @Override
    public String getMethod() {
        return method;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public String getHost() {
        return host;
    }

    public String getQueryString() {
        return queryString;
    }

    public String getRequestURI() {
        return path;
    }

} 