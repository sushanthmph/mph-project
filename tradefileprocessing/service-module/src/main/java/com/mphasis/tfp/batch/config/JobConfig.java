package com.mphasis.tfp.batch.config;

import com.mphasis.tfp.entity.FailedTransaction;
import com.mphasis.tfp.entity.SuccessfulTransaction;
import com.mphasis.tfp.repository.FailedTransactionRepository;
import com.mphasis.tfp.repository.SuccessfulTransactionRepository;
import com.mphasis.tfp.validation.TransactionValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class JobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final SuccessfulTransactionRepository successfulTransactionRepository;
    private final FailedTransactionRepository failedTransactionRepository;
    private final TransactionValidator transactionValidator;

    @Value("${app.batch.chunk-size}")
    private int chunkSize;

    @Value("${app.batch.skip-limit}")
    private int skipLimit;

    @Value("${app.transaction.field.transaction-id-index}")
    private int transactionIdIndex;

    @Value("${app.transaction.field.file-header-date-index}")
    private int fileHeaderDateIndex;

    @Value("${app.transaction.field.account-number-index}")
    private int accountNumberIndex;

    @Value("${app.transaction.field.transaction-type-index}")
    private int transactionTypeIndex;

    @Value("${app.transaction.field.batch-location-index}")
    private int batchLocationIndex;

    @Value("${app.transaction.field.batch-number-index}")
    private int batchNumberIndex;

    @Value("${app.transaction.field.update-batch-date-index}")
    private int updateBatchDateIndex;

    @Value("${app.transaction.field.action-name-index}")
    private int actionNameIndex;

    @Value("${app.transaction.field.related-file-key-index}")
    private int relatedFileKeyIndex;

    @Value("${app.transaction.field.do-not-report-flag-index}")
    private int doNotReportFlagIndex;

    @Value("${app.transaction.field.owning-portfolio-index}")
    private int owningPortfolioIndex;

    @Value("${app.transaction.field.poster-initials-index}")
    private int posterInitialsIndex;

    @Value("${app.transaction.field.transaction-subtype-index}")
    private int transactionSubtypeIndex;

    @Value("${app.transaction.field.cash-effect-index}")
    private int cashEffectIndex;

    @Value("${app.transaction.field.old-balance-index}")
    private int oldBalanceIndex;

    @Value("${app.transaction.field.new-balance-index}")
    private int newBalanceIndex;

    @Value("${app.status.failed}")
    private String statusFailed;

    @Bean
    public Job fileProcessingJob(Step processTransactionsStep) {
        return new JobBuilder("fileProcessingJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(processTransactionsStep)
                .build();
    }

    @Bean
    public Step processTransactionsStep(
            ItemReader<Map<String, Object>> reader,
            ItemProcessor<Map<String, Object>, Object> processor,
            ItemWriter<Object> writer) {

        log.info("Configuring batch step with chunk size: {} and skip limit: {}", chunkSize, skipLimit);

        return new StepBuilder("processTransactionsStep", jobRepository)
                .<Map<String, Object>, Object>chunk(chunkSize, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .faultTolerant()
                .skipLimit(skipLimit)
                .skip(Exception.class)
                .build();
    }

    @Bean
    public ItemReader<Map<String, Object>> transactionReader() {
        return new ListItemReader<>(new ArrayList<>());
    }

    @Bean
    public ItemProcessor<Map<String, Object>, Object> transactionProcessor() {
        return item -> {
            Long fileLoadId = (Long) item.get("fileLoadId");
            String[] fields = (String[]) item.get("fields");
            int lineNumber = (int) item.get("lineNumber");

            List<String> errors = transactionValidator.validate(fields, lineNumber);

            if (errors.isEmpty()) {
                return createSuccessfulTransaction(fields, fileLoadId, lineNumber);
            } else {
                return createFailedTransaction(fields, fileLoadId, lineNumber, errors);
            }
        };
    }

    @Bean
    public ItemWriter<Object> transactionWriter() {
        return items -> {
            List<SuccessfulTransaction> successList = new ArrayList<>();
            List<FailedTransaction> failedList = new ArrayList<>();

            for (Object item : items) {
                if (item instanceof SuccessfulTransaction) {
                    successList.add((SuccessfulTransaction) item);
                } else if (item instanceof FailedTransaction) {
                    failedList.add((FailedTransaction) item);
                }
            }

            if (!successList.isEmpty()) {
                successfulTransactionRepository.saveAll(successList);
                log.debug("Saved {} successful transactions", successList.size());
            }

            if (!failedList.isEmpty()) {
                failedTransactionRepository.saveAll(failedList);
                log.debug("Saved {} failed transactions", failedList.size());
            }
        };
    }

    private SuccessfulTransaction createSuccessfulTransaction(String[] fields, Long fileLoadId, int lineNumber) {
        SuccessfulTransaction transaction = new SuccessfulTransaction();
        transaction.setFileLoadId(fileLoadId);
        transaction.setTransactionId(getField(fields, transactionIdIndex));
        transaction.setFileHeaderDate(getField(fields, fileHeaderDateIndex));
        transaction.setAccountNumber(getField(fields, accountNumberIndex));
        transaction.setTransactionType(parseInteger(getField(fields, transactionTypeIndex)));
        transaction.setBatchLocation(getField(fields, batchLocationIndex));
        transaction.setBatchNumber(parseInteger(getField(fields, batchNumberIndex)));
        transaction.setUpdateBatchDate(parseInteger(getField(fields, updateBatchDateIndex)));
        transaction.setActionName(getField(fields, actionNameIndex));
        transaction.setRelatedFileKey(parseInteger(getField(fields, relatedFileKeyIndex)));
        transaction.setDoNotReportFlag(getField(fields, doNotReportFlagIndex));
        transaction.setOwningPortfolio(parseInteger(getField(fields, owningPortfolioIndex)));
        transaction.setPosterInitials(getField(fields, posterInitialsIndex));
        transaction.setTransactionSubtype(parseInteger(getField(fields, transactionSubtypeIndex)));
        transaction.setCashEffect(parseBigDecimal(getField(fields, cashEffectIndex)));
        transaction.setOldBalance(parseBigDecimal(getField(fields, oldBalanceIndex)));
        transaction.setNewBalance(parseBigDecimal(getField(fields, newBalanceIndex)));
        return transaction;
    }

    private FailedTransaction createFailedTransaction(String[] fields, Long fileLoadId, int lineNumber, List<String> errors) {
        FailedTransaction failedTransaction = new FailedTransaction();
        failedTransaction.setFileLoadId(fileLoadId);
        failedTransaction.setTransactionId(getField(fields, transactionIdIndex));
        failedTransaction.setAccountNumber(getField(fields, accountNumberIndex));
        failedTransaction.setRecordCount(lineNumber);
        failedTransaction.setErrorMessage(String.join("; ", errors));
        failedTransaction.setStatus(statusFailed);
        return failedTransaction;
    }

    private String getField(String[] fields, int index) {
        try {
            return fields != null && index < fields.length ? fields[index] : null;
        } catch (Exception e) {
            return null;
        }
    }

    private Integer parseInteger(String value) {
        try {
            return value != null && !value.trim().isEmpty() ? Integer.parseInt(value.trim()) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private BigDecimal parseBigDecimal(String value) {
        try {
            return value != null && !value.trim().isEmpty() ? new BigDecimal(value.trim()) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}