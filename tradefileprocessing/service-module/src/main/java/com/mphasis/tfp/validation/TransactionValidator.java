package com.mphasis.tfp.validation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
public class TransactionValidator {

    // ── Field Indices (all configurable from application.properties) ──
    @Value("${app.transaction.field.transaction-id-index:0}")
    private int transactionIdIndex;

    @Value("${app.transaction.field.file-header-date-index:1}")
    private int fileHeaderDateIndex;

    @Value("${app.transaction.field.account-number-index:2}")
    private int accountNumberIndex;

    @Value("${app.transaction.field.transaction-type-index:3}")
    private int transactionTypeIndex;

    @Value("${app.transaction.field.batch-location-index:4}")
    private int batchLocationIndex;

    @Value("${app.transaction.field.batch-number-index:5}")
    private int batchNumberIndex;

    @Value("${app.transaction.field.update-batch-date-index:6}")
    private int updateBatchDateIndex;

    @Value("${app.transaction.field.action-name-index:7}")
    private int actionNameIndex;

    @Value("${app.transaction.field.related-file-key-index:8}")
    private int relatedFileKeyIndex;

    @Value("${app.transaction.field.do-not-report-flag-index:9}")
    private int doNotReportFlagIndex;

    @Value("${app.transaction.field.owning-portfolio-index:10}")
    private int owningPortfolioIndex;

    @Value("${app.transaction.field.poster-initials-index:11}")
    private int posterInitialsIndex;

    @Value("${app.transaction.field.transaction-subtype-index:12}")
    private int transactionSubtypeIndex;

    @Value("${app.transaction.field.cash-effect-index:13}")
    private int cashEffectIndex;

    @Value("${app.transaction.field.old-balance-index:14}")
    private int oldBalanceIndex;

    @Value("${app.transaction.field.new-balance-index:15}")
    private int newBalanceIndex;

    // ── Validation Configuration ───────────────────────────────────────
    @Value("${app.validation.account-number-min-length:5}")
    private int accountNumberMinLength;

    @Value("${app.validation.account-number-max-length:20}")
    private int accountNumberMaxLength;

    @Value("${app.validation.account-number-pattern:^[A-Za-z0-9]{5,20}$}")
    private String accountNumberPattern;

    // ── Constants ──────────────────────────────────────────────────────
    private static final List<String> VALID_ACTIONS =
            Arrays.asList("CREATE", "UPDATE", "DELETE", "POSTED");

    private static final List<String> VALID_FLAGS =
            Arrays.asList("Y", "N");

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("uuuuMMdd").withResolverStyle(ResolverStyle.STRICT);

    private static final int EXPECTED_FIELD_COUNT = 16;

    /**
     * Validates all fields in a transaction record
     * @param fields Array of field values from CSV
     * @param lineNumber Line number for error reporting
     * @return List of validation error messages (empty if valid)
     */
    public List<String> validate(String[] fields, int lineNumber) {
        List<String> errors = new ArrayList<>();

        log.debug("Line {}: Starting validation for {} fields",
                lineNumber, fields != null ? fields.length : 0);

        // ── Field count check ──────────────────────────────────────────
        if (fields == null || fields.length < EXPECTED_FIELD_COUNT) {
            String error = String.format(
                    "Insufficient fields. Expected %d fields but found %d",
                    EXPECTED_FIELD_COUNT, fields != null ? fields.length : 0);
            errors.add(error);
            log.error("Line {}: {}", lineNumber, error);
            return errors;
        }

        // ── Perform all validations ────────────────────────────────────
        validateTransactionId(fields, lineNumber, errors);
        validateFileHeaderDate(fields, lineNumber, errors);
        validateAccountNumber(fields, lineNumber, errors);
        validateTransactionType(fields, lineNumber, errors);
        validateBatchNumber(fields, lineNumber, errors);
        validateActionName(fields, lineNumber, errors);
        validateDoNotReportFlag(fields, lineNumber, errors);
        validateOwningPortfolio(fields, lineNumber, errors);
        validateTransactionSubtype(fields, lineNumber, errors);
        validateCashEffect(fields, lineNumber, errors);
        validateOldBalance(fields, lineNumber, errors);
        validateNewBalance(fields, lineNumber, errors);
        validateBalanceEquation(fields, lineNumber, errors);

        if (errors.isEmpty()) {
            log.debug("Line {}: All validations passed successfully", lineNumber);
        } else {
            log.warn("Line {}: Failed with {} validation error(s)", lineNumber, errors.size());
        }

        return errors;
    }

    // ══════════════════════════════════════════════════════════════════
    // VALIDATION METHODS
    // ══════════════════════════════════════════════════════════════════

    private void validateTransactionId(String[] fields, int lineNumber, List<String> errors) {
        String transactionId = getField(fields, transactionIdIndex);
        if (!isValid(fields, transactionIdIndex)) {
            String error = "Transaction ID is required";
            errors.add(error);
            log.error("Line {}: {} - Value: '{}'", lineNumber, error, transactionId);
        } else if (transactionId.trim().isEmpty()) {
            String error = "Transaction ID cannot be empty";
            errors.add(error);
            log.error("Line {}: {}", lineNumber, error);
        }
    }

    private void validateFileHeaderDate(String[] fields, int lineNumber, List<String> errors) {
        String fileHeaderDate = getField(fields, fileHeaderDateIndex);
        if (!isValid(fields, fileHeaderDateIndex)) {
            String error = "File Header Date is required";
            errors.add(error);
            log.error("Line {}: {}", lineNumber, error);
        } else if (!isValidDate(fileHeaderDate)) {
            String error = String.format(
                    "File Header Date must be a valid date in yyyyMMdd format. Got: '%s'",
                    fileHeaderDate);
            errors.add(error);
            log.error("Line {}: {}", lineNumber, error);
        }
    }

    private void validateAccountNumber(String[] fields, int lineNumber, List<String> errors) {
        String accountNumber = getField(fields, accountNumberIndex);
        if (!isValid(fields, accountNumberIndex)) {
            String error = "Account Number is required";
            errors.add(error);
            log.error("Line {}: {} - Value: '{}'", lineNumber, error, accountNumber);
        } else if (!isValidAccountNumber(accountNumber)) {
            String error = String.format(
                    "Invalid Account Number format. Must be alphanumeric (%d-%d chars). Got: '%s'",
                    accountNumberMinLength, accountNumberMaxLength, accountNumber);
            errors.add(error);
            log.error("Line {}: {}", lineNumber, error);
        }
    }

    private void validateTransactionType(String[] fields, int lineNumber, List<String> errors) {
        String transactionType = getField(fields, transactionTypeIndex);
        // Transaction Type is optional, but if present must be a valid integer
        if (isValid(fields, transactionTypeIndex) && !isValidInteger(transactionType)) {
            String error = String.format(
                    "Transaction Type must be a valid integer. Got: '%s'", transactionType);
            errors.add(error);
            log.error("Line {}: {}", lineNumber, error);
        }
    }

    private void validateBatchNumber(String[] fields, int lineNumber, List<String> errors) {
        String batchNumber = getField(fields, batchNumberIndex);
        // Batch Number is optional, but if present must be a valid integer
        if (isValid(fields, batchNumberIndex) && !isValidInteger(batchNumber)) {
            String error = String.format(
                    "Batch Number must be a valid integer. Got: '%s'", batchNumber);
            errors.add(error);
            log.error("Line {}: {}", lineNumber, error);
        }
    }

    private void validateActionName(String[] fields, int lineNumber, List<String> errors) {
        String actionName = getField(fields, actionNameIndex);
        if (!isValid(fields, actionNameIndex)) {
            String error = "Action Name is required";
            errors.add(error);
            log.error("Line {}: {}", lineNumber, error);
        } else if (!VALID_ACTIONS.contains(actionName.toUpperCase().trim())) {
            String error = String.format(
                    "Action Name must be one of %s. Got: '%s'", VALID_ACTIONS, actionName);
            errors.add(error);
            log.error("Line {}: {}", lineNumber, error);
        }
    }

    private void validateDoNotReportFlag(String[] fields, int lineNumber, List<String> errors) {
        String doNotReportFlag = getField(fields, doNotReportFlagIndex);
        if (!isValid(fields, doNotReportFlagIndex)) {
            String error = "Do Not Report Flag is required";
            errors.add(error);
            log.error("Line {}: {}", lineNumber, error);
        } else if (!VALID_FLAGS.contains(doNotReportFlag.toUpperCase().trim())) {
            String error = String.format(
                    "Do Not Report Flag must be Y or N. Got: '%s'", doNotReportFlag);
            errors.add(error);
            log.error("Line {}: {}", lineNumber, error);
        }
    }

    private void validateOwningPortfolio(String[] fields, int lineNumber, List<String> errors) {
        String owningPortfolio = getField(fields, owningPortfolioIndex);
        if (!isValid(fields, owningPortfolioIndex)) {
            String error = "Owning Portfolio is required";
            errors.add(error);
            log.error("Line {}: {}", lineNumber, error);
        }
    }

    private void validateTransactionSubtype(String[] fields, int lineNumber, List<String> errors) {
        String transactionSubtype = getField(fields, transactionSubtypeIndex);
        // Transaction Subtype is optional, but if present must be a valid integer
        if (isValid(fields, transactionSubtypeIndex) && !isValidInteger(transactionSubtype)) {
            String error = String.format(
                    "Transaction Subtype must be a valid integer. Got: '%s'", transactionSubtype);
            errors.add(error);
            log.error("Line {}: {}", lineNumber, error);
        }
    }

    private void validateCashEffect(String[] fields, int lineNumber, List<String> errors) {
        String cashEffect = getField(fields, cashEffectIndex);
        if (!isValid(fields, cashEffectIndex)) {
            String error = "Cash Effect is required";
            errors.add(error);
            log.error("Line {}: {}", lineNumber, error);
        } else if (!isValidDecimal(cashEffect)) {
            String error = String.format(
                    "Cash Effect must be a valid decimal. Got: '%s'", cashEffect);
            errors.add(error);
            log.error("Line {}: {}", lineNumber, error);
        } else if (!isPositive(cashEffect)) {
            String error = String.format(
                    "Cash Effect must be a positive value. Got: '%s'", cashEffect);
            errors.add(error);
            log.error("Line {}: {}", lineNumber, error);
        }
    }

    private void validateOldBalance(String[] fields, int lineNumber, List<String> errors) {
        String oldBalance = getField(fields, oldBalanceIndex);
        if (!isValid(fields, oldBalanceIndex)) {
            String error = "Old Balance is required";
            errors.add(error);
            log.error("Line {}: {}", lineNumber, error);
        } else if (!isValidDecimal(oldBalance)) {
            String error = String.format(
                    "Old Balance must be a valid decimal. Got: '%s'", oldBalance);
            errors.add(error);
            log.error("Line {}: {}", lineNumber, error);
        } else if (!isPositive(oldBalance)) {
            String error = String.format(
                    "Old Balance must be a positive value. Got: '%s'", oldBalance);
            errors.add(error);
            log.error("Line {}: {}", lineNumber, error);
        }
    }

    private void validateNewBalance(String[] fields, int lineNumber, List<String> errors) {
        String newBalance = getField(fields, newBalanceIndex);
        if (!isValid(fields, newBalanceIndex)) {
            String error = "New Balance is required";
            errors.add(error);
            log.error("Line {}: {}", lineNumber, error);
        } else if (!isValidDecimal(newBalance)) {
            String error = String.format(
                    "New Balance must be a valid decimal. Got: '%s'", newBalance);
            errors.add(error);
            log.error("Line {}: {}", lineNumber, error);
        } else if (!isPositive(newBalance)) {
            String error = String.format(
                    "New Balance must be a positive value. Got: '%s'", newBalance);
            errors.add(error);
            log.error("Line {}: {}", lineNumber, error);
        }
    }

    private void validateBalanceEquation(String[] fields, int lineNumber, List<String> errors) {
        String oldBalance = getField(fields, oldBalanceIndex);
        String cashEffect = getField(fields, cashEffectIndex);
        String newBalance = getField(fields, newBalanceIndex);

        // Only validate equation if all three values are valid
        if (isValid(fields, oldBalanceIndex) &&
                isValid(fields, cashEffectIndex) &&
                isValid(fields, newBalanceIndex) &&
                isValidDecimal(oldBalance) &&
                isValidDecimal(cashEffect) &&
                isValidDecimal(newBalance) &&
                isPositive(oldBalance) &&
                isPositive(cashEffect) &&
                isPositive(newBalance)) {

            BigDecimal oldBal = new BigDecimal(oldBalance.trim());
            BigDecimal cashEff = new BigDecimal(cashEffect.trim());
            BigDecimal newBal = new BigDecimal(newBalance.trim());
            BigDecimal calculated = oldBal.add(cashEff);

            if (calculated.compareTo(newBal) != 0) {
                String error = String.format(
                        "Balance mismatch: Old Balance (%s) + Cash Effect (%s) = %s, but New Balance is %s",
                        oldBalance, cashEffect, calculated, newBalance);
                errors.add(error);
                log.error("Line {}: {}", lineNumber, error);
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ══════════════════════════════════════════════════════════════════

    /**
     * Check if a field at given index exists and is not empty
     */
    private boolean isValid(String[] fields, int index) {
        try {
            if (fields == null || index < 0 || index >= fields.length) {
                return false;
            }
            String value = fields[index];
            return value != null && !value.trim().isEmpty();
        } catch (Exception e) {
            log.error("Error checking field validity at index {}: {}", index, e.getMessage());
            return false;
        }
    }

    /**
     * Get field value at given index safely
     */
    private String getField(String[] fields, int index) {
        try {
            return (fields != null && index >= 0 && index < fields.length)
                    ? fields[index]
                    : null;
        } catch (Exception e) {
            log.error("Error getting field at index {}: {}", index, e.getMessage());
            return null;
        }
    }

    /**
     * Check if value is a valid integer
     */
    private boolean isValidInteger(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        try {
            Integer.parseInt(value.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Check if value is a valid decimal number
     */
    private boolean isValidDecimal(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        try {
            new BigDecimal(value.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Check if decimal value is positive (> 0)
     */
    private boolean isPositive(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        try {
            return new BigDecimal(value.trim()).compareTo(BigDecimal.ZERO) > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Check if value is a valid date in yyyyMMdd format
     */
    private boolean isValidDate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        try {
            LocalDate parsedDate = LocalDate.parse(value.trim(), DATE_FORMAT);
            // Additional validation: check if date is reasonable
            LocalDate minDate = LocalDate.of(2000, 1, 1);
            LocalDate maxDate = LocalDate.of(2099, 12, 31);
            return !parsedDate.isBefore(minDate) && !parsedDate.isAfter(maxDate);
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    /**
     * Check if account number matches the configured pattern
     */
    private boolean isValidAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            return false;
        }
        String trimmed = accountNumber.trim();

        // Check length
        if (trimmed.length() < accountNumberMinLength ||
                trimmed.length() > accountNumberMaxLength) {
            return false;
        }

        // Check pattern
        return trimmed.matches(accountNumberPattern);
    }
}