package com.example.qlogserver.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Data Transfer Object representing the payload received from an agent.
 * Includes validation constraints.
 */
public class LogSubmissionPayload {

    @NotBlank(message = "Agent ID cannot be blank")
    @Size(max = 100, message = "Agent ID cannot exceed 100 characters")
    private String agentId;

    @NotBlank(message = "Environment context cannot be blank")
    @Size(max = 100, message = "Environment context cannot exceed 100 characters")
    private String environmentContext; // New field

    @NotBlank(message = "Log file name cannot be blank")
    @Size(max = 255, message = "Log file name cannot exceed 255 characters")
    private String logFileName;

    @NotNull(message = "Log lines list cannot be null")
    @NotEmpty(message = "Log lines list cannot be empty")
    private List<String> logLines;

    // Default constructor
    public LogSubmissionPayload() {
    }

    // Constructor
    public LogSubmissionPayload(String agentId, String environmentContext, String logFileName, List<String> logLines) {
        this.agentId = agentId;
        this.environmentContext = environmentContext;
        this.logFileName = logFileName;
        this.logLines = logLines;
    }

    // --- Getters and Setters ---

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getEnvironmentContext() {
        return environmentContext;
    }

    public void setEnvironmentContext(String environmentContext) {
        this.environmentContext = environmentContext;
    }

    public String getLogFileName() {
        return logFileName;
    }

    public void setLogFileName(String logFileName) {
        this.logFileName = logFileName;
    }

    public List<String> getLogLines() {
        return logLines;
    }

    public void setLogLines(List<String> logLines) {
        this.logLines = logLines;
    }

    @Override
    public String toString() {
        return "LogSubmissionPayload{" +
               "agentId=		" + agentId + 				 + 
               ", environmentContext=		" + environmentContext + 				 + 
               ", logFileName=		" + logFileName + 				 + 
               ", logLines.size=" + (logLines != null ? logLines.size() : 0) +
               		}		;
    }
}

