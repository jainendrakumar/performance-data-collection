package com.example.qlogserver.service;

import com.example.qlogserver.config.LocalMonitorConfigProperties;
import com.example.qlogserver.model.TransactionRecord;
import com.example.qlogserver.repository.TransactionRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Service responsible for monitoring a local directory for Quintiq log files,
 * processing new entries, and saving them to the database.
 * This runs as a scheduled task within the server application.
 */
@Service
public class LocalLogMonitorService {

    private static final Logger log = LoggerFactory.getLogger(LocalLogMonitorService.class);

    private final LocalMonitorConfigProperties config;
    private final LocalFileStateService stateService;
    private final CsvParsingService csvParsingService;
    private final TransactionRecordRepository transactionRecordRepository;
    private PathMatcher pathMatcher;

    @Autowired
    public LocalLogMonitorService(LocalMonitorConfigProperties config,
                                LocalFileStateService stateService,
                                CsvParsingService csvParsingService,
                                TransactionRecordRepository transactionRecordRepository) {
        this.config = config;
        this.stateService = stateService;
        this.csvParsingService = csvParsingService;
        this.transactionRecordRepository = transactionRecordRepository;
    }

    @PostConstruct
    public void initialize() {
        if (config.isEnabled()) {
            log.info("Local log monitoring is ENABLED.");
            log.info("Monitoring directory: {}", config.getDirectory());
            log.info("File pattern: {}", config.getFilePattern());
            log.info("Polling interval: {} seconds", config.getPollIntervalSeconds());
            log.info("Context for local logs: {}", config.getContext());
            log.info("State file path: {}", config.getStateFilePath());

            // Initialize PathMatcher
            this.pathMatcher = FileSystems.getDefault().getPathMatcher("glob:" + config.getFilePattern());

            // Load initial state
            stateService.loadState(config.getStateFilePath());
        } else {
            log.info("Local log monitoring is DISABLED.");
        }
    }

    /**
     * Scheduled task to poll the local directory for log files.
     * Runs at a fixed delay specified in the configuration.
     */
    @Scheduled(fixedDelayString = "#{@localMonitorConfigProperties.pollIntervalSeconds * 1000}", initialDelay = 5000) // Run 5s after startup, then based on interval
    @Transactional // Manage transaction for batch saving
    public void pollDirectory() {
        if (!config.isEnabled()) {
            return; // Do nothing if disabled
        }

        log.debug("Polling local directory: {}", config.getDirectory());
        Path monitorDir = Paths.get(config.getDirectory());

        if (!Files.isDirectory(monitorDir)) {
            log.error("Local monitor directory does not exist or is not a directory: {}", config.getDirectory());
            return;
        }

        boolean stateChanged = false;
        try (Stream<Path> stream = Files.list(monitorDir)) {
            stream.filter(Files::isRegularFile)
                  .filter(path -> pathMatcher.matches(path.getFileName()))
                  .forEach(filePath -> {
                      if (processFile(filePath)) {
                          // Mark state as changed if any file was processed
                          // Note: processFile handles individual file state updates internally
                          // This flag is just to trigger a save at the end.
                          // A more robust approach might save state per file processed.
                          // For simplicity here, we save all states if any changed.
                          // stateChanged = true; // Re-evaluating this - saveState is called within processFile now
                      }
                  });

            // Save state if any file processing occurred and updated the state
            // Removed this global save - now saving happens within processFile upon successful processing
            // if (stateChanged) {
            //     stateService.saveState(config.getStateFilePath());
            // }

        } catch (IOException e) {
            log.error("Error listing files in local monitor directory {}: {}", config.getDirectory(), e.getMessage(), e);
        }
        log.debug("Finished polling local directory.");
    }

    /**
     * Processes a single log file, reading new lines based on stored state.
     *
     * @param filePath The path to the log file.
     * @return true if the file was processed and state potentially updated, false otherwise.
     */
    private boolean processFile(Path filePath) {
        String absolutePathStr = filePath.toAbsolutePath().toString();
        log.trace("Checking file: {}", absolutePathStr);

        try {
            BasicFileAttributes attrs = Files.readAttributes(filePath, BasicFileAttributes.class);
            Instant currentLastModified = attrs.lastModifiedTime().toInstant();
            long currentSize = attrs.size();

            LocalFileStateService.FileState currentState = stateService.getState(absolutePathStr);
            long startPosition = 0;
            boolean process = false;

            if (currentState == null) {
                // New file
                log.info("Detected new local log file: {}", absolutePathStr);
                process = true;
                startPosition = 0;
            } else {
                // Existing file, check if modified or grown
                if (currentLastModified.isAfter(currentState.getLastModified()) || currentSize > currentState.getLastReadPosition()) {
                    log.info("Detected changes in local log file: {}", absolutePathStr);
                    process = true;
                    startPosition = currentState.getLastReadPosition();
                    // Handle potential log rotation (size decreased but modified time changed)
                    if (currentSize < startPosition) {
                        log.warn("Log file {} appears to have been rotated (size decreased). Processing from beginning.", absolutePathStr);
                        startPosition = 0;
                    }
                } else {
                    log.trace("No changes detected for file: {}", absolutePathStr);
                }
            }

            if (process && currentSize > startPosition) {
                List<String> newLines = readNewLines(filePath, startPosition);
                if (!newLines.isEmpty()) {
                    log.debug("Read {} new lines from file: {}", newLines.size(), absolutePathStr);
                    // Parse and save within the transaction boundary of pollDirectory()
                    List<TransactionRecord> records = csvParsingService.parseLogLines(
                            newLines,
                            "LocalMonitor", // Agent ID for local logs
                            config.getContext(), // Configured local context
                            filePath.getFileName().toString()
                    );

                    if (!records.isEmpty()) {
                        transactionRecordRepository.saveAll(records); // Leverage batching
                        log.info("Saved {} records from local file {} for context {}", records.size(), absolutePathStr, config.getContext());
                        // Update state ONLY after successful save
                        stateService.updateState(absolutePathStr, currentSize, currentLastModified);
                        stateService.saveState(config.getStateFilePath()); // Save state immediately after processing a file
                        return true; // Indicate processing occurred
                    } else {
                         log.warn("No valid records parsed from new lines in local file: {}", absolutePathStr);
                         // Update state even if no valid records, to avoid reprocessing invalid lines
                         stateService.updateState(absolutePathStr, currentSize, currentLastModified);
                         stateService.saveState(config.getStateFilePath());
                         return true;
                    }
                } else {
                    // File changed but no new lines read (e.g., only timestamp updated)
                    // Update state to prevent reprocessing check until next modification
                    stateService.updateState(absolutePathStr, currentSize, currentLastModified);
                    stateService.saveState(config.getStateFilePath());
                    return true;
                }
            }

        } catch (IOException e) {
            log.error("Error processing local file {}: {}", absolutePathStr, e.getMessage(), e);
        } catch (Exception e) {
            // Catch unexpected parsing/saving errors
            log.error("Unexpected error processing local file {}: {}", absolutePathStr, e.getMessage(), e);
        }
        return false; // No processing occurred or error happened
    }

    /**
     * Reads lines from a file starting at a specific byte offset.
     *
     * @param filePath      The path to the file.
     * @param startPosition The byte offset to start reading from.
     * @return A list of lines read from the file.
     * @throws IOException If an I/O error occurs.
     */
    private List<String> readNewLines(Path filePath, long startPosition) throws IOException {
        List<String> lines = new ArrayList<>();
        try (RandomAccessFile raf = new RandomAccessFile(filePath.toFile(), "r")) {
            raf.seek(startPosition);
            String line;
            // Read lines using default charset, assuming logs are consistently encoded
            // Consider making charset configurable if needed
            while ((line = raf.readLine()) != null) {
                // RandomAccessFile.readLine() uses ISO-8859-1, need to re-encode if logs are UTF-8
                // A more robust solution might use Files.newBufferedReader with specific charset
                // For simplicity assuming default or compatible encoding here.
                // If logs are UTF-8, this might mangle multi-byte characters.
                // Let's try reading as UTF-8 directly for better compatibility
                lines.add(new String(line.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8));
            }
        }
        // Alternative using NIO (potentially more robust with charsets):
        /*
        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            // Skip bytes - less efficient than seeking
            long skipped = reader.skip(startPosition); // This skips CHARACTERS, not bytes - problematic!
            // Seeking is better, RandomAccessFile is okay but charset handling is tricky.
            // Best might be a library or custom byte buffer reading.
            // Sticking with corrected RAF for now.
            String line;
            while ((line = reader.readLine()) != null) {
                 lines.add(line);
            }
        }
        */
        return lines;
    }
}

