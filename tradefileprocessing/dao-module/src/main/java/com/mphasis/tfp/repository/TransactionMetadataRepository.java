package com.mphasis.tfp.repository;

import com.mphasis.tfp.entity.TransactionMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionMetadataRepository extends JpaRepository<TransactionMetadata, Long>,
        JpaSpecificationExecutor<TransactionMetadata> {

    Optional<TransactionMetadata> findByFilename(String filename);

    @Query(value = "SELECT * FROM transaction_metadata t WHERE " +
            "(:fileId IS NULL OR t.file_id = :fileId) AND " +
            "(:filename IS NULL OR LOWER(t.filename) LIKE LOWER(CONCAT('%', CAST(:filename AS TEXT), '%'))) AND " +
            "(CAST(:startDate AS TIMESTAMP) IS NULL OR t.upload_time >= CAST(:startDate AS TIMESTAMP)) AND " +
            "(CAST(:endDate AS TIMESTAMP) IS NULL OR t.upload_time <= CAST(:endDate AS TIMESTAMP)) AND " +
            "(:status IS NULL OR t.status = CAST(:status AS TEXT))",
            nativeQuery = true)
    List<TransactionMetadata> searchFiles(
            @Param("fileId") Long fileId,
            @Param("filename") String filename,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("status") String status);
    List<TransactionMetadata> findByStatus(String status);
}