package com.example.qlogserver.service;

import com.example.qlogserver.dto.LogSubmissionPayload;
import com.example.qlogserver.model.TransactionRecord;
import com.example.qlogserver.repository.TransactionRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service responsible for processing log submissions received from agents.
 * Parses the logs and saves them to the database using batch operations.
 */
@Service
public class AgentLogProcessingService {

    private static final Logger log = LoggerFactory.getLogger(AgentLogProcessingService.class);

    private final CsvParsingService csvParsingService;
    private final TransactionRecordRepository transactionRecordRepository;

    @Autowired
    public AgentLogProcessingService(CsvParsingService csvParsingService,
                                     TransactionRecordRepository transactionRecordRepository) {
        this.csvParsingService = csvParsingService;
        this.transactionRecordRepository = transactionRecordRepository;
    }

    /**
     * Asynchronously processes a log submission payload.
     * Parses the CSV lines and saves the resulting TransactionRecord objects to the database.
     * Uses @Transactional to ensure atomicity for the batch save.
     *
     * @param payload The LogSubmissionPayload received from the agent.
     */
    @Async // Process incoming logs asynchronously to avoid blocking the agent controller
    @Transactional // Ensures the saveAll operation is atomic for the batch
    public void processLogSubmission(LogSubmissionPayload payload) {
        log.info("Received log submission from agent 		{}		 for context 		{}		, file 		{}		 ({} lines). Processing asynchronously...",
                 payload.getAgentId(), payload.getEnvironmentContext(), payload.getLogFileName(), payload.getLogLines().size());

        try {
            // Parse CSV lines into TransactionRecord objects, associating the context
            List<TransactionRecord> records = csvParsingService.parseLogLines(
                    payload.getLogLines(),
                    payload.getAgentId(),
                    payload.getEnvironmentContext(), // Pass context
                    payload.getLogFileName()
            );

            if (!records.isEmpty()) {
                // Save the batch of records. JPA batching is enabled via application.properties
                transactionRecordRepository.saveAll(records);
                log.info("Successfully processed and saved {} records for context 		{}		, file 		{}		.",
                         records.size(), payload.getEnvironmentContext(), payload.getLogFileName());
            } else {
                log.warn("No valid records parsed from submission for context 		{}		, file 		{}		.",
                         payload.getEnvironmentContext(), payload.getLogFileName());
            }
        } catch (Exception e) {
            // Log any unexpected errors during parsing or saving
            log.error("Error processing log submission from agent 		{}		 for context 		{}		, file 		{}		: {}",
                      payload.getAgentId(), payload.getEnvironmentContext(), payload.getLogFileName(), e.getMessage(), e);
            // Depending on requirements, could implement error queuing or notification here
        }
    }
}

