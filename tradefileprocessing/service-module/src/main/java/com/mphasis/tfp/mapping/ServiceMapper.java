package com.mphasis.tfp.mapping;

import com.mphasis.tfp.dto.ErrorResponseDTO;
import com.mphasis.tfp.dto.FileLoadMetaDataResponse;
import com.mphasis.tfp.dto.UploadResponseDTO;
import com.mphasis.tfp.entity.FailedTransaction;
import com.mphasis.tfp.entity.TransactionMetadata;
import org.springframework.stereotype.Component;
import com.mphasis.tfp.entity.ArchivedTransaction;
import com.mphasis.tfp.entity.SuccessfulTransaction;
import java.util.List;
import java.util.stream.Collectors;
import com.mphasis.tfp.entity.ArchivedTransaction;
import com.mphasis.tfp.entity.SuccessfulTransaction;
/**
 * Centralized mapper for all entity-to-DTO conversions
 */
@Component
public class ServiceMapper {

    // ==================== TRANSACTION METADATA MAPPINGS ====================

    public FileLoadMetaDataResponse toFileLoadMetaDataResponse(TransactionMetadata metadata) {
        if (metadata == null) {
            return null;
        }

        return new FileLoadMetaDataResponse(
                metadata.getFileId(),
                metadata.getFilename(),
                metadata.getUploadTime(),
                metadata.getTotalRecords(),
                metadata.getSuccessCount(),
                metadata.getErrorCount(),
                metadata.getStatus()
        );
    }

    public List<FileLoadMetaDataResponse> toFileLoadMetaDataResponseList(List<TransactionMetadata> metadataList) {
        if (metadataList == null) {
            return null;
        }

        return metadataList.stream()
                .map(this::toFileLoadMetaDataResponse)
                .collect(Collectors.toList());
    }

    public UploadResponseDTO toUploadResponseDTO(TransactionMetadata metadata, String message) {
        if (metadata == null) {
            return null;
        }

        return new UploadResponseDTO(
                metadata.getFileId(),
                metadata.getFilename(),
                metadata.getStatus(),
                message
        );
    }

    // ==================== FAILED TRANSACTION MAPPINGS ====================

    public ErrorResponseDTO toErrorResponseDTO(FailedTransaction failedTransaction) {
        if (failedTransaction == null) {
            return null;
        }

        ErrorResponseDTO dto = new ErrorResponseDTO();
        dto.setErrorId(failedTransaction.getErrorId());
        dto.setTransactionId(failedTransaction.getTransactionId());
        dto.setAccountNumber(failedTransaction.getAccountNumber());
        dto.setErrorField(failedTransaction.getErrorField());
        dto.setErrorMessage(failedTransaction.getErrorMessage());
        dto.setStatus(failedTransaction.getStatus());
        dto.setFileId(failedTransaction.getFileLoadId() != null ?
                failedTransaction.getFileLoadId().toString() : null);
        dto.setTimestamp(failedTransaction.getCreatedTime());

        return dto;
    }

    public List<ErrorResponseDTO> toErrorResponseDTOList(List<FailedTransaction> failedTransactions) {
        if (failedTransactions == null) {
            return null;
        }

        return failedTransactions.stream()
                .map(this::toErrorResponseDTO)
                .collect(Collectors.toList());
    }
    public ArchivedTransaction toArchivedTransaction(SuccessfulTransaction source) {
        if (source == null) {
            return null;
        }

        ArchivedTransaction archived = new ArchivedTransaction();
        archived.setTransactionId(source.getTransactionId());
        archived.setFileHeaderDate(source.getFileHeaderDate());
        archived.setAccountNumber(source.getAccountNumber());
        archived.setTransactionType(source.getTransactionType());
        archived.setBatchLocation(source.getBatchLocation());
        archived.setBatchNumber(source.getBatchNumber());
        archived.setUpdateBatchDate(source.getUpdateBatchDate());
        archived.setActionName(source.getActionName());
        archived.setRelatedFileKey(source.getRelatedFileKey());
        archived.setDoNotReportFlag(source.getDoNotReportFlag());
        archived.setOwningPortfolio(source.getOwningPortfolio());
        archived.setPosterInitials(source.getPosterInitials());
        archived.setTransactionSubtype(source.getTransactionSubtype());
        archived.setCashEffect(source.getCashEffect());
        archived.setOldBalance(source.getOldBalance());
        archived.setNewBalance(source.getNewBalance());
        archived.setFileLoadId(source.getFileLoadId());

        return archived;
    }
    public SuccessfulTransaction toSuccessfulTransaction(ArchivedTransaction source) {
        if (source == null) return null;

        SuccessfulTransaction tx = new SuccessfulTransaction();
        tx.setTransactionId(source.getTransactionId());
        tx.setFileHeaderDate(source.getFileHeaderDate());
        tx.setAccountNumber(source.getAccountNumber());
        tx.setTransactionType(source.getTransactionType());
        tx.setBatchLocation(source.getBatchLocation());
        tx.setBatchNumber(source.getBatchNumber());
        tx.setUpdateBatchDate(source.getUpdateBatchDate());
        tx.setActionName(source.getActionName());
        tx.setRelatedFileKey(source.getRelatedFileKey());
        tx.setDoNotReportFlag(source.getDoNotReportFlag());
        tx.setOwningPortfolio(source.getOwningPortfolio());
        tx.setPosterInitials(source.getPosterInitials());
        tx.setTransactionSubtype(source.getTransactionSubtype());
        tx.setCashEffect(source.getCashEffect());
        tx.setOldBalance(source.getOldBalance());
        tx.setNewBalance(source.getNewBalance());
        tx.setFileLoadId(source.getFileLoadId());
        // createdTime auto-set by @PrePersist

        return tx;
    }
}