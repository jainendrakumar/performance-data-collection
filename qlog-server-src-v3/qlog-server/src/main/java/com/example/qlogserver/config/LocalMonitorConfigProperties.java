package com.example.qlogserver.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for the optional standalone local log monitoring feature.
 * Maps properties starting with "local.monitor".
 */
@Configuration
@ConfigurationProperties(prefix = "local.monitor")
@Validated // Enable validation of properties
public class LocalMonitorConfigProperties {

    /**
     * Enable monitoring of a local directory by the server itself.
     */
    private boolean enabled = false;

    /**
     * The directory for the server to monitor locally.
     * Required if local monitoring is enabled.
     */
    private String directory;

    /**
     * The file pattern for local log files (e.g., QServerTrans_*.csv).
     */
    @NotBlank(message = "Local monitor file pattern cannot be blank")
    private String filePattern = "QServerTrans_*.csv";

    /**
     * How often (in seconds) the server checks the local directory.
     */
    @Min(value = 1, message = "Local monitor poll interval must be at least 1 second")
    private int pollIntervalSeconds = 30;

    /**
     * The context name to assign to logs processed locally by the server.
     */
    @NotBlank(message = "Local monitor context cannot be blank")
    private String context = "LocalServerContext";

    /**
     * The file path for storing the state of the local monitor.
     */
    @NotBlank(message = "Local monitor state file path cannot be blank")
    private String stateFilePath = "./local-monitor-state.json";

    // --- Getters and Setters ---

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    public String getFilePattern() {
        return filePattern;
    }

    public void setFilePattern(String filePattern) {
        this.filePattern = filePattern;
    }

    public int getPollIntervalSeconds() {
        return pollIntervalSeconds;
    }

    public void setPollIntervalSeconds(int pollIntervalSeconds) {
        this.pollIntervalSeconds = pollIntervalSeconds;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getStateFilePath() {
        return stateFilePath;
    }

    public void setStateFilePath(String stateFilePath) {
        this.stateFilePath = stateFilePath;
    }
}

