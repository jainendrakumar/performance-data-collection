package com.example.qlogagent.monitor;

import com.example.qlogagent.config.AgentConfig;
import com.example.qlogagent.dto.LogSubmissionPayload;
import com.example.qlogagent.service.FileProcessingService;
import com.example.qlogagent.service.HttpService;
import com.example.qlogagent.service.StateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Monitors the configured directory for log files, processes new lines,
 * batches them, and schedules sending them to the server.
 */
public class DirectoryMonitor {

    private static final Logger log = LoggerFactory.getLogger(DirectoryMonitor.class);

    private final AgentConfig config;
    private final StateService stateService;
    private final FileProcessingService fileProcessingService;
    private final HttpService httpService;
    private final Path monitorPath;
    private final String filePattern;
    private final ScheduledExecutorService scheduler;
    private final List<String> batchBuffer = new ArrayList<>();
    private String currentBatchFileName = null; // Track file for the current buffer
    private final int batchSize;

    /**
     * Initializes the DirectoryMonitor.
     *
     * @param config              Agent configuration.
     * @param stateService        Service for managing file read state.
     * @param fileProcessingService Service for reading lines from files.
     * @param httpService         Service for sending logs to the server.
     */
    public DirectoryMonitor(AgentConfig config, StateService stateService, FileProcessingService fileProcessingService, HttpService httpService) {
        this.config = config;
        this.stateService = stateService;
        this.fileProcessingService = fileProcessingService;
        this.httpService = httpService;
        this.monitorPath = Paths.get(config.getMonitorDirectory());
        this.filePattern = config.getMonitorFilePattern(); // Simple glob pattern
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.batchSize = config.getBatchSize();

        if (!Files.isDirectory(monitorPath)) {
            log.error("Monitor directory does not exist or is not a directory: {}", monitorPath.toAbsolutePath());
            throw new IllegalArgumentException("Monitor directory is invalid: " + monitorPath.toAbsolutePath());
        }
        log.info("Monitoring directory: {}", monitorPath.toAbsolutePath());
        log.info("Using file pattern: {}", filePattern);
    }

    /**
     * Starts the monitoring process, scheduling periodic checks.
     */
    public void start() {
        int pollInterval = config.getPollIntervalSeconds();
        log.info("Starting directory monitor. Poll interval: {} seconds.", pollInterval);
        // Schedule the checkDirectory task to run periodically
        scheduler.scheduleAtFixedRate(this::checkDirectory, 0, pollInterval, TimeUnit.SECONDS);
    }

    /**
     * Stops the monitoring process gracefully.
     */
    public void stop() {
        log.info("Stopping directory monitor...");
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
            // Send any remaining lines in the buffer before exiting
            flushBatchBuffer();
            log.info("Directory monitor stopped.");
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
            log.error("Interrupted while waiting for monitor shutdown.");
        }
    }

    /**
     * Checks the monitored directory for log files and processes new lines.
     * This method is executed periodically by the scheduler.
     */
    private void checkDirectory() {
        log.debug("Checking directory: {}", monitorPath);
        try (Stream<Path> stream = Files.list(monitorPath)) {
            // Find files matching the pattern
            List<Path> logFiles = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> matchesPattern(path.getFileName().toString()))
                    .sorted() // Process files in a consistent order (e.g., by name)
                    .collect(Collectors.toList());

            if (logFiles.isEmpty()) {
                log.debug("No log files matching pattern 		{}		 found in {}.", filePattern, monitorPath);
                return;
            }

            log.debug("Found {} potential log files.", logFiles.size());

            // Process each log file
            for (Path logFile : logFiles) {
                processLogFile(logFile);
            }

            // Cleanup state for files that might have been deleted/rotated
            Map<String, Path> currentFileMap = logFiles.stream()
                    .collect(Collectors.toMap(p -> p.getFileName().toString(), p -> p));
            stateService.cleanupStaleState(currentFileMap);

        } catch (IOException e) {
            log.error("Error listing files in directory {}: {}", monitorPath, e.getMessage());
        }
    }

    /**
     * Checks if a filename matches the simple glob pattern.
     *
     * @param filename The filename to check.
     * @return true if it matches, false otherwise.
     */
    private boolean matchesPattern(String filename) {
        // Simple pattern matching (e.g., QServerTrans_*.csv)
        // For more complex patterns, PathMatcher could be used
        String regex = filePattern.replace("*", ".*").replace("?", ".");
        return filename.matches(regex);
    }

    /**
     * Processes a single log file, reading new lines and adding them to the batch buffer.
     *
     * @param logFile Path to the log file.
     */
    private void processLogFile(Path logFile) {
        String filename = logFile.getFileName().toString();
        log.debug("Processing file: {}", filename);

        List<String> newLines = fileProcessingService.readNewLines(logFile);

        if (!newLines.isEmpty()) {
            log.debug("Adding {} new lines from file 		{}		 to buffer.", newLines.size(), filename);
            addToBatchBuffer(filename, newLines);
        }
    }

    /**
     * Adds lines from a specific file to the batch buffer.
     * If the file changes, the current buffer is flushed first.
     *
     * @param filename The name of the file the lines belong to.
     * @param lines    The list of lines to add.
     */
    private synchronized void addToBatchBuffer(String filename, List<String> lines) {
        // If the filename changes, flush the buffer for the previous file first
        if (currentBatchFileName != null && !currentBatchFileName.equals(filename)) {
            log.debug("Log file changed from {} to {}. Flushing buffer for previous file.", currentBatchFileName, filename);
            flushBatchBuffer();
        }

        currentBatchFileName = filename;
        batchBuffer.addAll(lines);

        // Send batches as they fill up
        while (batchBuffer.size() >= batchSize) {
            List<String> batchToSend = new ArrayList<>(batchBuffer.subList(0, batchSize));
            batchBuffer.subList(0, batchSize).clear(); // Remove sent lines

            LogSubmissionPayload payload = new LogSubmissionPayload(config.getAgentId(), currentBatchFileName, batchToSend);
            // Consider making sendLogs asynchronous if it blocks for too long
            boolean success = httpService.sendLogs(payload);
            if (!success) {
                // Basic error handling: Logged in HttpService. Could add retry logic here.
                // For simplicity, we are currently dropping the batch on failure.
                log.error("Failed to send batch for file {}. Batch dropped.", currentBatchFileName);
                // Potential: Add batch back to buffer start for retry?
            }
        }
    }

    /**
     * Sends any remaining lines in the buffer.
     * Called during shutdown or when the processed file changes.
     */
    private synchronized void flushBatchBuffer() {
        if (!batchBuffer.isEmpty() && currentBatchFileName != null) {
            log.info("Flushing remaining {} lines in buffer for file {}.", batchBuffer.size(), currentBatchFileName);
            List<String> batchToSend = new ArrayList<>(batchBuffer); // Copy remaining lines
            batchBuffer.clear(); // Clear the buffer

            LogSubmissionPayload payload = new LogSubmissionPayload(config.getAgentId(), currentBatchFileName, batchToSend);
            boolean success = httpService.sendLogs(payload);
            if (!success) {
                log.error("Failed to flush remaining batch for file {}. Batch dropped.", currentBatchFileName);
            }
            currentBatchFileName = null; // Reset current file tracking
        }
    }
}

