package com.example.qlogserver.service;

import com.example.qlogserver.model.TransactionRecord;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service responsible for parsing CSV log lines into TransactionRecord objects.
 */
@Service
public class CsvParsingService {

    private static final Logger log = LoggerFactory.getLogger(CsvParsingService.class);

    // Expected header fields (adjust based on actual QServerTrans format)
    // Example headers - THESE MUST MATCH THE ACTUAL LOG FILE HEADERS
    private static final String[] EXPECTED_HEADERS = {
            "TransactionId", "TransactionKind", "Status", "StartTime", "EndTime",
            "Length", "WaitingTime", "ProcTime", "FuncTime", "DBTime", "StreamTime",
            "KernelTime", "MemoryCommitTime", "CleanupTime", "BeginTime", "EndTimeMetric", // Renamed from End
            "ClientId", "Username", "IPClient", "ClientType",
            "ActionElementName", "ActionElementType", "ActionName",
            "DatasetName", "DatasetReadCount", "DatasetWriteCount", "DatasetReadTime", "DatasetWriteTime",
            "ThreadId", "MemoryUsageStart", "MemoryUsageEnd",
            "ObjectsCreated", "ObjectsDeleted", "ObjectsUpdated", "ObjectsRead"
            // Add any other headers present in the log file
    };

    /**
     * Parses a list of CSV log lines into TransactionRecord objects.
     *
     * @param logLines         The list of raw CSV strings.
     * @param agentId          The ID of the agent submitting the logs.
     * @param environmentContext The environment context for these logs.
     * @param sourceLogFile    The name of the source log file.
     * @return A list of parsed TransactionRecord objects.
     */
    public List<TransactionRecord> parseLogLines(List<String> logLines, String agentId, String environmentContext, String sourceLogFile) {
        List<TransactionRecord> records = new ArrayList<>();
        if (logLines == null || logLines.isEmpty()) {
            return records;
        }

        Map<String, Integer> headerMap = null;
        int lineIndex = 0;

        for (String line : logLines) {
            lineIndex++;
            if (line == null || line.isBlank()) {
                continue;
            }

            try (CSVReader reader = new CSVReader(new StringReader(line))) {
                String[] values = reader.readNext(); // Reads the single line
                if (values == null) continue;

                // First non-blank line is assumed to be the header
                if (headerMap == null) {
                    headerMap = createHeaderMap(values);
                    log.debug("Parsed header map for context 		{}		, file 		{}		: {}", environmentContext, sourceLogFile, headerMap.keySet());
                    // Skip processing the header line itself as a record
                    continue;
                }

                // Process data line
                TransactionRecord record = mapValuesToRecord(values, headerMap, lineIndex);
                if (record != null) {
                    record.setAgentId(agentId);
                    record.setEnvironmentContext(environmentContext); // Set context
                    record.setSourceLogFile(sourceLogFile);
                    records.add(record);
                }

            } catch (CsvValidationException | IOException e) {
                log.warn("Failed to parse CSV line {} for context 		{}		, file 		{}		: 		{}		. Line: 		{}		", lineIndex, environmentContext, sourceLogFile, e.getMessage(), line);
            } catch (Exception e) {
                log.error("Unexpected error parsing CSV line {} for context 		{}		, file 		{}		: 		{}		. Line: 		{}		", lineIndex, environmentContext, sourceLogFile, e.getMessage(), line, e);
            }
        }

        log.info("Parsed {} records from {} lines for context 		{}		, file 		{}		.", records.size(), logLines.size(), environmentContext, sourceLogFile);
        return records;
    }

    /**
     * Creates a map from header name to column index.
     *
     * @param headers Array of header strings.
     * @return Map where key is header name (lowercase) and value is index.
     */
    private Map<String, Integer> createHeaderMap(String[] headers) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            if (headers[i] != null) {
                map.put(headers[i].trim().toLowerCase(), i); // Use lowercase for case-insensitive matching
            }
        }
        // Validate against expected headers (optional but recommended)
        // for (String expected : EXPECTED_HEADERS) {
        //     if (!map.containsKey(expected.toLowerCase())) {
        //         log.warn("Expected header 		{}		 not found in CSV.", expected);
        //     }
        // }
        return map;
    }

    /**
     * Maps CSV values to a TransactionRecord object based on the header map.
     *
     * @param values    Array of values from a CSV line.
     * @param headerMap Map of header names to indices.
     * @param lineIndex The original line number (for logging).
     * @return A populated TransactionRecord, or null if mapping fails.
     */
    private TransactionRecord mapValuesToRecord(String[] values, Map<String, Integer> headerMap, int lineIndex) {
        TransactionRecord record = new TransactionRecord();
        record.setCsvIndex(lineIndex);

        try {
            // Map values using helper methods
            record.setTransactionId(getStringValue(values, headerMap, "transactionid"));
            record.setTransactionKind(getStringValue(values, headerMap, "transactionkind"));
            record.setStatus(getStringValue(values, headerMap, "status"));
            record.setStartTimeRaw(getStringValue(values, headerMap, "starttime"));
            record.setEndTimeRaw(getStringValue(values, headerMap, "endtime"));

            record.setLength(getDoubleValue(values, headerMap, "length"));
            record.setWaitingTime(getDoubleValue(values, headerMap, "waitingtime"));
            record.setProcTime(getDoubleValue(values, headerMap, "proctime"));
            record.setFuncTime(getDoubleValue(values, headerMap, "functime"));
            record.setDbTime(getDoubleValue(values, headerMap, "dbtime"));
            record.setStreamTime(getDoubleValue(values, headerMap, "streamtime"));
            record.setKernelTime(getDoubleValue(values, headerMap, "kerneltime"));
            record.setMemoryCommitTime(getDoubleValue(values, headerMap, "memorycommittime"));
            record.setCleanupTime(getDoubleValue(values, headerMap, "cleanuptime"));
            record.setBeginTime(getDoubleValue(values, headerMap, "begintime"));
            record.setEndTimeMetric(getDoubleValue(values, headerMap, "endtimemetric")); // Use the renamed field

            record.setClientId(getStringValue(values, headerMap, "clientid"));
            record.setUsername(getStringValue(values, headerMap, "username"));
            record.setIpClient(getStringValue(values, headerMap, "ipclient"));
            record.setClientType(getStringValue(values, headerMap, "clienttype"));

            record.setActionElementName(getStringValue(values, headerMap, "actionelementname"));
            record.setActionElementType(getStringValue(values, headerMap, "actionelementtype"));
            record.setActionName(getStringValue(values, headerMap, "actionname"));

            record.setDatasetName(getStringValue(values, headerMap, "datasetname"));
            record.setDatasetReadCount(getIntegerValue(values, headerMap, "datasetreadcount"));
            record.setDatasetWriteCount(getIntegerValue(values, headerMap, "datasetwritecount"));
            record.setDatasetReadTime(getDoubleValue(values, headerMap, "datasetreadtime"));
            record.setDatasetWriteTime(getDoubleValue(values, headerMap, "datasetwritetime"));

            record.setThreadId(getIntegerValue(values, headerMap, "threadid"));
            record.setMemoryUsageStart(getLongValue(values, headerMap, "memoryusagestart"));
            record.setMemoryUsageEnd(getLongValue(values, headerMap, "memoryusageend"));
            record.setObjectsCreated(getIntegerValue(values, headerMap, "objectscreated"));
            record.setObjectsDeleted(getIntegerValue(values, headerMap, "objectsdeleted"));
            record.setObjectsUpdated(getIntegerValue(values, headerMap, "objectsupdated"));
            record.setObjectsRead(getIntegerValue(values, headerMap, "objectsread"));

            // Attempt to parse dates (optional, handle potential errors)
            // tryParseAndSetDates(record);

            return record;
        } catch (Exception e) {
            log.warn("Error mapping values for line {}: {}. Values: {}", lineIndex, e.getMessage(), String.join(",", values));
            return null;
        }
    }

    // --- Helper methods for safe value retrieval and type conversion ---

    private String getStringValue(String[] values, Map<String, Integer> headerMap, String headerName) {
        Integer index = headerMap.get(headerName.toLowerCase());
        if (index != null && index < values.length && values[index] != null) {
            return values[index].trim();
        }
        return null;
    }

    private Double getDoubleValue(String[] values, Map<String, Integer> headerMap, String headerName) {
        String value = getStringValue(values, headerMap, headerName);
        if (value != null && !value.isBlank()) {
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException e) {
                log.trace("Could not parse double for header 		{}		: 		{}		", headerName, value);
            }
        }
        return null;
    }

    private Integer getIntegerValue(String[] values, Map<String, Integer> headerMap, String headerName) {
        String value = getStringValue(values, headerMap, headerName);
        if (value != null && !value.isBlank()) {
            try {
                // Handle potential decimal values if logs sometimes include them
                if (value.contains(".")) {
                    return (int) Double.parseDouble(value);
                }
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                log.trace("Could not parse integer for header 		{}		: 		{}		", headerName, value);
            }
        }
        return null;
    }

    private Long getLongValue(String[] values, Map<String, Integer> headerMap, String headerName) {
        String value = getStringValue(values, headerMap, headerName);
        if (value != null && !value.isBlank()) {
            try {
                 if (value.contains(".")) {
                    return (long) Double.parseDouble(value);
                }
                return Long.parseLong(value);
            } catch (NumberFormatException e) {
                log.trace("Could not parse long for header 		{}		: 		{}		", headerName, value);
            }
        }
        return null;
    }

    // Optional: Add date parsing logic here if needed, handling potential format issues
    // private void tryParseAndSetDates(TransactionRecord record) { ... }
}

