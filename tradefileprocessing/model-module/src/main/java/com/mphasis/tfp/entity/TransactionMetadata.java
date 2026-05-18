package com.mphasis.tfp.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "transaction_metadata")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long fileId;

    @Column(name = "filename", nullable = false)
    private String filename;

    @Column(name = "upload_time")
    private LocalDateTime uploadTime;

    @Column(name = "total_records")
    private Integer totalRecords;

    @Column(name = "success_count")
    private Integer successCount;

    @Column(name = "error_count")
    private Integer errorCount;

    @Column(name = "status")
    private String status;

    @PrePersist
    protected void onCreate() {
        uploadTime = LocalDateTime.now();
    }
}