package com.mphasis.tfp.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileLoadMetaDataResponse {

    private Long fileId;
    private String fileName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime uploadTime;

    private Integer recordCount;
    private Integer successCount;
    private Integer errorCount;
    private String status;
}