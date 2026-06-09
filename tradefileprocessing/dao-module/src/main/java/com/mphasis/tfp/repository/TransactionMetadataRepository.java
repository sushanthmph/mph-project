package com.mphasis.tfp.repository;

import com.mphasis.tfp.entity.TransactionMetadata;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionMetadataRepository extends JpaRepository<TransactionMetadata, Long> {

    Optional<TransactionMetadata> findByFilenameAndUserId(String filename, Long userId);

    @Query(value = "SELECT * FROM transaction_metadata t WHERE " +
            "(CAST(:fileId AS BIGINT) IS NULL OR t.file_id = CAST(:fileId AS BIGINT)) AND " +
            "(CAST(:filename AS TEXT) IS NULL OR LOWER(t.filename) LIKE LOWER(CONCAT('%', CAST(:filename AS TEXT), '%'))) AND " +
            "(CAST(:startDate AS TIMESTAMP) IS NULL OR t.upload_time >= CAST(:startDate AS TIMESTAMP)) AND " +
            "(CAST(:endDate AS TIMESTAMP) IS NULL OR t.upload_time <= CAST(:endDate AS TIMESTAMP)) AND " +
            "(CAST(:status AS TEXT) IS NULL OR t.status = CAST(:status AS TEXT)) AND " +
            "t.user_id = :userId AND " +
            "t.status != 'DELETED'",
            nativeQuery = true)
    List<TransactionMetadata> searchFiles(
            @Param("fileId") Long fileId,
            @Param("filename") String filename,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("status") String status,
            @Param("userId") Long userId);

    List<TransactionMetadata> findByStatus(String status);

    List<TransactionMetadata> findByStatusAndUserId(String status, Long userId);

    @Query(value = "SELECT * FROM transaction_metadata t WHERE " +
            "(CAST(:fileId AS BIGINT) IS NULL OR t.file_id = CAST(:fileId AS BIGINT)) AND " +
            "(CAST(:filename AS TEXT) IS NULL OR LOWER(t.filename) LIKE LOWER(CONCAT('%', CAST(:filename AS TEXT), '%'))) AND " +
            "(CAST(:startDate AS TIMESTAMP) IS NULL OR t.upload_time >= CAST(:startDate AS TIMESTAMP)) AND " +
            "(CAST(:endDate AS TIMESTAMP) IS NULL OR t.upload_time <= CAST(:endDate AS TIMESTAMP)) AND " +
            "(CAST(:status AS TEXT) IS NULL OR t.status = CAST(:status AS TEXT)) AND " +
            "t.user_id = :userId AND " +
            "t.status != 'DELETED'",
            countQuery = "SELECT COUNT(*) FROM transaction_metadata t WHERE " +
                    "(CAST(:fileId AS BIGINT) IS NULL OR t.file_id = CAST(:fileId AS BIGINT)) AND " +
                    "(CAST(:filename AS TEXT) IS NULL OR LOWER(t.filename) LIKE LOWER(CONCAT('%', CAST(:filename AS TEXT), '%'))) AND " +
                    "(CAST(:startDate AS TIMESTAMP) IS NULL OR t.upload_time >= CAST(:startDate AS TIMESTAMP)) AND " +
                    "(CAST(:endDate AS TIMESTAMP) IS NULL OR t.upload_time <= CAST(:endDate AS TIMESTAMP)) AND " +
                    "(CAST(:status AS TEXT) IS NULL OR t.status = CAST(:status AS TEXT)) AND " +
                    "t.user_id = :userId AND " +
                    "t.status != 'DELETED'",
            nativeQuery = true)
    Page<TransactionMetadata> searchFilesPaginated(
            @Param("fileId") Long fileId,
            @Param("filename") String filename,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("status") String status,
            @Param("userId") Long userId,
            Pageable pageable);
}