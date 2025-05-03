package com.example.qlogserver.service;

import com.example.qlogserver.model.TransactionRecord;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for generating Excel reports based on transaction data.
 * Adapts logic from the previous monolithic implementation.
 */
@Service
public class ReportingService {

    private static final Logger log = LoggerFactory.getLogger(ReportingService.class);

    private final TransactionService transactionService;

    // Define headers for the Excel report
    private static final String[] HEADERS = {
            "Transaction Kind", "Count", "Average Length (ms)", "Average Waiting Time (ms)",
            "Average Proc Time (ms)", "Average Func Time (ms)", "Average DB Time (ms)",
            "Average Stream Time (ms)", "Average Kernel Time (ms)", "Average Mem Commit Time (ms)",
            "Average Cleanup Time (ms)", "Average Begin Time (ms)", "Average End Time (ms)"
    };

    @Autowired
    public ReportingService(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    /**
     * Generates a summary report grouped by transaction kind.
     *
     * @param filters Filters to apply before generating the report.
     * @return A ByteArrayInputStream containing the generated Excel report.
     * @throws IOException If an error occurs during Excel generation.
     */
    public ByteArrayInputStream generateSummaryByKindReport(Map<String, String> filters) throws IOException {
        List<TransactionRecord> records = transactionService.findAllTransactions(filters);
        log.info("Generating summary report for {} records matching filters.", records.size());

        // Group records by transaction kind and calculate aggregates
        Map<String, List<TransactionRecord>> groupedData = records.stream()
                .filter(r -> r.getTransactionKind() != null)
                .collect(Collectors.groupingBy(TransactionRecord::getTransactionKind));

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Summary by Transaction Kind");

            // Create Header Row
            Row headerRow = sheet.createRow(0);
            CellStyle headerCellStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerCellStyle.setFont(headerFont);

            for (int col = 0; col < HEADERS.length; col++) {
                Cell cell = headerRow.createCell(col);
                cell.setCellValue(HEADERS[col]);
                cell.setCellStyle(headerCellStyle);
            }

            // Create Data Rows
            int rowIdx = 1;
            for (Map.Entry<String, List<TransactionRecord>> entry : groupedData.entrySet()) {
                String kind = entry.getKey();
                List<TransactionRecord> kindRecords = entry.getValue();
                int count = kindRecords.size();

                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(kind);
                row.createCell(1).setCellValue(count);

                // Calculate averages safely
                row.createCell(2).setCellValue(calculateAverage(kindRecords, TransactionRecord::getLength));
                row.createCell(3).setCellValue(calculateAverage(kindRecords, TransactionRecord::getWaitingTime));
                row.createCell(4).setCellValue(calculateAverage(kindRecords, TransactionRecord::getProcTime));
                row.createCell(5).setCellValue(calculateAverage(kindRecords, TransactionRecord::getFuncTime));
                row.createCell(6).setCellValue(calculateAverage(kindRecords, TransactionRecord::getDbTime));
                row.createCell(7).setCellValue(calculateAverage(kindRecords, TransactionRecord::getStreamTime));
                row.createCell(8).setCellValue(calculateAverage(kindRecords, TransactionRecord::getKernelTime));
                row.createCell(9).setCellValue(calculateAverage(kindRecords, TransactionRecord::getMemoryCommitTime));
                row.createCell(10).setCellValue(calculateAverage(kindRecords, TransactionRecord::getCleanupTime));
                row.createCell(11).setCellValue(calculateAverage(kindRecords, TransactionRecord::getBeginTime));
                row.createCell(12).setCellValue(calculateAverage(kindRecords, TransactionRecord::getEndTimeMetric));
            }

            // Auto-size columns
            for (int col = 0; col < HEADERS.length; col++) {
                sheet.autoSizeColumn(col);
            }

            workbook.write(out);
            log.info("Excel report generated successfully.");
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            log.error("Error generating Excel report: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Helper method to calculate the average of a specific metric for a list of records.
     *
     * @param records List of records.
     * @param mapper  Function to extract the Double metric from a record.
     * @return The calculated average, or 0.0 if no valid data.
     */
    private double calculateAverage(List<TransactionRecord> records, java.util.function.Function<TransactionRecord, Double> mapper) {
        return records.stream()
                .map(mapper)
                .filter(value -> value != null && !Double.isNaN(value) && !Double.isInfinite(value))
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
    }
}

