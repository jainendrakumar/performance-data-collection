package com.example.qlogagent.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Service responsible for reading new lines from a specific log file
 * starting from a given byte offset.
 */
public class FileProcessingService {

    private static final Logger log = LoggerFactory.getLogger(FileProcessingService.class);
    private final StateService stateService;

    /**
     * Constructs a FileProcessingService.
     *
     * @param stateService The service used to get and update file read offsets.
     */
    public FileProcessingService(StateService stateService) {
        this.stateService = stateService;
    }

    /**
     * Reads new lines from the specified log file since the last recorded offset.
     * Updates the state with the new offset after reading.
     *
     * @param logFilePath The path to the log file.
     * @return A list of new lines read from the file, or an empty list if no new lines or an error occurs.
     */
    public List<String> readNewLines(Path logFilePath) {
        List<String> newLines = new ArrayList<>();
        String filename = logFilePath.getFileName().toString();
        long lastOffset = stateService.getLastReadOffset(filename);
        long currentSize;

        try {
            if (!Files.exists(logFilePath) || !Files.isReadable(logFilePath)) {
                log.warn("Log file not found or not readable: {}", logFilePath);
                return newLines; // Return empty list
            }

            currentSize = Files.size(logFilePath);

            // Check if file has shrunk (e.g., log rotation without moving)
            if (currentSize < lastOffset) {
                log.warn("Log file 		{}		 appears to have shrunk (or rotated without moving). Resetting offset from {} to 0.", filename, lastOffset);
                lastOffset = 0;
                // Update state immediately to reflect reset
                stateService.updateLastReadOffset(filename, 0);
            }

            if (currentSize == lastOffset) {
                log.trace("No new content in file 		{}		 since last check (offset: {}).", filename, lastOffset);
                return newLines; // No new content
            }

            log.debug("Reading file 		{}		 from offset {} to {}", filename, lastOffset, currentSize);

            // Use FileChannel for seeking and BufferedReader for efficient line reading
            try (FileChannel channel = FileChannel.open(logFilePath, StandardOpenOption.READ);
                 BufferedReader reader = new BufferedReader(Files.newBufferedReader(logFilePath, StandardCharsets.UTF_8))) // Assuming UTF-8 encoding
            {
                // Seek the channel (BufferedReader doesn't support seeking directly)
                // Note: Seeking channel doesn't affect BufferedReader's internal position directly.
                // We need to skip characters in the reader.
                // This is less efficient than direct channel reading but simpler for line splitting.
                // A more optimized approach might read bytes via channel and handle line breaks manually.

                // Skip bytes in the reader to approximate the offset
                // This assumes single-byte characters mostly, might be inaccurate for multi-byte chars at the boundary
                long skipped = reader.skip(lastOffset);
                if (skipped != lastOffset) {
                    log.warn("Could not skip accurately to offset {} in file {}. Skipped {}. Reading might be inaccurate.", lastOffset, filename, skipped);
                    // Attempt to recover might be needed here, or just proceed cautiously.
                }

                String line;
                long currentReadOffset = lastOffset;
                while ((line = reader.readLine()) != null) {
                    newLines.add(line);
                    // Estimate new offset - this is approximate due to newline chars
                    // A more precise way requires counting bytes including newline chars
                    currentReadOffset += line.getBytes(StandardCharsets.UTF_8).length + System.lineSeparator().getBytes(StandardCharsets.UTF_8).length; // Approximation
                }

                // Update state with the *actual* new size of the file, ensuring we don't miss anything
                // if the byte counting above was inaccurate.
                stateService.updateLastReadOffset(filename, currentSize);
                log.info("Read {} new lines from file 		{}		. New offset: {}", newLines.size(), filename, currentSize);

            } catch (IOException e) {
                log.error("Error reading file {}: {}", logFilePath, e.getMessage());
                // Don't update offset if reading failed
                return new ArrayList<>(); // Return empty list on error
            }

        } catch (IOException e) {
            log.error("Error accessing file size or metadata for {}: {}", logFilePath, e.getMessage());
            return new ArrayList<>(); // Return empty list on error
        }

        return newLines;
    }
}

