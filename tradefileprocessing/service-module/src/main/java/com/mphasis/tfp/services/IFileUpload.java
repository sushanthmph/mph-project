package com.mphasis.tfp.services;

import com.mphasis.tfp.dto.ErrorResponseDTO;
import com.mphasis.tfp.dto.FileLoadMetaDataResponse;
import com.mphasis.tfp.dto.UploadResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

public interface IFileUpload {

    UploadResponseDTO uploadFile(MultipartFile file);

    FileLoadMetaDataResponse getFileStatus(Long fileId);

    List<FileLoadMetaDataResponse> searchFiles(
            Long fileId,
            String fileName,
            LocalDate uploadDateFrom,
            LocalDate uploadDateTo,
            String status);

    String archiveFile(Long fileId);

    String deleteTransactions(Long fileId);

    // ADD THESE THREE
    List<FileLoadMetaDataResponse> getArchivedFiles();

    String unarchiveFile(Long fileId);

    List<ErrorResponseDTO> getErrorLogs();
    // Add these two methods to your service interface

    // ADD THESE TO IFileUpload INTERFACE


    Page<ErrorResponseDTO> getErrorLogsPaginated(
            Long fileId,
            String fileName,
            int page,
            int size,
            String sortBy,
            String sortDirection
    );

    Page<FileLoadMetaDataResponse> searchFilesPaginated(
            Long fileId,
            String fileName,
            LocalDate uploadDateFrom,
            LocalDate uploadDateTo,
            String status,
            int page,
            int size,
            String sortBy,
            String sortDirection
    );
}