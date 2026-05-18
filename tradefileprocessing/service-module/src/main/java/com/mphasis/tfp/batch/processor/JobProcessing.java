package com.mphasis.tfp.batch.processor;

import com.mphasis.tfp.entity.FailedTransaction;
import com.mphasis.tfp.entity.SuccessfulTransaction;
import com.mphasis.tfp.entity.TransactionMetadata;
import com.mphasis.tfp.enums.FileUploadStatus;
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
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JobProcessing {

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
    public void processFile(MultipartFile file, Long fileLoadId) {
        log.info("========================================");
        log.info("Starting async file processing");
        log.info("File Name: {}", file.getOriginalFilename());
        log.info("File Size: {} bytes", file.getSize());
        log.info("File Load ID: {}", fileLoadId);
        log.info("========================================");

        int totalRecords = 0;
        int successCount = 0;
        int failureCount = 0;
        int lineNumber = 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {

            String line;
            long startTime = System.currentTimeMillis();

            log.info("Reading file line by line...");

            // Skip header line
            String header = reader.readLine();
            lineNumber++;
            log.debug("Header line: {}", header);

            while ((line = reader.readLine()) != null) {
                lineNumber++;

                // Skip empty lines
                if (line.trim().isEmpty()) {
                    log.debug("Line {}: Skipping empty line", lineNumber);
                    continue;
                }

                totalRecords++;

                try {
                    // Log progress every chunk size
                    if (totalRecords % chunkSize == 0) {
                        log.info("Progress: Processed {} records | Success: {} | Failed: {}",
                                totalRecords, successCount, failureCount);
                    }

                    // Parse line
                    String[] fields = line.split(delimiter, -1);

                    log.debug("Line {}: Raw data: {}", lineNumber, line);
                    log.debug("Line {}: Parsed into {} fields", lineNumber, fields.length);

                    // Validate fields
                    List<String> validationErrors = transactionValidator.validate(fields, lineNumber);

                    if (validationErrors.isEmpty()) {
                        // Create and save successful transaction
                        SuccessfulTransaction transaction = createSuccessfulTransaction(fields, fileLoadId, lineNumber);
                        successfulTransactionRepository.save(transaction);
                        successCount++;

                        log.debug("Line {}: Successfully processed transaction ID: {}",
                                lineNumber, transaction.getTransactionId());
                    } else {
                        // Create and save failed transaction
                        FailedTransaction failedTransaction = createFailedTransaction(
                                fields, fileLoadId, lineNumber, validationErrors);
                        failedTransactionRepository.save(failedTransaction);
                        failureCount++;

                        log.warn("Line {}: Validation failed with {} error(s)",
                                lineNumber, validationErrors.size());
                        log.warn("Line {}: Errors: {}", lineNumber, String.join(" | ", validationErrors));
                        log.warn("Line {}: Failed data: {}", lineNumber, line);
                    }

                } catch (Exception e) {
                    failureCount++;
                    log.error("Line {}: Unexpected error processing line", lineNumber, e);
                    log.error("Line {}: Problematic data: {}", lineNumber, line);

                    // Save as failed transaction
                    try {
                        String[] fields = line.split(delimiter, -1);
                        FailedTransaction failedTransaction = createFailedTransaction(
                                fields, fileLoadId, lineNumber,
                                Arrays.asList("Processing error: " + e.getMessage()));
                        failedTransactionRepository.save(failedTransaction);
                    } catch (Exception ex) {
                        log.error("Line {}: Could not save failed transaction", lineNumber, ex);
                    }
                }
            }

            long endTime = System.currentTimeMillis();
            long processingTime = endTime - startTime;

            log.info("========================================");
            log.info("File processing completed");
            log.info("Total lines read: {}", lineNumber);
            log.info("Total records processed: {}", totalRecords);
            log.info("Successful transactions: {}", successCount);
            log.info("Failed transactions: {}", failureCount);
            log.info("Processing time: {} ms ({} seconds)", processingTime, processingTime / 1000.0);
            log.info("Average time per record: {} ms", totalRecords > 0 ? processingTime / totalRecords : 0);
            log.info("========================================");

            // Update metadata
            updateMetadata(fileLoadId, totalRecords, successCount, failureCount);

        } catch (Exception e) {
            log.error("========================================");
            log.error("FATAL ERROR: File processing failed");
            log.error("File: {}", file.getOriginalFilename());
            log.error("Last processed line: {}", lineNumber);
            log.error("Total records processed before error: {}", totalRecords);
            log.error("Error details: ", e);
            log.error("========================================");

            updateMetadataOnFailure(fileLoadId, e.getMessage());
        }
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
                    lineNumber,
                    transaction.getTransactionId(),
                    transaction.getAccountNumber());

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

            // Determine error field
            if (!errors.isEmpty()) {
                String firstError = errors.get(0);
                if (firstError.contains("Transaction ID")) {
                    failedTransaction.setErrorField("transactionId");
                } else if (firstError.contains("Account Number")) {
                    failedTransaction.setErrorField("accountNumber");
                } else if (firstError.contains("Transaction Type")) {
                    failedTransaction.setErrorField("transactionType");
                } else if (firstError.contains("Cash Effect")) {
                    failedTransaction.setErrorField("cashEffect");
                } else if (firstError.contains("Balance")) {
                    failedTransaction.setErrorField("balance");
                } else {
                    failedTransaction.setErrorField("unknown");
                }
            }

            log.trace("Line {}: Created FailedTransaction - TxnID: {}, Error: {}",
                    lineNumber,
                    failedTransaction.getTransactionId(),
                    failedTransaction.getErrorMessage());

        } catch (Exception e) {
            log.error("Line {}: Error creating failed transaction", lineNumber, e);
        }

        return failedTransaction;
    }

    private void updateMetadata(Long fileLoadId, int totalRecords, int successCount, int failureCount) {
        try {
            TransactionMetadata metadata = metadataRepository.findById(fileLoadId)
                    .orElseThrow(() -> new RuntimeException("Metadata not found for fileId: " + fileLoadId));

            metadata.setTotalRecords(totalRecords);
            metadata.setSuccessCount(successCount);
            metadata.setErrorCount(failureCount);

            if (failureCount == 0) {
                metadata.setStatus(statusCompleted);
                log.info("File status: COMPLETED - All {} records processed successfully", totalRecords);
            } else if (successCount > 0) {
                metadata.setStatus(statusPartiallyCompleted);
                log.warn("File status: PARTIALLY_COMPLETED - {} successful, {} failed",
                        successCount, failureCount);
            } else {
                metadata.setStatus(statusFailed);
                log.error("File status: FAILED - All {} records failed validation", totalRecords);
            }

            metadataRepository.save(metadata);

        } catch (Exception e) {
            log.error("Error updating metadata for fileId: {}", fileLoadId, e);
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
}