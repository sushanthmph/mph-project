package com.mphasis.tfp.services;

import com.mphasis.tfp.dto.ErrorResponseDTO;
import com.mphasis.tfp.dto.FileLoadMetaDataResponse;
import com.mphasis.tfp.dto.UploadResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

public interface IFileUpload {
    UploadResponseDTO uploadFile(MultipartFile file, Long userId);

    FileLoadMetaDataResponse getFileStatus(Long fileId, Long userId);

    List<FileLoadMetaDataResponse> searchFiles(
            Long fileId,
            String fileName,
            LocalDate uploadDateFrom,
            LocalDate uploadDateTo,
            String status,
            Long userId);

    String archiveFile(Long fileId, Long userId);

    String deleteTransactions(Long fileId, Long userId);

    List<FileLoadMetaDataResponse> getArchivedFiles(Long userId);

    String unarchiveFile(Long fileId, Long userId);

    List<ErrorResponseDTO> getErrorLogs(Long userId);

    Page<ErrorResponseDTO> getErrorLogsPaginated(
            Long fileId,
            String fileName,
            int page,
            int size,
            String sortBy,
            String sortDirection,
            Long userId
    );

    Page<FileLoadMetaDataResponse> searchFilesPaginated(
            Long fileId,
            String fileName,
            LocalDate uploadDateFrom,
            LocalDate uploadDateTo,
            String status,
            Long userId,
            int page,
            int size,
            String sortBy,
            String sortDirection
    );
}