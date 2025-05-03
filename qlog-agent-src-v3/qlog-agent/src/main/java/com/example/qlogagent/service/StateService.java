package com.example.qlogagent.service;

import com.example.qlogagent.config.AgentConfig;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Manages the state of processed log files, specifically the last read position (byte offset).
 * Persists state to a JSON file to handle agent restarts.
 */
public class StateService {

    private static final Logger log = LoggerFactory.getLogger(StateService.class);
    private final Path stateFilePath;
    private final ObjectMapper objectMapper;
    // Stores filename -> last byte offset read
    private final Map<String, Long> fileStates;
    private final ReentrantLock lock = new ReentrantLock();

    /**
     * Initializes the StateService, loading existing state from the configured file path.
     *
     * @param config Agent configuration providing the state file path.
     */
    public StateService(AgentConfig config) {
        this.stateFilePath = Paths.get(config.getStateFilePath());
        this.objectMapper = new ObjectMapper();
        this.fileStates = loadState();
    }

    /**
     * Loads the persisted state from the JSON file.
     *
     * @return A map containing filename-to-offset mappings, or an empty map if loading fails.
     */
    private Map<String, Long> loadState() {
        lock.lock();
        try {
            if (Files.exists(stateFilePath)) {
                try {
                    byte[] jsonData = Files.readAllBytes(stateFilePath);
                    if (jsonData.length > 0) {
                        Map<String, Long> loaded = objectMapper.readValue(jsonData, new TypeReference<Map<String, Long>>() {});
                        log.info("Loaded state for {} files from {}", loaded.size(), stateFilePath.toAbsolutePath());
                        // Return a ConcurrentHashMap for thread safety
                        return new ConcurrentHashMap<>(loaded);
                    } else {
                        log.info("State file {} is empty, starting with empty state.", stateFilePath.toAbsolutePath());
                    }
                } catch (IOException e) {
                    log.error("Failed to read or parse state file {}: {}. Starting with empty state.", stateFilePath.toAbsolutePath(), e.getMessage());
                }
            } else {
                log.info("State file {} not found, starting with empty state.", stateFilePath.toAbsolutePath());
            }
        } finally {
            lock.unlock();
        }
        return new ConcurrentHashMap<>(); // Return empty concurrent map if load fails or file doesn't exist
    }

    /**
     * Persists the current state (all file offsets) to the JSON file.
     */
    private void saveState() {
        lock.lock();
        try {
            // Create parent directories if they don't exist
            Path parentDir = stateFilePath.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }
            byte[] jsonData = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(fileStates);
            Files.write(stateFilePath, jsonData, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            log.debug("Saved state for {} files to {}", fileStates.size(), stateFilePath.toAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to save state file {}: {}", stateFilePath.toAbsolutePath(), e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    /**
     * Gets the last known byte offset for a given log file.
     *
     * @param filename The name of the log file.
     * @return The last read byte offset, or 0 if the file is unknown.
     */
    public long getLastReadOffset(String filename) {
        return fileStates.getOrDefault(filename, 0L);
    }

    /**
     * Updates the last known byte offset for a given log file and persists the state.
     *
     * @param filename The name of the log file.
     * @param offset   The new last read byte offset.
     */
    public void updateLastReadOffset(String filename, long offset) {
        if (offset < 0) {
            log.warn("Attempted to update offset for file 	'{}' with negative value: {}. Ignoring.", filename, offset);
            return;
        }
        Long previous = fileStates.put(filename, offset);
        if (previous == null || !previous.equals(offset)) {
             log.debug("Updated offset for file 	'{}' from {} to {}", filename, previous != null ? previous : "N/A", offset);
             // Save state immediately after update
             saveState();
        } else {
             log.trace("Offset for file 	'{}' remains unchanged at {}. No state save needed.", filename, offset);
        }
    }

    /**
     * Removes state for files that no longer exist or are no longer relevant.
     * This is a placeholder and might need more sophisticated logic (e.g., based on last modified time).
     *
     * @param currentFiles A map of currently relevant files (e.g., filename -> Path).
     */
    public void cleanupStaleState(Map<String, Path> currentFiles) {
        lock.lock();
        try {
            boolean changed = fileStates.keySet().retainAll(currentFiles.keySet());
            if (changed) {
                log.info("Cleaned up stale entries from state file.");
                saveState();
            }
        } finally {
            lock.unlock();
        }
    }
}

