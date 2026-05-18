package com.mphasis.tfp.services;

import com.mphasis.tfp.dto.ErrorResponseDTO;
import com.mphasis.tfp.dto.FileLoadMetaDataResponse;
import com.mphasis.tfp.dto.UploadResponseDTO;
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
}