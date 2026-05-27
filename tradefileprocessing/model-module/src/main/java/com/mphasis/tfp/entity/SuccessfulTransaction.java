package com.mphasis.tfp.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "successful_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SuccessfulTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", nullable = false)
    private String transactionId;

    @Column(name = "file_header_date")
    private String fileHeaderDate;

    @Column(name = "account_number", nullable = false)
    private String accountNumber;

    @Column(name = "transaction_type")
    private Integer transactionType;

    @Column(name = "batch_location")
    private String batchLocation;

    @Column(name = "batch_number")
    private Integer batchNumber;

    @Column(name = "update_batch_date")
    private Integer updateBatchDate;

    @Column(name = "action_name")
    private String actionName;

    @Column(name = "related_file_key")
    private Integer relatedFileKey;

    @Column(name = "do_not_report_flag")
    private String doNotReportFlag;

    @Column(name = "owning_portfolio")
    private Integer owningPortfolio;

    @Column(name = "poster_initials")
    private String posterInitials;

    @Column(name = "transaction_subtype")
    private Integer transactionSubtype;

    @Column(name = "cash_effect", precision = 15, scale = 2)
    private BigDecimal cashEffect;

    @Column(name = "old_balance", precision = 15, scale = 2)
    private BigDecimal oldBalance;

    @Column(name = "new_balance", precision = 15, scale = 2)
    private BigDecimal newBalance;

    @Column(name = "file_load_id")
    private Long fileLoadId;

    @Column(name = "created_time")
    private LocalDateTime createdTime;

    @PrePersist
    protected void onCreate() {
        createdTime = LocalDateTime.now();
    }
}