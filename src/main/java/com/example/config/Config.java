package com.example.config;

import com.example.exception.WebServerException;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Data
public class Config {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Getter
    private final int port;
    private final String defaultHost;
    private final List<HostConfig> hosts;
    private final SecurityConfig security;
    private final ErrorPages errorPages;
    @Getter
    private final List<ServletConfig> servlets;
    private List<Pattern> forbiddenPatterns;

    @JsonCreator
    public Config(
            @JsonProperty("port") int port,
            @JsonProperty("defaultHost") String defaultHost,
            @JsonProperty("hosts") List<HostConfig> hosts,
            @JsonProperty("security") SecurityConfig security,
            @JsonProperty("errorPages") ErrorPages errorPages,
            @JsonProperty("servlets") List<ServletConfig> servlets) {
        this.port = port;
        this.defaultHost = defaultHost;
        this.hosts = hosts != null ? hosts : new ArrayList<>();
        this.security = security != null ? security : new SecurityConfig(List.of(), List.of(), List.of());
        this.errorPages = errorPages != null ? errorPages : new ErrorPages("error/404.html", "error/403.html", "error/500.html");
        this.servlets = servlets != null ? servlets : new ArrayList<>();
        initializeForbiddenPatterns();

        log.info("Config loaded with {} servlets", this.servlets.size());
        this.servlets.forEach(servlet ->
            log.info("Servlet config: {} -> {} with patterns: {}",
                servlet.name(), servlet.className(), servlet.urlPatterns()));
    }

    public static Config load(String configPath) throws WebServerException {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(new File(configPath), Config.class);
        } catch (IOException e) {
            throw new WebServerException(500, "Failed to load configuration: " + e.getMessage());
        }
    }

    private void initializeForbiddenPatterns() {
        List<String> patterns = security.forbiddenPatterns();
        if (patterns == null || patterns.isEmpty()) {
            patterns = List.of(".*\\.exe$", ".*\\.sh$", ".*\\.bat$", ".*\\.cmd$");
        }
        
        forbiddenPatterns = patterns.stream()
                .map(Pattern::compile)
                .collect(Collectors.toList());
    }

    public String getDocBase(String host) {
        return findHostConfig(host)
                .map(HostConfig::getHttpRoot)
                .orElse(null);
    }

    public String getWelcomeFile(String host) {
        return findHostConfig(host)
                .map(HostConfig::getWelcomeFile)
                .orElse("index.html");
    }

    public String getErrorPage(String host, int statusCode) {
        return findHostConfig(host)
                .map(HostConfig::getErrorPages)
                .map(errorPages -> switch (statusCode) {
                    case 404 -> errorPages.notFound();
                    case 403 -> errorPages.forbidden();
                    case 500 -> errorPages.internalError();
                    default -> null;
                })
                .orElse(null);
    }

    public boolean isHostConfigured(String host) {
        return findHostConfig(host).isPresent();
    }

    public boolean isPathForbidden(String path) {
        return forbiddenPatterns.stream()
                .anyMatch(pattern -> pattern.matcher(path).matches());
    }

    private java.util.Optional<HostConfig> findHostConfig(String host) {
        return hosts.stream()
                .filter(h -> h.getName().equals(host))
                .findFirst();
    }

    public record ServletConfig(String name, String className, List<String> urlPatterns) {}

    public record HostConfig(
            String name,
            String httpRoot,
            String welcomeFile,
            ErrorPages errorPages
    ) {
        public String getName() { return name; }
        public String getHttpRoot() { return httpRoot; }
        public String getWelcomeFile() { return welcomeFile; }
        public ErrorPages getErrorPages() { return errorPages; }
    }

    public record SecurityConfig(
        List<String> forbiddenPatterns,
        List<String> forbiddenExtensions,
        List<String> forbiddenPaths
    ) {}

    public record ErrorPages(
        String notFound,
        String forbidden,
        String internalError
    ) {}

}