package com.mphasis.tfp.controllerImpl;

import com.mphasis.tfp.controller.ITransactionController;
import com.mphasis.tfp.dto.ApiResponseDTO;
import com.mphasis.tfp.dto.ErrorResponseDTO;
import com.mphasis.tfp.dto.FileLoadMetaDataResponse;
import com.mphasis.tfp.dto.UploadResponseDTO;
import com.mphasis.tfp.services.IFileUpload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
public class
TransactionControllerImpl implements ITransactionController {

    private final IFileUpload fileUploadService;

    @Override
    public ApiResponseDTO<UploadResponseDTO> uploadFile(MultipartFile file) {
        log.info("API: Received file upload request: {}", file != null ? file.getOriginalFilename() : "null");
        UploadResponseDTO response = fileUploadService.uploadFile(file);
        return ApiResponseDTO.success(response, "File uploaded successfully and processing started");
    }

    @Override
    public ApiResponseDTO<FileLoadMetaDataResponse> getFileStatus(Long fileId) {
        log.info("API: Received request to get file status for fileId: {}", fileId);
        FileLoadMetaDataResponse response = fileUploadService.getFileStatus(fileId);
        return ApiResponseDTO.success(response, "File status retrieved successfully");
    }

    @Override
    public ApiResponseDTO<List<FileLoadMetaDataResponse>> searchFiles(
            Long fileId, String fileName, LocalDate uploadDateFrom, LocalDate uploadDateTo, String status) {

        log.info("API: Received search request - fileId: {}, fileName: {}, from: {}, to: {}, status: {}",
                fileId, fileName, uploadDateFrom, uploadDateTo, status);

        List<FileLoadMetaDataResponse> response = fileUploadService.searchFiles(
                fileId, fileName, uploadDateFrom, uploadDateTo, status);

        String message = response.isEmpty() ? "No files found matching the criteria" : "Found " + response.size() + " file(s)";
        return ApiResponseDTO.success(response, message);
    }
    @Override
    public ApiResponseDTO<String> archiveFile(Long fileId) {
        log.info("API: Received archive request for fileId: {}", fileId);
        String result = fileUploadService.archiveFile(fileId);
        return ApiResponseDTO.success(result, "Transactions archived successfully");
    }
    @Override
    public ApiResponseDTO<String> deleteTransactions(Long fileId) {
        log.info("API: Received soft delete request for fileId: {}", fileId);
        String result = fileUploadService.deleteTransactions(fileId);
        return ApiResponseDTO.success(result, "Transactions deleted successfully");
    }
    @Override
    public ApiResponseDTO<List<FileLoadMetaDataResponse>> getArchivedFiles() {
        log.info("API: Received request to get all archived files");
        List<FileLoadMetaDataResponse> response = fileUploadService.getArchivedFiles();
        String message = response.isEmpty() ? "No archived files found" : "Found " + response.size() + " archived file(s)";
        return ApiResponseDTO.success(response, message);
    }

    @Override
    public ApiResponseDTO<String> unarchiveFile(Long fileId) {
        log.info("API: Received unarchive request for fileId: {}", fileId);
        String result = fileUploadService.unarchiveFile(fileId);
        return ApiResponseDTO.success(result, "File unarchived successfully");
    }

    @Override
    public ApiResponseDTO<List<ErrorResponseDTO>> getErrorLogs() {
        log.info("API: Received request to get all error logs");
        List<ErrorResponseDTO> response = fileUploadService.getErrorLogs();
        String message = response.isEmpty() ? "No error logs found" : "Found " + response.size() + " error log(s)";
        return ApiResponseDTO.success(response, message);
    }
}