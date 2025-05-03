package com.example.qlogserver.controller;

import com.example.qlogserver.dto.LogSubmissionPayload;
import com.example.qlogserver.service.AgentLogProcessingService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller handling log submissions from QLog Agents.
 */
@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private static final Logger log = LoggerFactory.getLogger(AgentController.class);

    private final AgentLogProcessingService agentLogProcessingService;

    @Autowired
    public AgentController(AgentLogProcessingService agentLogProcessingService) {
        this.agentLogProcessingService = agentLogProcessingService;
    }

    /**
     * Endpoint for agents to submit batches of log lines.
     * Requires authentication (configured via Spring Security).
     *
     * @param payload        The log submission payload containing agent ID, filename, and lines.
     * @param authentication Spring Security authentication object (injected).
     * @return ResponseEntity indicating success (202 Accepted) or failure (e.g., 400 Bad Request).
     */
    @PostMapping("/submit-logs")
    public ResponseEntity<String> submitLogs(@Valid @RequestBody LogSubmissionPayload payload,
                                             Authentication authentication) {

        // Log authenticated user (agent)
        String agentUsername = "unknown";
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
            agentUsername = ((UserDetails) authentication.getPrincipal()).getUsername();
        } else if (authentication != null) {
            agentUsername = authentication.getName();
        }

        log.info("Received log submission from authenticated agent 		{}		 for file 		{}		 ({} lines)",
                 agentUsername, payload.getLogFileName(), payload.getLogLines().size());

        // Basic validation is handled by @Valid, but add any custom validation if needed
        if (payload.getLogLines() == null || payload.getLogLines().isEmpty()) {
            log.warn("Received empty log lines list from agent {}.", agentUsername);
            return ResponseEntity.badRequest().body("Log lines list cannot be null or empty.");
        }

        try {
            // Trigger asynchronous processing
            agentLogProcessingService.processLogBatch(payload.getLogLines(), payload.getAgentId(), payload.getLogFileName());

            // Respond immediately with 202 Accepted as processing is asynchronous
            return ResponseEntity.accepted().body("Log batch accepted for processing.");

        } catch (Exception e) {
            // Catch unexpected errors during the submission process itself (not async processing)
            log.error("Error accepting log batch from agent {}: {}", agentUsername, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error processing request.");
        }
    }
}

