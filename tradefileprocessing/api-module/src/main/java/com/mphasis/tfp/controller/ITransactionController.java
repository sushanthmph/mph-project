package com.mphasis.tfp.controller;

import com.mphasis.tfp.dto.ApiResponseDTO;
import com.mphasis.tfp.dto.ErrorResponseDTO;
import com.mphasis.tfp.dto.FileLoadMetaDataResponse;
import com.mphasis.tfp.dto.UploadResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Transaction File Processing", description = "APIs for uploading and managing transaction files")
@RequestMapping("/Files")
public interface ITransactionController {

    @Operation(summary = "Upload transaction file", description = "Upload a TXT/CSV file containing transaction data for processing")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File uploaded successfully",
                    content = @Content(schema = @Schema(implementation = UploadResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid file format or empty file"),
            @ApiResponse(responseCode = "413", description = "File size exceeds limit"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ApiResponseDTO<UploadResponseDTO> uploadFile(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request);

    @Operation(summary = "Get file processing status", description = "Retrieve the processing status and statistics of an uploaded file")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File status retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "File not found")
    })
    @GetMapping("/status/{fileId}")
    ApiResponseDTO<FileLoadMetaDataResponse> getFileStatus(
            @Parameter(description = "ID of the uploaded file", required = true)
            @PathVariable Long fileId,
            HttpServletRequest request
    );

    @Operation(summary = "Search uploaded files", description = "Search for uploaded files based on various criteria")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Search completed successfully")
    })
    @GetMapping("/search")
    ApiResponseDTO<List<FileLoadMetaDataResponse>> searchFiles(
            @RequestParam(required = false) Long fileId,
            @RequestParam(required = false) String fileName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate uploadDateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate uploadDateTo,
            @RequestParam(required = false) String status,
            HttpServletRequest request);

    @Operation(summary = "Archive transactions by file ID",
            description = "Archives all successful transactions of a file. Only allowed if all transactions in the file are successful.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transactions archived successfully"),
            @ApiResponse(responseCode = "400", description = "File has failed transactions or does not exist")
    })
    @PostMapping("/archive/{fileId}")
    ApiResponseDTO<String> archiveFile(
            @Parameter(description = "ID of the file to archive", required = true)
            @PathVariable Long fileId,
            HttpServletRequest request
    );

    @Operation(summary = "Soft delete transactions by file ID",
            description = "Marks a file and all its transactions as deleted. Records remain in the database but are hidden from all views.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transactions deleted successfully"),
            @ApiResponse(responseCode = "400", description = "File is archived or already deleted"),
            @ApiResponse(responseCode = "404", description = "File not found")
    })
    @DeleteMapping("/transactions/{fileId}")
    ApiResponseDTO<String> deleteTransactions(
            @Parameter(description = "ID of the file to soft delete", required = true)
            @PathVariable Long fileId,
            HttpServletRequest request
    );

    @Operation(summary = "Get all archived files")
    @ApiResponse(responseCode = "200", description = "Archived files retrieved successfully")
    @GetMapping("/archive")
    ApiResponseDTO<List<FileLoadMetaDataResponse>> getArchivedFiles(
            HttpServletRequest request
    );

    @Operation(summary = "Unarchive a file by file ID",
            description = "Moves file back from archive to dashboard")
    @PostMapping("/unarchive/{fileId}")
    ApiResponseDTO<String> unarchiveFile(
            @Parameter(description = "ID of the file to unarchive", required = true)
            @PathVariable Long fileId,
            HttpServletRequest request
    );

    @Operation(summary = "Get all error logs",
            description = "Returns all failed transaction records excluding deleted files")
    @GetMapping("/errors")
    ApiResponseDTO<List<ErrorResponseDTO>> getErrorLogs(
            HttpServletRequest request
    );

    @Operation(summary = "Get error logs with pagination",
            description = "Retrieve error logs with server-side pagination and filtering")
    @GetMapping("/errors/paginated")
    ApiResponseDTO<org.springframework.data.domain.Page<ErrorResponseDTO>> getErrorLogsPaginated(
            @Parameter(description = "File ID filter") @RequestParam(required = false) Long fileId,
            @Parameter(description = "File name filter") @RequestParam(required = false) String fileName,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort by") @RequestParam(defaultValue = "createdTime") String sortBy,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "DESC") String sortDirection,
            HttpServletRequest request
    );

    @Operation(summary = "Search files with pagination",
            description = "Search and filter files with server-side pagination")
    @GetMapping("/search/paginated")
    ApiResponseDTO<org.springframework.data.domain.Page<FileLoadMetaDataResponse>> searchFilesPaginated(
            @Parameter(description = "File ID") @RequestParam(required = false) Long fileId,
            @Parameter(description = "File name") @RequestParam(required = false) String fileName,
            @Parameter(description = "Upload date from") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate uploadDateFrom,
            @Parameter(description = "Upload date to") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate uploadDateTo,
            @Parameter(description = "Status") @RequestParam(required = false) String status,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort by") @RequestParam(defaultValue = "uploadTime") String sortBy,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "DESC") String sortDirection,
            HttpServletRequest request
    );
}