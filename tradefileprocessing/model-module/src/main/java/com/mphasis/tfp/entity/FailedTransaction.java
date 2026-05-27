package com.mphasis.tfp.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "failed_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FailedTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long errorId;

    @Column(name = "file_load_id")
    private Long fileLoadId;

    @Column(name = "transaction_id")
    private String transactionId;

    @Column(name = "account_number")
    private String accountNumber;

    @Column(name = "record_count")
    private Integer recordCount;

    @Column(name = "error_field")
    private String errorField;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "status")
    private String status;

    @Column(name = "created_time")
    private LocalDateTime createdTime;

    @PrePersist
    protected void onCreate() {
        createdTime = LocalDateTime.now();
    }
}