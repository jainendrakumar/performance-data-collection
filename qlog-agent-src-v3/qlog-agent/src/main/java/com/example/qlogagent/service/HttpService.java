package com.example.qlogagent.service;

import com.example.qlogagent.config.AgentConfig;
import com.example.qlogagent.dto.LogSubmissionPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.zip.GZIPOutputStream;

/**
 * Service responsible for sending log batches to the central QLog Server via HTTP/HTTPS.
 * Includes retry logic and optional Gzip compression.
 */
public class HttpService {

    private static final Logger log = LoggerFactory.getLogger(HttpService.class);
    private final AgentConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final URI serverUri;
    private final int maxRetries;
    private final long initialRetryDelayMillis;
    private final boolean compressionEnabled;

    /**
     * Initializes the HttpService with configuration and creates a configured HttpClient.
     *
     * @param config Agent configuration containing server URL, credentials, retry settings, and compression flag.
     */
    public HttpService(AgentConfig config) {
        this.config = config;
        this.objectMapper = new ObjectMapper();
        this.serverUri = URI.create(config.getServerUrl());
        this.maxRetries = config.getRetryMaxAttempts();
        this.initialRetryDelayMillis = config.getRetryInitialDelayMillis();
        this.compressionEnabled = config.isCompressionEnabled(); // Read compression setting

        // Configure HttpClient
        HttpClient.Builder clientBuilder = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(config.getHttpConnectTimeoutSeconds()));

        // Authentication
        String username = config.getServerUsername();
        String password = config.getServerPassword();
        if (username != null && !username.isBlank() && password != null) {
            log.info("Configuring HttpClient with Basic Authentication for user: {}", username);
        } else {
            log.warn("Server username or password not configured. Sending requests without authentication.");
        }

        // SSL/TLS Configuration (Default)

        this.httpClient = clientBuilder.build();
        log.info("HttpService initialized. Server URL: {}, Compression: {}, Max Retries: {}, Initial Delay: {}ms",
                 serverUri, compressionEnabled, maxRetries, initialRetryDelayMillis);
    }

    /**
     * Sends a batch of log data to the server with retry logic and optional compression.
     *
     * @param payload The LogSubmissionPayload containing agent ID, context, filename, and log lines.
     * @return true if the submission was ultimately successful (received 2xx status code), false otherwise.
     */
    public boolean sendLogs(LogSubmissionPayload payload) {
        int attempt = 0;
        long currentDelay = initialRetryDelayMillis;

        while (attempt <= maxRetries) {
            attempt++;
            log.debug("Attempt {}/{} to send log batch for context 		{}		, file 		{}		 ({} lines) to {}",
                      attempt, maxRetries + 1, payload.getEnvironmentContext(), payload.getLogFileName(), payload.getLogLines().size(), serverUri);

            try {
                // 1. Serialize Payload to JSON bytes
                byte[] jsonPayloadBytes = objectMapper.writeValueAsBytes(payload);
                byte[] requestBodyBytes = jsonPayloadBytes;
                boolean isCompressed = false;

                // 2. Optionally Compress
                if (compressionEnabled) {
                    try {
                        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                        try (GZIPOutputStream gzipStream = new GZIPOutputStream(byteStream)) {
                            gzipStream.write(jsonPayloadBytes);
                        }
                        requestBodyBytes = byteStream.toByteArray();
                        isCompressed = true;
                        log.debug("Compressed payload from {} bytes to {} bytes ({}% reduction)",
                                  jsonPayloadBytes.length, requestBodyBytes.length,
                                  (100 - (100.0 * requestBodyBytes.length / jsonPayloadBytes.length)));
                    } catch (IOException e) {
                        log.error("Failed to Gzip payload for file 		{}		. Sending uncompressed. Error: {}", payload.getLogFileName(), e.getMessage());
                        // Fallback to sending uncompressed if compression fails
                        requestBodyBytes = jsonPayloadBytes;
                        isCompressed = false;
                    }
                }

                // 3. Build Request
                HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                        .uri(serverUri)
                        .timeout(Duration.ofSeconds(config.getHttpRequestTimeoutSeconds()))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofByteArray(requestBodyBytes));

                // 4. Add Compression Header if needed
                if (isCompressed) {
                    requestBuilder.header("Content-Encoding", "gzip");
                }

                // 5. Add Authentication Header
                String username = config.getServerUsername();
                String password = config.getServerPassword();
                if (username != null && !username.isBlank() && password != null) {
                    String auth = username + ":" + password;
                    String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
                    requestBuilder.header("Authorization", "Basic " + encodedAuth);
                }

                HttpRequest request = requestBuilder.build();

                // 6. Send Request and Handle Response
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                int statusCode = response.statusCode();

                log.debug("Received response from server (Attempt {}): Status = {}, Body = {}", attempt, statusCode, response.body());

                if (statusCode >= 200 && statusCode < 300) {
                    log.info("Successfully sent log batch for context 		{}		, file 		{}		 ({} lines) after {} attempt(s).",
                             payload.getEnvironmentContext(), payload.getLogFileName(), payload.getLogLines().size(), attempt);
                    return true; // Success
                } else {
                    log.warn("Failed to send log batch for context 		{}		, file 		{}		 (Attempt {}). Server responded with status {}: {}",
                            payload.getEnvironmentContext(), payload.getLogFileName(), attempt, statusCode, response.body());

                    if (shouldRetry(statusCode)) {
                        if (attempt > maxRetries) {
                            log.error("Max retries ({}) reached for context 		{}		, file 		{}		. Giving up on this batch.", maxRetries, payload.getEnvironmentContext(), payload.getLogFileName());
                            return false; // Max retries exceeded
                        }
                        log.warn("Retrying in {} ms...", currentDelay);
                        try {
                            Thread.sleep(currentDelay);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            log.error("Retry delay interrupted for context 		{}		, file 		{}		. Aborting retries.", payload.getEnvironmentContext(), payload.getLogFileName());
                            return false;
                        }
                        currentDelay *= 2;
                    } else {
                        log.error("Non-retryable error status {} received for context 		{}		, file 		{}		. Aborting send for this batch.", statusCode, payload.getEnvironmentContext(), payload.getLogFileName());
                        if (statusCode == 401 || statusCode == 403) {
                             log.error("Authentication/Authorization failed. Check agent credentials (server.username, server.password) in agent.properties and server configuration.");
                        }
                        return false;
                    }
                }
            } catch (IOException e) {
                log.warn("IOException during send attempt {} for context 		{}		, file 		{}		 to server {}: {}.",
                         attempt, payload.getEnvironmentContext(), payload.getLogFileName(), serverUri, e.getMessage());
                if (attempt > maxRetries) {
                    log.error("Max retries ({}) reached due to IOExceptions for context 		{}		, file 		{}		. Giving up on this batch.", maxRetries, payload.getEnvironmentContext(), payload.getLogFileName());
                    return false;
                }
                log.warn("Retrying in {} ms...", currentDelay);
                try {
                    Thread.sleep(currentDelay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.error("Retry delay interrupted for context 		{}		, file 		{}		. Aborting retries.", payload.getEnvironmentContext(), payload.getLogFileName());
                    return false;
                }
                currentDelay *= 2;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("HTTP send interrupted for context 		{}		, file 		{}		. Aborting send.", payload.getEnvironmentContext(), payload.getLogFileName());
                return false;
            } catch (Exception e) {
                 log.error("Unexpected error during send attempt {} for context 		{}		, file 		{}		: {}", attempt, payload.getEnvironmentContext(), payload.getLogFileName(), e.getMessage(), e);
                 return false;
            }
        } // end while loop

        log.error("Exited send loop unexpectedly after {} attempts for context 		{}		, file 		{}		. Assuming failure.", attempt -1, payload.getEnvironmentContext(), payload.getLogFileName());
        return false;
    }

    private boolean shouldRetry(int statusCode) {
        return statusCode >= 500 || statusCode == 408 || statusCode == 429;
    }
}

