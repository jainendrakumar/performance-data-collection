package com.example.qlogagent.dto;

import java.util.List;

/**
 * Data Transfer Object representing the payload sent from the agent to the server.
 */
public class LogSubmissionPayload {

    private String agentId;
    private String environmentContext; // New field for environment context
    private String logFileName;
    private List<String> logLines;

    // Default constructor (required for JSON deserialization if needed, though primarily used for serialization here)
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

