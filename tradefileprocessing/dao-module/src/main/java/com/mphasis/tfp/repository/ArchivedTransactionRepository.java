package com.mphasis.tfp.repository;

import com.mphasis.tfp.entity.ArchivedTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArchivedTransactionRepository extends JpaRepository<ArchivedTransaction, Long> {

    List<ArchivedTransaction> findByFileLoadId(Long fileLoadId);

    List<ArchivedTransaction> findByTransactionId(String transactionId);

    List<ArchivedTransaction> findByAccountNumber(String accountNumber);

    @Query("SELECT COUNT(a) FROM ArchivedTransaction a WHERE a.fileLoadId = :fileLoadId")
    Integer countByFileLoadId(@Param("fileLoadId") Long fileLoadId);

    // ADD THIS
    void deleteByFileLoadId(Long fileLoadId);
}