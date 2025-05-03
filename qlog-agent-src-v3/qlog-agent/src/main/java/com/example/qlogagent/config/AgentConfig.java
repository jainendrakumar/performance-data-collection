package com.example.qlogagent.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Loads and provides access to agent configuration from a properties file.
 */
public class AgentConfig {

    private static final Logger log = LoggerFactory.getLogger(AgentConfig.class);
    private final Properties properties = new Properties();

    // Default configuration file name
    private static final String DEFAULT_CONFIG_FILE = "agent.properties";

    // Property keys
    private static final String KEY_AGENT_ID = "agent.id";
    private static final String KEY_ENV_CONTEXT = "environment.context"; // New
    private static final String KEY_MONITOR_DIR = "monitor.directory";
    private static final String KEY_MONITOR_PATTERN = "monitor.filePattern";
    private static final String KEY_POLL_INTERVAL = "poll.interval.seconds";
    private static final String KEY_SERVER_URL = "server.url";
    private static final String KEY_SERVER_USER = "server.username";
    private static final String KEY_SERVER_PASS = "server.password";
    private static final String KEY_BATCH_SIZE = "batch.size";
    private static final String KEY_STATE_FILE = "state.filePath";
    private static final String KEY_HTTP_CONNECT_TIMEOUT = "http.connect.timeout.seconds";
    private static final String KEY_HTTP_REQUEST_TIMEOUT = "http.request.timeout.seconds";
    private static final String KEY_RETRY_ATTEMPTS = "retry.max.attempts";
    private static final String KEY_RETRY_DELAY = "retry.initial.delay.millis";
    private static final String KEY_COMPRESSION_ENABLED = "communication.compression.enabled"; // New

    /**
     * Loads configuration from the specified file path or the default location.
     *
     * @param configFilePath Optional path to the configuration file. If null, uses DEFAULT_CONFIG_FILE in the current directory.
     * @throws IOException If the configuration file cannot be read.
     */
    public AgentConfig(String configFilePath) throws IOException {
        String effectivePath = (configFilePath != null && !configFilePath.isBlank()) ? configFilePath : DEFAULT_CONFIG_FILE;
        Path path = Paths.get(effectivePath).toAbsolutePath();

        if (Files.exists(path) && Files.isReadable(path)) {
            try (InputStream input = new FileInputStream(path.toFile())) {
                properties.load(input);
                log.info("Loaded agent configuration from: {}", path);
            } catch (IOException e) {
                log.error("Failed to load configuration file from {}: {}", path, e.getMessage());
                throw e;
            }
        } else {
            String errorMsg = String.format("Configuration file not found or not readable at: %s", path);
            log.error(errorMsg);
            throw new IOException(errorMsg);
        }
        logConfiguration(); // Log loaded values
    }

    // --- Getters with Defaults ---

    public String getAgentId() {
        return properties.getProperty(KEY_AGENT_ID, "DefaultAgent");
    }

    public String getEnvironmentContext() {
        return properties.getProperty(KEY_ENV_CONTEXT, "DefaultContext"); // New
    }

    public String getMonitorDirectory() {
        return properties.getProperty(KEY_MONITOR_DIR, "."); // Default to current directory
    }

    public String getMonitorFilePattern() {
        return properties.getProperty(KEY_MONITOR_PATTERN, "QServerTrans_*.csv");
    }

    public int getPollIntervalSeconds() {
        return Integer.parseInt(properties.getProperty(KEY_POLL_INTERVAL, "10"));
    }

    public String getServerUrl() {
        return properties.getProperty(KEY_SERVER_URL, "http://localhost:8080/api/agent/submit-logs");
    }

    public String getServerUsername() {
        return properties.getProperty(KEY_SERVER_USER);
    }

    public String getServerPassword() {
        return properties.getProperty(KEY_SERVER_PASS);
    }

    public int getBatchSize() {
        return Integer.parseInt(properties.getProperty(KEY_BATCH_SIZE, "500")); // Increased default
    }

    public String getStateFilePath() {
        return properties.getProperty(KEY_STATE_FILE, "./agent-state.json");
    }

    public int getHttpConnectTimeoutSeconds() {
        return Integer.parseInt(properties.getProperty(KEY_HTTP_CONNECT_TIMEOUT, "15")); // Increased default
    }

    public int getHttpRequestTimeoutSeconds() {
        return Integer.parseInt(properties.getProperty(KEY_HTTP_REQUEST_TIMEOUT, "60")); // Increased default
    }

    public int getRetryMaxAttempts() {
        return Integer.parseInt(properties.getProperty(KEY_RETRY_ATTEMPTS, "3"));
    }

    public long getRetryInitialDelayMillis() {
        return Long.parseLong(properties.getProperty(KEY_RETRY_DELAY, "2000")); // Increased default
    }

    public boolean isCompressionEnabled() {
        return Boolean.parseBoolean(properties.getProperty(KEY_COMPRESSION_ENABLED, "true")); // New, default true
    }

    /**
     * Logs the loaded configuration values (excluding password).
     */
    private void logConfiguration() {
        log.info("--- Agent Configuration ---");
        log.info("Agent ID: {}", getAgentId());
        log.info("Environment Context: {}", getEnvironmentContext());
        log.info("Monitor Directory: {}", getMonitorDirectory());
        log.info("Monitor File Pattern: {}", getMonitorFilePattern());
        log.info("Poll Interval: {} seconds", getPollIntervalSeconds());
        log.info("Server URL: {}", getServerUrl());
        log.info("Server Username: {}", getServerUsername() != null ? getServerUsername() : "(Not Set)");
        log.info("Batch Size: {}", getBatchSize());
        log.info("State File Path: {}", getStateFilePath());
        log.info("Compression Enabled: {}", isCompressionEnabled());
        log.info("HTTP Connect Timeout: {} seconds", getHttpConnectTimeoutSeconds());
        log.info("HTTP Request Timeout: {} seconds", getHttpRequestTimeoutSeconds());
        log.info("Retry Max Attempts: {}", getRetryMaxAttempts());
        log.info("Retry Initial Delay: {} ms", getRetryInitialDelayMillis());
        log.info("-------------------------");
    }
}

