package com.mphasis.tfp.batch.processor;

import com.mphasis.tfp.entity.FailedTransaction;
import com.mphasis.tfp.entity.SuccessfulTransaction;
import com.mphasis.tfp.entity.TransactionMetadata;
import com.mphasis.tfp.repository.FailedTransactionRepository;
import com.mphasis.tfp.repository.SuccessfulTransactionRepository;
import com.mphasis.tfp.repository.TransactionMetadataRepository;
import com.mphasis.tfp.validation.TransactionValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class JobProcessing {

    private static final String LOG_SEPARATOR = "========================================";
    private static final String ERROR_FIELD_GENERAL = "general";

    private final TransactionMetadataRepository metadataRepository;
    private final SuccessfulTransactionRepository successfulTransactionRepository;
    private final FailedTransactionRepository failedTransactionRepository;
    private final TransactionValidator transactionValidator;

    @Value("${app.file.delimiter}")
    private String delimiter;

    @Value("${app.status.completed}")
    private String statusCompleted;

    @Value("${app.status.partially-completed}")
    private String statusPartiallyCompleted;

    @Value("${app.status.failed}")
    private String statusFailed;

    @Value("${app.batch.chunk-size}")
    private int chunkSize;

    @Value("${app.transaction.field.transaction-id-index}")
    private int transactionIdIndex;

    @Value("${app.transaction.field.account-number-index}")
    private int accountNumberIndex;

    @Async
    public void processFile(MultipartFile file, Long fileLoadId, Long userId) {
        logFileProcessingStart(file, fileLoadId);

        ProcessingStats stats = new ProcessingStats();
        long startTime = System.currentTimeMillis();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            processFileLines(reader, fileLoadId, stats);

            long processingTime = System.currentTimeMillis() - startTime;
            logFileProcessingComplete(stats, processingTime);

            updateMetadata(fileLoadId, stats.totalRecords, stats.successCount, stats.failureCount);

        } catch (Exception e) {
            logFileProcessingError(file, stats.lineNumber, stats.totalRecords, e);
            updateMetadataOnFailure(fileLoadId, e.getMessage());
        }
    }

    private void processFileLines(BufferedReader reader, Long fileLoadId, ProcessingStats stats) throws Exception {
        String line;

        String header = reader.readLine();
        stats.lineNumber++;
        log.debug("Header line: {}", header);

        while ((line = reader.readLine()) != null) {
            stats.lineNumber++;

            if (line.trim().isEmpty()) {
                log.debug("Line {}: Skipping empty line", stats.lineNumber);
                continue;
            }

            stats.totalRecords++;
            logProgressIfNeeded(stats);

            processTransaction(line, fileLoadId, stats);
        }
    }

    private void processTransaction(String line, Long fileLoadId, ProcessingStats stats) {
        try {
            String[] fields = line.split(delimiter, -1);

            log.debug("Line {}: Raw data: {}", stats.lineNumber, line);
            log.debug("Line {}: Parsed into {} fields", stats.lineNumber, fields.length);

            List<String> validationErrors = transactionValidator.validate(fields, stats.lineNumber);

            if (validationErrors.isEmpty()) {
                processSuccessfulTransaction(fields, fileLoadId, stats);
            } else {
                processFailedTransaction(fields, fileLoadId, stats, validationErrors, line);
            }

        } catch (Exception e) {
            stats.failureCount++;
            log.error("Line {}: Unexpected error processing line", stats.lineNumber, e);
            log.error("Line {}: Problematic data: {}", stats.lineNumber, line);

            saveFailedTransactionSafely(line, fileLoadId, stats, e);
        }
    }

    private void processSuccessfulTransaction(String[] fields, Long fileLoadId, ProcessingStats stats) {
        SuccessfulTransaction transaction = createSuccessfulTransaction(fields, fileLoadId, stats.lineNumber);
        successfulTransactionRepository.save(transaction);
        stats.successCount++;

        log.debug("Line {}: Successfully processed transaction ID: {}",
                stats.lineNumber, transaction.getTransactionId());
    }

    private void processFailedTransaction(String[] fields, Long fileLoadId, ProcessingStats stats,
                                          List<String> validationErrors, String line) {
        FailedTransaction failedTransaction = createFailedTransaction(
                fields, fileLoadId, stats.lineNumber, validationErrors);
        failedTransactionRepository.save(failedTransaction);
        stats.failureCount++;

        log.warn("Line {}: Validation failed with {} error(s)", stats.lineNumber, validationErrors.size());
        log.warn("Line {}: Errors: {}", stats.lineNumber, String.join(" | ", validationErrors));
        log.warn("Line {}: Failed data: {}", stats.lineNumber, line);
    }

    private void saveFailedTransactionSafely(String line, Long fileLoadId, ProcessingStats stats, Exception originalError) {
        try {
            String[] fields = line.split(delimiter, -1);
            FailedTransaction failedTransaction = createFailedTransaction(
                    fields, fileLoadId, stats.lineNumber,
                    Arrays.asList("Processing error: " + originalError.getMessage()));
            failedTransactionRepository.save(failedTransaction);
        } catch (Exception ex) {
            log.error("Line {}: Could not save failed transaction", stats.lineNumber, ex);
        }
    }

    private void logProgressIfNeeded(ProcessingStats stats) {
        if (stats.totalRecords % chunkSize == 0) {
            log.info("Progress: Processed {} records | Success: {} | Failed: {}",
                    stats.totalRecords, stats.successCount, stats.failureCount);
        }
    }

    private void logFileProcessingStart(MultipartFile file, Long fileLoadId) {
        log.info(LOG_SEPARATOR);
        log.info("Starting async file processing");
        log.info("File Name: {}", file.getOriginalFilename());
        log.info("File Size: {} bytes", file.getSize());
        log.info("File Load ID: {}", fileLoadId);
        log.info(LOG_SEPARATOR);
    }

    private void logFileProcessingComplete(ProcessingStats stats, long processingTime) {
        log.info(LOG_SEPARATOR);
        log.info("File processing completed");
        log.info("Total lines read: {}", stats.lineNumber);
        log.info("Total records processed: {}", stats.totalRecords);
        log.info("Successful transactions: {}", stats.successCount);
        log.info("Failed transactions: {}", stats.failureCount);
        log.info("Processing time: {} ms ({} seconds)", processingTime, processingTime / 1000.0);
        log.info("Average time per record: {} ms",
                stats.totalRecords > 0 ? processingTime / stats.totalRecords : 0);
        log.info(LOG_SEPARATOR);
    }

    private void logFileProcessingError(MultipartFile file, int lineNumber, int totalRecords, Exception e) {
        log.error(LOG_SEPARATOR);
        log.error("FATAL ERROR: File processing failed");
        log.error("File: {}", file.getOriginalFilename());
        log.error("Last processed line: {}", lineNumber);
        log.error("Total records processed before error: {}", totalRecords);
        log.error("Error details: ", e);
        log.error(LOG_SEPARATOR);
    }

    private SuccessfulTransaction createSuccessfulTransaction(String[] fields, Long fileLoadId, int lineNumber) {
        SuccessfulTransaction transaction = new SuccessfulTransaction();

        try {
            transaction.setFileLoadId(fileLoadId);
            transaction.setTransactionId(getField(fields, 0));
            transaction.setFileHeaderDate(getField(fields, 1));
            transaction.setAccountNumber(getField(fields, 2));
            transaction.setTransactionType(parseInteger(getField(fields, 3)));
            transaction.setBatchLocation(getField(fields, 4));
            transaction.setBatchNumber(parseInteger(getField(fields, 5)));
            transaction.setUpdateBatchDate(parseInteger(getField(fields, 6)));
            transaction.setActionName(getField(fields, 7));
            transaction.setRelatedFileKey(parseInteger(getField(fields, 8)));
            transaction.setDoNotReportFlag(getField(fields, 9));
            transaction.setOwningPortfolio(parseInteger(getField(fields, 10)));
            transaction.setPosterInitials(getField(fields, 11));
            transaction.setTransactionSubtype(parseInteger(getField(fields, 12)));
            transaction.setCashEffect(parseBigDecimal(getField(fields, 13)));
            transaction.setOldBalance(parseBigDecimal(getField(fields, 14)));
            transaction.setNewBalance(parseBigDecimal(getField(fields, 15)));

            log.trace("Line {}: Created SuccessfulTransaction - TxnID: {}, Account: {}",
                    lineNumber, transaction.getTransactionId(), transaction.getAccountNumber());

        } catch (Exception e) {
            log.error("Line {}: Error creating successful transaction", lineNumber, e);
            throw e;
        }

        return transaction;
    }

    private FailedTransaction createFailedTransaction(String[] fields, Long fileLoadId,
                                                      int lineNumber, List<String> errors) {
        FailedTransaction failedTransaction = new FailedTransaction();

        try {
            failedTransaction.setFileLoadId(fileLoadId);
            failedTransaction.setTransactionId(getField(fields, transactionIdIndex));
            failedTransaction.setAccountNumber(getField(fields, accountNumberIndex));
            failedTransaction.setRecordCount(lineNumber);
            failedTransaction.setErrorMessage(String.join("; ", errors));
            failedTransaction.setStatus(statusFailed);

            if (!errors.isEmpty()) {
                failedTransaction.setErrorField(determineErrorField(errors.get(0)));
            }

            log.trace("Line {}: Created FailedTransaction - TxnID: {}, ErrorField: {}, Error: {}",
                    lineNumber, failedTransaction.getTransactionId(),
                    failedTransaction.getErrorField(), failedTransaction.getErrorMessage());

        } catch (Exception e) {
            log.error("Line {}: Error creating failed transaction", lineNumber, e);
        }

        return failedTransaction;
    }

    private String determineErrorField(String firstError) {
        String errorLower = firstError.toLowerCase();

        Map<String, String> errorFieldMap = createErrorFieldMap();

        return errorFieldMap.entrySet().stream()
                .filter(entry -> errorLower.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(ERROR_FIELD_GENERAL);
    }

    private Map<String, String> createErrorFieldMap() {
        Map<String, String> errorFieldMap = new HashMap<>();
        errorFieldMap.put("transaction id", "transactionId");
        errorFieldMap.put("file header date", "fileHeaderDate");
        errorFieldMap.put("account number", "accountNumber");
        errorFieldMap.put("transaction type", "transactionType");
        errorFieldMap.put("batch number", "batchNumber");
        errorFieldMap.put("action name", "actionName");
        errorFieldMap.put("do not report flag", "doNotReportFlag");
        errorFieldMap.put("owning portfolio", "owningPortfolio");
        errorFieldMap.put("transaction subtype", "transactionSubtype");
        errorFieldMap.put("cash effect", "cashEffect");
        errorFieldMap.put("old balance", "oldBalance");
        errorFieldMap.put("new balance", "newBalance");
        errorFieldMap.put("balance mismatch", "balanceMismatch");
        errorFieldMap.put("insufficient fields", "fieldCount");
        errorFieldMap.put("processing error", "processingError");
        return errorFieldMap;
    }

    private void updateMetadata(Long fileLoadId, int totalRecords, int successCount, int failureCount) {
        try {
            TransactionMetadata metadata = metadataRepository.findById(fileLoadId)
                    .orElseThrow(() -> new RuntimeException("Metadata not found for fileId: " + fileLoadId));

            metadata.setTotalRecords(totalRecords);
            metadata.setSuccessCount(successCount);
            metadata.setErrorCount(failureCount);
            metadata.setStatus(determineFileStatus(totalRecords, successCount, failureCount));

            metadataRepository.save(metadata);

        } catch (Exception e) {
            log.error("Error updating metadata for fileId: {}", fileLoadId, e);
        }
    }

    private String determineFileStatus(int totalRecords, int successCount, int failureCount) {
        if (failureCount == 0) {
            log.info("File status: COMPLETED - All {} records processed successfully", totalRecords);
            return statusCompleted;
        } else if (successCount > 0) {
            log.warn("File status: PARTIALLY_COMPLETED - {} successful, {} failed",
                    successCount, failureCount);
            return statusPartiallyCompleted;
        } else {
            log.error("File status: FAILED - All {} records failed validation", totalRecords);
            return statusFailed;
        }
    }

    private void updateMetadataOnFailure(Long fileLoadId, String errorMessage) {
        try {
            metadataRepository.findById(fileLoadId).ifPresent(metadata -> {
                metadata.setStatus(statusFailed);
                metadataRepository.save(metadata);
                log.error("Updated file status to FAILED due to error: {}", errorMessage);
            });
        } catch (Exception e) {
            log.error("Error updating metadata on failure for fileId: {}", fileLoadId, e);
        }
    }

    private String getField(String[] fields, int index) {
        try {
            return fields != null && index < fields.length ? fields[index] : null;
        } catch (Exception e) {
            return null;
        }
    }

    private Integer parseInteger(String value) {
        try {
            return value != null && !value.trim().isEmpty() ? Integer.parseInt(value.trim()) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private BigDecimal parseBigDecimal(String value) {
        try {
            return value != null && !value.trim().isEmpty() ? new BigDecimal(value.trim()) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static class
    ProcessingStats {
        int totalRecords = 0;
        int successCount = 0;
        int failureCount = 0;
        int lineNumber = 0;
    }
}