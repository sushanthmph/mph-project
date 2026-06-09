package com.mphasis.tfp.repository;

import com.mphasis.tfp.entity.FailedTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @Query("SELECT f FROM FailedTransaction f " +
            "JOIN TransactionMetadata m ON f.fileLoadId = m.fileId " +
            "WHERE m.status != 'DELETED' " +
            "AND m.userId = :userId")
    List<FailedTransaction> findAllExcludeDeleted(@Param("userId") Long userId);

    @Query("SELECT f FROM FailedTransaction f " +
            "JOIN TransactionMetadata m ON f.fileLoadId = m.fileId " +
            "WHERE m.status != 'DELETED' AND " +
            "m.userId = :userId AND " +
            "(:fileId IS NULL OR f.fileLoadId = :fileId) AND " +
            "(:fileName IS NULL OR LOWER(m.filename) LIKE LOWER(CONCAT('%', :fileName, '%')))")
    Page<FailedTransaction> findErrorsWithPagination(
            @Param("fileId") Long fileId,
            @Param("fileName") String fileName,
            @Param("userId") Long userId,
            Pageable pageable
    );
}