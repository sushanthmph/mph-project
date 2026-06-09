package com.mphasis.tfp.servicesimpl;

import com.mphasis.tfp.batch.processor.JobProcessing;
import com.mphasis.tfp.dto.ErrorResponseDTO;
import com.mphasis.tfp.dto.FileLoadMetaDataResponse;
import com.mphasis.tfp.dto.UploadResponseDTO;
import com.mphasis.tfp.entity.*;
import com.mphasis.tfp.enums.FileUploadStatus;
import com.mphasis.tfp.exception.EmptyFileException;
import com.mphasis.tfp.exception.InvalidFileFormatException;
import com.mphasis.tfp.mapping.ServiceMapper;
import com.mphasis.tfp.repository.ArchivedTransactionRepository;
import com.mphasis.tfp.repository.FailedTransactionRepository;
import com.mphasis.tfp.repository.SuccessfulTransactionRepository;
import com.mphasis.tfp.repository.TransactionMetadataRepository;
import com.mphasis.tfp.repository.UserRepository;
import com.mphasis.tfp.services.IFileUpload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileUploadImpl implements IFileUpload {

    private static final String LOG_SEPARATOR = "========================================";
    private static final String STATUS_ARCHIVED = "ARCHIVED";
    private static final String STATUS_DELETED = "DELETED";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String LOG_FILE_NOT_FOUND = "File not found with ID: {}";
    private static final String ERROR_INVALID_FILE_ID = "Invalid file ID. File ID must be a positive number.";
    private static final String LOG_STEP_PREFIX = "Step ";

    private final TransactionMetadataRepository metadataRepository;
    private final JobProcessing jobProcessing;
    private final ServiceMapper serviceMapper;
    private final ArchivedTransactionRepository archivedTransactionRepository;
    private final SuccessfulTransactionRepository successfulTransactionRepository;
    private final FailedTransactionRepository failedTransactionRepository;
    private final UserRepository userRepository;

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

    private void verifyOwnership(TransactionMetadata metadata, Long userId) {
        if (metadata.getUserId() == null || !metadata.getUserId().equals(userId)) {
            log.error("Access denied — File ID {} does not belong to userId {}",
                    metadata.getFileId(), userId);
            throw new IllegalStateException(
                    "Access denied. File ID " + metadata.getFileId() +
                            " does not belong to you.");
        }
    }

    @Override
    @Transactional
    public UploadResponseDTO uploadFile(MultipartFile file, Long userId) {
        log.info(LOG_SEPARATOR);
        log.info("File upload request received");
        log.info("File name: {}", file != null ? file.getOriginalFilename() : "null");
        log.info("File size: {} bytes", file != null ? file.getSize() : 0);
        log.info("User ID: {}", userId);
        log.info(LOG_SEPARATOR);

        log.info("{}1: Validating file...", LOG_STEP_PREFIX);
        validateFile(file);
        log.info("{}1: File validation passed", LOG_STEP_PREFIX);

        if (!allowDuplicate) {
            log.info("{}2: Checking for duplicate files for userId: {}", LOG_STEP_PREFIX, userId);
            checkDuplicateFile(file.getOriginalFilename(), userId);
            log.info("{}2: No duplicate file found", LOG_STEP_PREFIX);
        } else {
            log.info("{}2: Duplicate file check skipped (allowed)", LOG_STEP_PREFIX);
        }

        log.info("{}3: Creating metadata entry...", LOG_STEP_PREFIX);
        TransactionMetadata metadata = createMetadata(file, userId);
        metadata = metadataRepository.save(metadata);
        log.info("{}3: Metadata created with File ID: {}", LOG_STEP_PREFIX, metadata.getFileId());

        log.info("{}4: Starting asynchronous file processing...", LOG_STEP_PREFIX);
        jobProcessing.processFile(file, metadata.getFileId(), userId);
        log.info("{}4: File processing initiated", LOG_STEP_PREFIX);

        log.info(LOG_SEPARATOR);
        log.info("File upload completed successfully");
        log.info("File ID: {}", metadata.getFileId());
        log.info("Status: {}", metadata.getStatus());
        log.info(LOG_SEPARATOR);

        return serviceMapper.toUploadResponseDTO(metadata,
                "File uploaded successfully and processing started");
    }

    @Override
    public FileLoadMetaDataResponse getFileStatus(Long fileId, Long userId) {
        log.info("File status request received for File ID: {} by userId: {}",
                fileId, userId);

        if (fileId == null || fileId <= 0) {
            log.error("Invalid file ID provided: {}", fileId);
            throw new IllegalArgumentException(ERROR_INVALID_FILE_ID);
        }

        TransactionMetadata metadata = metadataRepository.findById(fileId)
                .orElseThrow(() -> {
                    log.error(LOG_FILE_NOT_FOUND, fileId);
                    return new NoSuchElementException(
                            String.format(errorFileNotFound, fileId));
                });

        verifyOwnership(metadata, userId);

        log.info("File status retrieved — ID: {}, Status: {}, Total: {}, " +
                        "Success: {}, Failed: {}",
                metadata.getFileId(), metadata.getStatus(),
                metadata.getTotalRecords(), metadata.getSuccessCount(),
                metadata.getErrorCount());

        return serviceMapper.toFileLoadMetaDataResponse(metadata);
    }

    @Override
    public List<FileLoadMetaDataResponse> searchFiles(
            Long fileId,
            String fileName,
            LocalDate uploadDateFrom,
            LocalDate uploadDateTo,
            String status,
            Long userId) {

        log.info("File search request received for userId: {}", userId);
        log.info("Search criteria — File ID: {}, File Name: {}, " +
                        "Date From: {}, Date To: {}, Status: {}",
                fileId, fileName, uploadDateFrom, uploadDateTo, status);

        if (uploadDateFrom != null && uploadDateTo != null
                && uploadDateFrom.isAfter(uploadDateTo)) {
            log.error("Invalid date range: From ({}) is after To ({})",
                    uploadDateFrom, uploadDateTo);
            throw new IllegalArgumentException("Start date cannot be after end date");
        }

        if (status != null && !status.trim().isEmpty()) {
            log.debug("Validating status value: {}", status);
            validateStatus(status);
        }

        LocalDateTime startDateTime = uploadDateFrom != null ?
                uploadDateFrom.atStartOfDay() : null;

        LocalDateTime endDateTime = calculateEndDateTime(uploadDateFrom, uploadDateTo);

        log.debug("Resolved datetime range: {} to {}", startDateTime, endDateTime);

        List<TransactionMetadata> metadataList = metadataRepository.searchFiles(
                fileId, fileName, startDateTime, endDateTime, status, userId);

        log.info("Search completed — Found {} file(s) for userId: {}",
                metadataList.size(), userId);

        return serviceMapper.toFileLoadMetaDataResponseList(metadataList);
    }

    @Override
    @Transactional
    public String archiveFile(Long fileId, Long userId) {
        log.info(LOG_SEPARATOR);
        log.info("Archive request received for File ID: {} by userId: {}",
                fileId, userId);

        if (fileId == null || fileId <= 0) {
            throw new IllegalArgumentException(ERROR_INVALID_FILE_ID);
        }

        TransactionMetadata metadata = metadataRepository.findById(fileId)
                .orElseThrow(() -> {
                    log.error(LOG_FILE_NOT_FOUND, fileId);
                    return new NoSuchElementException(
                            String.format(errorFileNotFound, fileId));
                });

        verifyOwnership(metadata, userId);

        log.info("File found — ID: {}, Total: {}, Success: {}, Error: {}, Status: {}",
                metadata.getFileId(), metadata.getTotalRecords(),
                metadata.getSuccessCount(), metadata.getErrorCount(),
                metadata.getStatus());

        if (STATUS_ARCHIVED.equalsIgnoreCase(metadata.getStatus())) {
            log.warn("File ID {} is already archived", fileId);
            throw new IllegalStateException(
                    "File ID " + fileId + " has already been archived.");
        }

        if (STATUS_DELETED.equalsIgnoreCase(metadata.getStatus())) {
            log.warn("File ID {} is deleted, cannot archive", fileId);
            throw new IllegalStateException(
                    "File ID " + fileId + " is deleted and cannot be archived.");
        }

        if (metadata.getErrorCount() > 0 ||
                !metadata.getSuccessCount().equals(metadata.getTotalRecords())) {
            log.error("File ID {} cannot be archived — Success: {}, Total: {}, Errors: {}",
                    fileId, metadata.getSuccessCount(),
                    metadata.getTotalRecords(), metadata.getErrorCount());
            throw new IllegalStateException(
                    "File ID " + fileId + " cannot be archived. " +
                            "All transactions must be successful. " +
                            "Total: " + metadata.getTotalRecords() +
                            ", Successful: " + metadata.getSuccessCount() +
                            ", Failed: " + metadata.getErrorCount());
        }

        List<SuccessfulTransaction> successfulTransactions =
                successfulTransactionRepository.findByFileLoadId(fileId);

        if (successfulTransactions.isEmpty()) {
            log.error("No successful transactions found for File ID: {}", fileId);
            throw new NoSuchElementException(
                    "No successful transactions found for File ID: " + fileId);
        }

        log.info("{}1: Copying {} transactions to archive...",
                LOG_STEP_PREFIX, successfulTransactions.size());

        List<ArchivedTransaction> archivedTransactions = successfulTransactions.stream()
                .map(serviceMapper::toArchivedTransaction)
                .toList();

        archivedTransactionRepository.saveAll(archivedTransactions);
        log.info("{}2: {} transactions saved to archive",
                LOG_STEP_PREFIX, archivedTransactions.size());

        successfulTransactionRepository.deleteByFileLoadId(fileId);
        log.info("{}3: Deleted {} transactions from successful_transactions",
                LOG_STEP_PREFIX, successfulTransactions.size());

        metadata.setStatus(STATUS_ARCHIVED);
        metadataRepository.save(metadata);
        log.info("{}4: File ID {} status updated to ARCHIVED", LOG_STEP_PREFIX, fileId);

        log.info("Archive completed successfully for File ID: {}", fileId);
        log.info(LOG_SEPARATOR);

        return "Successfully archived " + archivedTransactions.size() +
                " transactions for File ID: " + fileId;
    }

    @Override
    @Transactional
    public String deleteTransactions(Long fileId, Long userId) {
        log.info(LOG_SEPARATOR);
        log.info("Soft delete request received for File ID: {} by userId: {}",
                fileId, userId);

        if (fileId == null || fileId <= 0) {
            throw new IllegalArgumentException(ERROR_INVALID_FILE_ID);
        }

        TransactionMetadata metadata = metadataRepository.findById(fileId)
                .orElseThrow(() -> {
                    log.error(LOG_FILE_NOT_FOUND, fileId);
                    return new NoSuchElementException(
                            String.format(errorFileNotFound, fileId));
                });

        verifyOwnership(metadata, userId);

        log.info("File found — ID: {}, Status: {}",
                metadata.getFileId(), metadata.getStatus());

        if (STATUS_DELETED.equalsIgnoreCase(metadata.getStatus())) {
            log.warn("File ID {} is already deleted", fileId);
            throw new IllegalStateException(
                    "File ID " + fileId + " is already deleted.");
        }

        if (STATUS_ARCHIVED.equalsIgnoreCase(metadata.getStatus())) {
            log.warn("File ID {} is archived, cannot delete", fileId);
            throw new IllegalStateException(
                    "Transactions archived, unable to delete. File ID: " + fileId);
        }

        metadata.setStatus(STATUS_DELETED);
        metadataRepository.save(metadata);

        log.info("File ID {} marked as DELETED in transaction_metadata", fileId);
        log.info(LOG_SEPARATOR);

        return "File ID " + fileId + " has been successfully deleted. " +
                "Total records affected: " + metadata.getTotalRecords();
    }

    @Override
    public List<FileLoadMetaDataResponse> getArchivedFiles(Long userId) {
        log.info("Fetching all archived files for userId: {}", userId);

        List<TransactionMetadata> archivedFiles =
                metadataRepository.findByStatusAndUserId(STATUS_ARCHIVED, userId);

        log.info("Found {} archived file(s) for userId: {}",
                archivedFiles.size(), userId);

        return serviceMapper.toFileLoadMetaDataResponseList(archivedFiles);
    }

    @Override
    @Transactional
    public String unarchiveFile(Long fileId, Long userId) {
        log.info(LOG_SEPARATOR);
        log.info("Unarchive request received for File ID: {} by userId: {}",
                fileId, userId);

        if (fileId == null || fileId <= 0) {
            throw new IllegalArgumentException(ERROR_INVALID_FILE_ID);
        }

        TransactionMetadata metadata = metadataRepository.findById(fileId)
                .orElseThrow(() -> {
                    log.error(LOG_FILE_NOT_FOUND, fileId);
                    return new NoSuchElementException(
                            String.format(errorFileNotFound, fileId));
                });

        verifyOwnership(metadata, userId);

        if (!STATUS_ARCHIVED.equalsIgnoreCase(metadata.getStatus())) {
            log.error("File ID {} is not archived. Current status: {}",
                    fileId, metadata.getStatus());
            throw new IllegalStateException(
                    "File ID " + fileId + " is not archived. " +
                            "Current status: " + metadata.getStatus());
        }

        List<ArchivedTransaction> archivedTransactions =
                archivedTransactionRepository.findByFileLoadId(fileId);

        if (archivedTransactions.isEmpty()) {
            log.warn("No archived transactions found for File ID: {}", fileId);
            throw new NoSuchElementException(
                    "No archived transactions found for File ID: " + fileId);
        }

        log.info("{}1: Moving {} transactions back to successful_transactions",
                LOG_STEP_PREFIX, archivedTransactions.size());

        List<SuccessfulTransaction> successfulTransactions = archivedTransactions.stream()
                .map(serviceMapper::toSuccessfulTransaction)
                .toList();

        successfulTransactionRepository.saveAll(successfulTransactions);
        log.info("{}2: {} transactions restored to successful_transactions",
                LOG_STEP_PREFIX, successfulTransactions.size());

        archivedTransactionRepository.deleteByFileLoadId(fileId);
        log.info("{}3: Deleted transactions from archived_transactions", LOG_STEP_PREFIX);

        metadata.setStatus(STATUS_COMPLETED);
        metadataRepository.save(metadata);
        log.info("{}4: File ID {} status updated back to COMPLETED", LOG_STEP_PREFIX, fileId);

        log.info("Unarchive completed for File ID: {}", fileId);
        log.info(LOG_SEPARATOR);

        return "Successfully unarchived " + archivedTransactions.size() +
                " transactions for File ID: " + fileId;
    }

    @Override
    public List<ErrorResponseDTO> getErrorLogs(Long userId) {
        log.info("Fetching all error logs for userId: {}", userId);

        List<com.mphasis.tfp.entity.FailedTransaction> failedTransactions =
                failedTransactionRepository.findAllExcludeDeleted(userId);

        log.info("Found {} error log(s) for userId: {}",
                failedTransactions.size(), userId);

        return serviceMapper.toErrorResponseDTOList(failedTransactions);
    }

    @Override
    public Page<FileLoadMetaDataResponse> searchFilesPaginated(
            Long fileId,
            String fileName,
            LocalDate uploadDateFrom,
            LocalDate uploadDateTo,
            String status,
            Long userId,
            int page,
            int size,
            String sortBy,
            String sortDir) {

        log.info("Paginated search request received for userId: {}", userId);
        log.info("Criteria — fileId: {}, fileName: {}, from: {}, to: {}, status: {}",
                fileId, fileName, uploadDateFrom, uploadDateTo, status);
        log.info("Pagination — page: {}, size: {}, sortBy: {}, sortDir: {}",
                page, size, sortBy, sortDir);

        if (uploadDateFrom != null && uploadDateTo != null
                && uploadDateFrom.isAfter(uploadDateTo)) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }

        if (status != null && !status.trim().isEmpty()) {
            validateStatus(status);
        }

        LocalDateTime startDateTime = uploadDateFrom != null ?
                uploadDateFrom.atStartOfDay() : null;

        LocalDateTime endDateTime = calculateEndDateTime(uploadDateFrom, uploadDateTo);

        Sort sort = sortDir.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() :
                Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<TransactionMetadata> metadataPage = metadataRepository.searchFilesPaginated(
                fileId, fileName, startDateTime, endDateTime, status, userId, pageable);

        log.info("Paginated search completed — Total elements: {}, Total pages: {}, UserId: {}",
                metadataPage.getTotalElements(), metadataPage.getTotalPages(), userId);

        return metadataPage.map(serviceMapper::toFileLoadMetaDataResponse);
    }

    @Override
    public Page<ErrorResponseDTO> getErrorLogsPaginated(
            Long fileId,
            String fileName,
            int page,
            int size,
            String sortBy,
            String sortDirection,
            Long userId) {

        log.info("Paginated error logs - Page: {}, Size: {}, FileId: {}, FileName: {}, UserId: {}",
                page, size, fileId, fileName, userId);

        size = Math.min(size, 100);

        Sort.Direction direction = "ASC".equalsIgnoreCase(sortDirection)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, sortBy);

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<FailedTransaction> failedPage = failedTransactionRepository.findErrorsWithPagination(
                fileId, fileName, userId, pageable);

        List<ErrorResponseDTO> content = serviceMapper.toErrorResponseDTOList(failedPage.getContent());

        log.info("Found {} error logs (Page {}/{}) for userId: {}",
                content.size(), page + 1, failedPage.getTotalPages(), userId);

        return new PageImpl<>(content, pageable, failedPage.getTotalElements());
    }


    private LocalDateTime calculateEndDateTime(LocalDate uploadDateFrom, LocalDate uploadDateTo) {
        if (uploadDateTo != null) {
            return uploadDateTo.atTime(LocalTime.MAX);
        }
        if (uploadDateFrom != null) {
            return LocalDate.now().atTime(LocalTime.MAX);
        }
        return null;
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
        List<String> allowedExtensionList =
                Arrays.asList(allowedExtensions.split(","));

        log.debug("File extension: '{}', Allowed extensions: {}",
                fileExtension, allowedExtensionList);

        if (!allowedExtensionList.contains(fileExtension.toLowerCase())) {
            log.error("Validation failed: Invalid file extension '{}'. Allowed: {}",
                    fileExtension, allowedExtensionList);
            throw new InvalidFileFormatException(
                    filename, String.join(", ", allowedExtensionList));
        }

        if (file.getSize() > maxFileSize) {
            log.error("Validation failed: File size {} bytes exceeds maximum {} bytes",
                    file.getSize(), maxFileSize);
            throw new IllegalArgumentException(
                    String.format(errorFileTooLarge, maxFileSizeMb));
        }

        log.debug("File validation successful — Name: {}, Size: {} bytes, Extension: {}",
                filename, file.getSize(), fileExtension);
    }

    private void checkDuplicateFile(String filename, Long userId) {
        log.debug("Checking for duplicate file: {} for userId: {}", filename, userId);

        metadataRepository.findByFilenameAndUserId(filename, userId).ifPresent(existing -> {
            log.warn("Duplicate file detected — File: {}, Existing File ID: {}, " +
                            "Upload Time: {}, UserId: {}",
                    filename, existing.getFileId(),
                    existing.getUploadTime(), userId);
            throw new IllegalStateException(
                    String.format(errorDuplicateFile, filename));
        });
    }

    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
            return filename.substring(lastDotIndex + 1);
        }
        return "";
    }

    private TransactionMetadata createMetadata(MultipartFile file, Long userId) {
        TransactionMetadata metadata = new TransactionMetadata();
        metadata.setFilename(file.getOriginalFilename());
        metadata.setUploadTime(LocalDateTime.now());
        metadata.setStatus(statusProcessing);
        metadata.setTotalRecords(0);
        metadata.setSuccessCount(0);
        metadata.setErrorCount(0);
        metadata.setUserId(userId);

        log.debug("Created metadata: File: {}, Status: {}, Upload Time: {}, UserId: {}",
                metadata.getFilename(), metadata.getStatus(),
                metadata.getUploadTime(), metadata.getUserId());

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
                                    .collect(Collectors.joining(", ")));
        }
    }
}