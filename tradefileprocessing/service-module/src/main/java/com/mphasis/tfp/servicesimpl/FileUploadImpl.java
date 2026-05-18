package com.mphasis.tfp.servicesimpl;
import com.mphasis.tfp.entity.*;
import com.mphasis.tfp.repository.ArchivedTransactionRepository;
import com.mphasis.tfp.repository.SuccessfulTransactionRepository;
import com.mphasis.tfp.batch.processor.JobProcessing;
import com.mphasis.tfp.dto.FileLoadMetaDataResponse;
import com.mphasis.tfp.dto.UploadResponseDTO;
import com.mphasis.tfp.enums.FileUploadStatus;
import com.mphasis.tfp.exception.EmptyFileException;
import com.mphasis.tfp.exception.InvalidFileFormatException;
import com.mphasis.tfp.mapping.ServiceMapper;
import com.mphasis.tfp.repository.TransactionMetadataRepository;
import com.mphasis.tfp.services.IFileUpload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.mphasis.tfp.dto.ErrorResponseDTO;
import com.mphasis.tfp.entity.ArchivedTransaction;
import com.mphasis.tfp.entity.SuccessfulTransaction;
import com.mphasis.tfp.repository.ArchivedTransactionRepository;
import com.mphasis.tfp.repository.FailedTransactionRepository;
import com.mphasis.tfp.repository.SuccessfulTransactionRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileUploadImpl implements IFileUpload {

    private final TransactionMetadataRepository metadataRepository;
    private final JobProcessing jobProcessing;
    private final ServiceMapper serviceMapper;
    private final SuccessfulTransactionRepository successfulTransactionRepository;
    private final ArchivedTransactionRepository archivedTransactionRepository;
    private final FailedTransactionRepository failedTransactionRepository;


    @Value("${app.file.max-size}")
    private long maxFileSize;

    @Value("${app.file.max-size-mb}")
    private long maxFileSizeMb;

    @Value("${app.file.allowed-extensions}")
    private String allowedExtensions;

    @Value("${app.file.allow-duplicate}")
    private boolean allowDuplicate;

    @Value("${app.error.file-empty}")
    private String errorFileEmpty;

    @Value("${app.error.file-too-large}")
    private String errorFileTooLarge;

    @Value("${app.error.file-not-found}")
    private String errorFileNotFound;

    @Value("${app.error.duplicate-file}")
    private String errorDuplicateFile;

    @Value("${app.error.invalid-file-name}")
    private String errorInvalidFileName;

    @Value("${app.error.no-file-provided}")
    private String errorNoFileProvided;

    @Value("${app.status.processing}")
    private String statusProcessing;

    @Override
    @Transactional
    public UploadResponseDTO uploadFile(MultipartFile file) {
        log.info("========================================");
        log.info("File upload request received");
        log.info("File name: {}", file != null ? file.getOriginalFilename() : "null");
        log.info("File size: {} bytes", file != null ? file.getSize() : 0);
        log.info("========================================");

        // Validate file
        log.info("Step 1: Validating file...");
        validateFile(file);
        log.info("Step 1: File validation passed");

        // Check for duplicates if not allowed
        if (!allowDuplicate) {
            log.info("Step 2: Checking for duplicate files...");
            checkDuplicateFile(file.getOriginalFilename());
            log.info("Step 2: No duplicate file found");
        } else {
            log.info("Step 2: Duplicate file check skipped (allowed)");
        }

        // Create metadata entry
        log.info("Step 3: Creating metadata entry...");
        TransactionMetadata metadata = createMetadata(file);
        metadata = metadataRepository.save(metadata);
        log.info("Step 3: Metadata created with File ID: {}", metadata.getFileId());

        // Process file asynchronously
        log.info("Step 4: Starting asynchronous file processing...");
        jobProcessing.processFile(file, metadata.getFileId());
        log.info("Step 4: File processing initiated");

        log.info("========================================");
        log.info("File upload completed successfully");
        log.info("File ID: {}", metadata.getFileId());
        log.info("Status: {}", metadata.getStatus());
        log.info("========================================");

        return serviceMapper.toUploadResponseDTO(metadata,
                "File uploaded successfully and processing started");
    }

    @Override
    public FileLoadMetaDataResponse getFileStatus(Long fileId) {
        log.info("File status request received for File ID: {}", fileId);

        if (fileId == null || fileId <= 0) {
            log.error("Invalid file ID provided: {}", fileId);
            throw new IllegalArgumentException("Invalid file ID. File ID must be a positive number.");
        }

        log.debug("Fetching metadata from database for File ID: {}", fileId);
        TransactionMetadata metadata = metadataRepository.findById(fileId)
                .orElseThrow(() -> {
                    log.error("File not found with ID: {}", fileId);
                    return new NoSuchElementException(String.format(errorFileNotFound, fileId));
                });

        log.info("File status retrieved - ID: {}, Status: {}, Total: {}, Success: {}, Failed: {}",
                metadata.getFileId(),
                metadata.getStatus(),
                metadata.getTotalRecords(),
                metadata.getSuccessCount(),
                metadata.getErrorCount());

        return serviceMapper.toFileLoadMetaDataResponse(metadata);
    }

    @Override
    public List<FileLoadMetaDataResponse> searchFiles(
            Long fileId,
            String fileName,
            LocalDate uploadDateFrom,
            LocalDate uploadDateTo,
            String status) {

        log.info("File search request received");
        log.info("Search criteria - File ID: {}, File Name: {}, Date From: {}, Date To: {}, Status: {}",
                fileId, fileName, uploadDateFrom, uploadDateTo, status);

        if (uploadDateFrom != null && uploadDateTo != null && uploadDateFrom.isAfter(uploadDateTo)) {
            log.error("Invalid date range: From ({}) is after To ({})", uploadDateFrom, uploadDateTo);
            throw new IllegalArgumentException("Start date cannot be after end date");
        }

        if (status != null && !status.trim().isEmpty()) {
            log.debug("Validating status value: {}", status);
            validateStatus(status);
        }

        LocalDateTime startDateTime = uploadDateFrom != null ?
                uploadDateFrom.atStartOfDay() : null;
        LocalDateTime endDateTime = uploadDateTo != null ?
                uploadDateTo.atTime(LocalTime.MAX) :
                (uploadDateFrom != null ? LocalDate.now().atTime(LocalTime.MAX) : null);

        log.debug("Resolved datetime range: {} to {}", startDateTime, endDateTime);


        List<TransactionMetadata> metadataList = metadataRepository.searchFiles(
                fileId, fileName, startDateTime, endDateTime, status);

        log.info("Search completed - Found {} file(s)", metadataList.size());

        return serviceMapper.toFileLoadMetaDataResponseList(metadataList);
    }
    @Override
    @Transactional
    public String archiveFile(Long fileId) {
        log.info("========================================");
        log.info("Archive request received for File ID: {}", fileId);

        // Step 1: Validate fileId
        if (fileId == null || fileId <= 0) {
            log.error("Invalid file ID: {}", fileId);
            throw new IllegalArgumentException("Invalid file ID. File ID must be a positive number.");
        }

        // Step 2: Check file exists
        TransactionMetadata metadata = metadataRepository.findById(fileId)
                .orElseThrow(() -> {
                    log.error("File not found with ID: {}", fileId);
                    return new NoSuchElementException(String.format(errorFileNotFound, fileId));
                });

        log.info("File found - ID: {}, Total: {}, Success: {}, Error: {}, Status: {}",
                metadata.getFileId(), metadata.getTotalRecords(),
                metadata.getSuccessCount(), metadata.getErrorCount(), metadata.getStatus());

        // Step 3: Check already archived
        if ("ARCHIVED".equalsIgnoreCase(metadata.getStatus())) {
            log.warn("File ID {} is already archived", fileId);
            throw new IllegalStateException("File ID " + fileId + " has already been archived.");
        }

        // Step 4: Check all transactions are successful
        if (metadata.getErrorCount() > 0 || !metadata.getSuccessCount().equals(metadata.getTotalRecords())) {
            log.error("File ID {} cannot be archived - Success: {}, Total: {}, Errors: {}",
                    fileId, metadata.getSuccessCount(), metadata.getTotalRecords(), metadata.getErrorCount());
            throw new IllegalStateException(
                    "File ID " + fileId + " cannot be archived. All transactions must be successful. " +
                            "Total: " + metadata.getTotalRecords() +
                            ", Successful: " + metadata.getSuccessCount() +
                            ", Failed: " + metadata.getErrorCount());
        }


        // Step 5: Fetch successful transactions
        List<SuccessfulTransaction> successfulTransactions =
                successfulTransactionRepository.findByFileLoadId(fileId);

        if (successfulTransactions.isEmpty()) {
            log.error("No successful transactions found for File ID: {}", fileId);
            throw new NoSuchElementException("No successful transactions found for File ID: " + fileId);
        }

        log.info("Step 1: Copying {} transactions to archive...", successfulTransactions.size());

        // Step 6: Map and save to archive table
        List<ArchivedTransaction> archivedTransactions = successfulTransactions.stream()
                .map(serviceMapper::toArchivedTransaction)
                .collect(java.util.stream.Collectors.toList());

        archivedTransactionRepository.saveAll(archivedTransactions);
        log.info("Step 2: {} transactions saved to archive", archivedTransactions.size());

        // Step 7: Delete from successful transactions
        successfulTransactionRepository.deleteByFileLoadId(fileId);
        log.info("Step 3: Deleted {} transactions from successful_transactions", successfulTransactions.size());

        // Step 8: Update file metadata status to ARCHIVED
        metadata.setStatus("ARCHIVED");
        metadataRepository.save(metadata);
        log.info("Step 4: File ID {} status updated to ARCHIVED", fileId);

        log.info("Archive completed successfully for File ID: {}", fileId);
        log.info("========================================");

        return "Successfully archived " + archivedTransactions.size() +
                " transactions for File ID: " + fileId;
    }
    @Override
    @Transactional
    public String deleteTransactions(Long fileId) {
        log.info("========================================");
        log.info("Soft delete request received for File ID: {}", fileId);

        // Step 1: Validate fileId
        if (fileId == null || fileId <= 0) {
            log.error("Invalid file ID: {}", fileId);
            throw new IllegalArgumentException("Invalid file ID. File ID must be a positive number.");
        }

        // Step 2: Check file exists
        TransactionMetadata metadata = metadataRepository.findById(fileId)
                .orElseThrow(() -> {
                    log.error("File not found with ID: {}", fileId);
                    return new NoSuchElementException(String.format(errorFileNotFound, fileId));
                });

        log.info("File found - ID: {}, Status: {}", metadata.getFileId(), metadata.getStatus());

        // Step 3: Check if already deleted
        if ("DELETED".equalsIgnoreCase(metadata.getStatus())) {
            log.warn("File ID {} is already deleted", fileId);
            throw new IllegalStateException("File ID " + fileId + " is already deleted.");
        }

        // Step 4: Check if archived
        if ("ARCHIVED".equalsIgnoreCase(metadata.getStatus())) {
            log.warn("File ID {} is archived, cannot delete", fileId);
            throw new IllegalStateException("Transactions archived, unable to delete. File ID: " + fileId);
        }

        // Step 5: Soft delete — only update transaction_metadata status
        metadata.setStatus("DELETED");
        metadataRepository.save(metadata);

        log.info("File ID {} marked as DELETED in transaction_metadata", fileId);
        log.info("========================================");

        return "File ID " + fileId + " has been successfully deleted. " +
                "Total records affected: " + metadata.getTotalRecords();
    }
    @Override
    public List<FileLoadMetaDataResponse> getArchivedFiles() {
        log.info("Fetching all archived files");

        List<TransactionMetadata> archivedFiles = metadataRepository.findByStatus("ARCHIVED");

        log.info("Found {} archived file(s)", archivedFiles.size());
        return serviceMapper.toFileLoadMetaDataResponseList(archivedFiles);
    }
    @Override
    @Transactional
    public String unarchiveFile(Long fileId) {
        log.info("========================================");
        log.info("Unarchive request received for File ID: {}", fileId);

        // Step 1: Validate fileId
        if (fileId == null || fileId <= 0) {
            throw new IllegalArgumentException("Invalid file ID. File ID must be a positive number.");
        }

        // Step 2: Check file exists
        TransactionMetadata metadata = metadataRepository.findById(fileId)
                .orElseThrow(() -> {
                    log.error("File not found with ID: {}", fileId);
                    return new NoSuchElementException(String.format(errorFileNotFound, fileId));
                });

        // Step 3: Check it is actually archived
        if (!"ARCHIVED".equalsIgnoreCase(metadata.getStatus())) {
            log.error("File ID {} is not archived. Current status: {}", fileId, metadata.getStatus());
            throw new IllegalStateException(
                    "File ID " + fileId + " is not archived. Current status: " + metadata.getStatus());
        }

        // Step 4: Fetch archived transactions
        List<ArchivedTransaction> archivedTransactions =
                archivedTransactionRepository.findByFileLoadId(fileId);

        if (archivedTransactions.isEmpty()) {
            log.warn("No archived transactions found for File ID: {}", fileId);
            throw new NoSuchElementException("No archived transactions found for File ID: " + fileId);
        }

        log.info("Step 1: Moving {} transactions back to successful_transactions", archivedTransactions.size());

        // Step 5: Map back to SuccessfulTransaction and save
        List<SuccessfulTransaction> successfulTransactions = archivedTransactions.stream()
                .map(serviceMapper::toSuccessfulTransaction)
                .collect(java.util.stream.Collectors.toList());

        successfulTransactionRepository.saveAll(successfulTransactions);
        log.info("Step 2: {} transactions restored to successful_transactions", successfulTransactions.size());

        // Step 6: Delete from archive table
        archivedTransactionRepository.deleteByFileLoadId(fileId);
        log.info("Step 3: Deleted transactions from archived_transactions");

        // Step 7: Update metadata status back to COMPLETED
        metadata.setStatus("COMPLETED");
        metadataRepository.save(metadata);
        log.info("Step 4: File ID {} status updated back to COMPLETED", fileId);

        log.info("Unarchive completed for File ID: {}", fileId);
        log.info("========================================");

        return "Successfully unarchived " + archivedTransactions.size() +
                " transactions for File ID: " + fileId;
    }

    @Override
    public List<ErrorResponseDTO> getErrorLogs() {
        log.info("Fetching all error logs");

        List<FailedTransaction> failedTransactions =
                failedTransactionRepository.findAllExcludeDeleted();

        log.info("Found {} error log(s)", failedTransactions.size());
        return serviceMapper.toErrorResponseDTOList(failedTransactions);
    }

    private void validateFile(MultipartFile file) {
        log.debug("Validating file...");

        if (file == null) {
            log.error("Validation failed: File object is null");
            throw new EmptyFileException(errorNoFileProvided);
        }

        if (file.isEmpty() || file.getSize() == 0) {
            log.error("Validation failed: File is empty (size: {})", file.getSize());
            throw new EmptyFileException(errorFileEmpty);
        }

        String filename = file.getOriginalFilename();

        if (filename == null || filename.trim().isEmpty()) {
            log.error("Validation failed: File name is missing");
            throw new InvalidFileFormatException(errorInvalidFileName);
        }

        String fileExtension = getFileExtension(filename);
        List<String> allowedExtensionList = Arrays.asList(allowedExtensions.split(","));

        log.debug("File extension: '{}', Allowed extensions: {}", fileExtension, allowedExtensionList);

        if (!allowedExtensionList.contains(fileExtension.toLowerCase())) {
            log.error("Validation failed: Invalid file extension '{}'. Allowed: {}",
                    fileExtension, allowedExtensionList);
            throw new InvalidFileFormatException(filename, String.join(", ", allowedExtensionList));
        }

        if (file.getSize() > maxFileSize) {
            log.error("Validation failed: File size {} bytes exceeds maximum {} bytes",
                    file.getSize(), maxFileSize);
            throw new IllegalArgumentException(String.format(errorFileTooLarge, maxFileSizeMb));
        }

        log.debug("File validation successful - Name: {}, Size: {} bytes, Extension: {}",
                filename, file.getSize(), fileExtension);
    }

    private void checkDuplicateFile(String filename) {
        log.debug("Checking for duplicate file: {}", filename);

        metadataRepository.findByFilename(filename).ifPresent(existing -> {
            log.warn("Duplicate file detected - File: {}, Existing File ID: {}, Upload Time: {}",
                    filename, existing.getFileId(), existing.getUploadTime());
            throw new IllegalStateException(String.format(errorDuplicateFile, filename));
        });
    }

    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
            return filename.substring(lastDotIndex + 1);
        }
        return "";
    }

    private TransactionMetadata createMetadata(MultipartFile file) {
        TransactionMetadata metadata = new TransactionMetadata();
        metadata.setFilename(file.getOriginalFilename());
        metadata.setUploadTime(LocalDateTime.now());
        metadata.setStatus(statusProcessing);
        metadata.setTotalRecords(0);
        metadata.setSuccessCount(0);
        metadata.setErrorCount(0);

        log.debug("Created metadata: File: {}, Status: {}, Upload Time: {}",
                metadata.getFilename(), metadata.getStatus(), metadata.getUploadTime());

        return metadata;
    }

    private void validateStatus(String status) {
        try {
            FileUploadStatus.fromValue(status);
            log.debug("Status validation passed: {}", status);
        } catch (IllegalArgumentException e) {
            log.error("Invalid status value: {}", status);
            throw new IllegalArgumentException(
                    "Invalid status value. Allowed values are: " +
                            Arrays.stream(FileUploadStatus.values())
                                    .map(FileUploadStatus::getValue)
                                    .collect(java.util.stream.Collectors.joining(", ")));
        }
    }

}