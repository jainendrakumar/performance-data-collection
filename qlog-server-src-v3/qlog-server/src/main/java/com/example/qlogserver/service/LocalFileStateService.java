package com.example.qlogserver.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the state (last read position, last modified time) for locally monitored log files.
 * Reads and writes state to a JSON file.
 */
@Service
public class LocalFileStateService {

    private static final Logger log = LoggerFactory.getLogger(LocalFileStateService.class);
    private final ObjectMapper objectMapper;
    private final Map<String, FileState> fileStates = new ConcurrentHashMap<>();

    public LocalFileStateService() {
        this.objectMapper = new ObjectMapper();
        // Register JavaTimeModule to handle Instant serialization/deserialization
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Loads the processing state from the specified JSON file.
     *
     * @param stateFilePath The path to the state file.
     */
    public synchronized void loadState(String stateFilePath) {
        Path path = Paths.get(stateFilePath);
        if (Files.exists(path)) {
            try {
                Map<String, FileState> loadedStates = objectMapper.readValue(path.toFile(), new TypeReference<HashMap<String, FileState>>() {});
                fileStates.clear();
                fileStates.putAll(loadedStates);
                log.info("Loaded local monitor state for {} files from {}", fileStates.size(), stateFilePath);
            } catch (IOException e) {
                log.error("Failed to load local monitor state from {}: {}", stateFilePath, e.getMessage(), e);
                // Decide if we should proceed with an empty state or halt
            }
        } else {
            log.info("Local monitor state file not found at {}, starting with empty state.", stateFilePath);
            fileStates.clear();
        }
    }

    /**
     * Saves the current processing state to the specified JSON file.
     *
     * @param stateFilePath The path to the state file.
     */
    public synchronized void saveState(String stateFilePath) {
        Path path = Paths.get(stateFilePath);
        try {
            // Ensure parent directory exists
            Files.createDirectories(path.getParent());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), fileStates);
            log.debug("Saved local monitor state for {} files to {}", fileStates.size(), stateFilePath);
        } catch (IOException e) {
            log.error("Failed to save local monitor state to {}: {}", stateFilePath, e.getMessage(), e);
        }
    }

    /**
     * Gets the processing state for a specific file.
     *
     * @param filePath The absolute path of the log file.
     * @return The FileState object, or null if no state exists for this file.
     */
    public FileState getState(String filePath) {
        return fileStates.get(filePath);
    }

    /**
     * Updates the processing state for a specific file.
     *
     * @param filePath      The absolute path of the log file.
     * @param lastReadPosition The new last read position (byte offset).
     * @param lastModified  The last modified timestamp of the file when this state was recorded.
     */
    public void updateState(String filePath, long lastReadPosition, Instant lastModified) {
        fileStates.put(filePath, new FileState(lastReadPosition, lastModified));
        log.trace("Updated local state for file 		{}		: position={}, modified={}", filePath, lastReadPosition, lastModified);
    }

    /**
     * Represents the processing state of a single log file.
     */
    public static class FileState {
        private long lastReadPosition;
        private Instant lastModified;

        // Default constructor for Jackson
        public FileState() {}

        public FileState(long lastReadPosition, Instant lastModified) {
            this.lastReadPosition = lastReadPosition;
            this.lastModified = lastModified;
        }

        public long getLastReadPosition() {
            return lastReadPosition;
        }

        public void setLastReadPosition(long lastReadPosition) {
            this.lastReadPosition = lastReadPosition;
        }

        public Instant getLastModified() {
            return lastModified;
        }

        public void setLastModified(Instant lastModified) {
            this.lastModified = lastModified;
        }

        @Override
        public String toString() {
            return "FileState{" +
                   "lastReadPosition=" + lastReadPosition +
                   ", lastModified=" + lastModified +
                   '}';
        }
    }
}

