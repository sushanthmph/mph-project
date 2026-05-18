package com.mphasis.tfp.repository;

import com.mphasis.tfp.entity.FailedTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FailedTransactionRepository extends JpaRepository<FailedTransaction, Long> {

    List<FailedTransaction> findByFileLoadId(Long fileLoadId);

    List<FailedTransaction> findByTransactionId(String transactionId);

    List<FailedTransaction> findByAccountNumber(String accountNumber);

    List<FailedTransaction> findByStatus(String status);

    @Query("SELECT COUNT(f) FROM FailedTransaction f WHERE f.fileLoadId = :fileLoadId")
    Integer countByFileLoadId(@Param("fileLoadId") Long fileLoadId);

    void deleteByFileLoadId(Long fileLoadId);

    // ADD THIS
    @Query("SELECT f FROM FailedTransaction f " +
            "JOIN TransactionMetadata m ON f.fileLoadId = m.fileId " +
            "WHERE m.status != 'DELETED'")
    List<FailedTransaction> findAllExcludeDeleted();
}