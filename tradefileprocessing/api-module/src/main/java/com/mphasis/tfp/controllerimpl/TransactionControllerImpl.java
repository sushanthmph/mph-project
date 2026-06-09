package com.mphasis.tfp.controllerimpl;

import com.mphasis.tfp.controller.ITransactionController;
import com.mphasis.tfp.dto.ApiResponseDTO;
import com.mphasis.tfp.dto.ErrorResponseDTO;
import com.mphasis.tfp.dto.FileLoadMetaDataResponse;
import com.mphasis.tfp.dto.UploadResponseDTO;
import com.mphasis.tfp.repository.UserRepository;
import com.mphasis.tfp.services.IFileUpload;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequiredArgsConstructor
@Slf4j
public class TransactionControllerImpl implements ITransactionController {

    private final IFileUpload fileUploadService;
    private final UserRepository userRepository;
    private static final String MSG_FOUND = "Found ";
    @Override
    public ApiResponseDTO<UploadResponseDTO> uploadFile(
            MultipartFile file, HttpServletRequest request) {
        Long userId = resolveUserId(request);
        UploadResponseDTO response = fileUploadService.uploadFile(file, userId);
        return ApiResponseDTO.success(response, "File uploaded successfully");
    }

    @Override
    public ApiResponseDTO<FileLoadMetaDataResponse> getFileStatus(Long fileId, HttpServletRequest request) {
        log.info("API: Received request to get file status for fileId: {}", fileId);
        Long userId = resolveUserId(request);
        FileLoadMetaDataResponse response = fileUploadService.getFileStatus(fileId, userId);
        return ApiResponseDTO.success(response, "File status retrieved successfully");
    }

    @Override
    public ApiResponseDTO<List<FileLoadMetaDataResponse>> searchFiles(
            Long fileId, String fileName, LocalDate uploadDateFrom,
            LocalDate uploadDateTo, String status, HttpServletRequest request) {
        Long userId = resolveUserId(request);
        List<FileLoadMetaDataResponse> response = fileUploadService.searchFiles(
                fileId, fileName, uploadDateFrom, uploadDateTo, status, userId);
        return ApiResponseDTO.success(response, MSG_FOUND + response.size() + " file(s)");
    }

    @Override
    public ApiResponseDTO<String> archiveFile(Long fileId, HttpServletRequest request) {
        log.info("API: Received archive request for fileId: {}", fileId);
        Long userId = resolveUserId(request);
        String result = fileUploadService.archiveFile(fileId, userId);
        return ApiResponseDTO.success(result, "Transactions archived successfully");
    }

    @Override
    public ApiResponseDTO<String> deleteTransactions(Long fileId, HttpServletRequest request) {
        log.info("API: Received soft delete request for fileId: {}", fileId);
        Long userId = resolveUserId(request);
        String result = fileUploadService.deleteTransactions(fileId, userId);
        return ApiResponseDTO.success(result, "Transactions deleted successfully");
    }

    @Override
    public ApiResponseDTO<List<FileLoadMetaDataResponse>> getArchivedFiles(HttpServletRequest request) {
        log.info("API: Received request to get all archived files");
        Long userId = resolveUserId(request);
        List<FileLoadMetaDataResponse> response = fileUploadService.getArchivedFiles(userId);
        String message = response.isEmpty() ? "No archived files found" : MSG_FOUND + response.size() + " archived file(s)";
        return ApiResponseDTO.success(response, message);
    }

    @Override
    public ApiResponseDTO<String> unarchiveFile(Long fileId, HttpServletRequest request) {
        log.info("API: Received unarchive request for fileId: {}", fileId);
        Long userId = resolveUserId(request);
        String result = fileUploadService.unarchiveFile(fileId, userId);
        return ApiResponseDTO.success(result, "File unarchived successfully");
    }

    @Override
    public ApiResponseDTO<List<ErrorResponseDTO>> getErrorLogs(HttpServletRequest request) {
        log.info("API: Received request to get all error logs");
        Long userId = resolveUserId(request);
        List<ErrorResponseDTO> response = fileUploadService.getErrorLogs(userId);
        String message = response.isEmpty() ? "No error logs found" : MSG_FOUND + response.size() + " error log(s)";
        return ApiResponseDTO.success(response, message);
    }

    @Override
    public ApiResponseDTO<org.springframework.data.domain.Page<ErrorResponseDTO>> getErrorLogsPaginated(
            Long fileId,
            String fileName,
            int page,
            int size,
            String sortBy,
            String sortDirection,
            HttpServletRequest request) {

        log.info("API: Paginated error logs - Page: {}, Size: {}", page, size);
        Long userId = resolveUserId(request);

        org.springframework.data.domain.Page<ErrorResponseDTO> response =
                fileUploadService.getErrorLogsPaginated(fileId, fileName, page, size, sortBy, sortDirection, userId);

        String message = String.format("Retrieved page %d of %d (%d total errors)",
                page + 1, response.getTotalPages(), response.getTotalElements());

        return ApiResponseDTO.success(response, message);
    }

    @Override
    public ApiResponseDTO<org.springframework.data.domain.Page<FileLoadMetaDataResponse>> searchFilesPaginated(
            Long fileId,
            String fileName,
            LocalDate uploadDateFrom,
            LocalDate uploadDateTo,
            String status,
            int page,
            int size,
            String sortBy,
            String sortDirection,
            HttpServletRequest request) {

        log.info("API: Paginated search - Page: {}, Size: {}", page, size);
        Long userId = resolveUserId(request);

        org.springframework.data.domain.Page<FileLoadMetaDataResponse> response =
                fileUploadService.searchFilesPaginated(
                        fileId, fileName, uploadDateFrom, uploadDateTo, status, userId, page, size, sortBy, sortDirection);

        String message = String.format("Retrieved page %d of %d (%d total files)",
                page + 1, response.getTotalPages(), response.getTotalElements());

        return ApiResponseDTO.success(response, message);
    }


    private Long resolveUserId(HttpServletRequest request) {
        String username = (String) request.getAttribute("loggedInUser");
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("User not found"))
                .getId();
    }
}