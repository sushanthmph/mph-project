package com.mphasis.tfp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadResponseDTO {
    private Long fileId;
    private String fileName;
    private String status;
    private String message;
    private Integer totalRecords;
    private Integer successCount;
    private Integer errorCount;
}