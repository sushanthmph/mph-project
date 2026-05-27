package com.mphasis.tfp.exception;

import com.mphasis.tfp.dto.ApiResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Global Exception Handler for the entire application.
 * Handles all types of exceptions including custom and standard Java/Spring exceptions.
 *
 * @author Trade File Processing Team
 * @version 1.0
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ==================== CUSTOM EXCEPTIONS ====================

    /**
     * Handle Empty File Exception
     * Status Code: 400 BAD_REQUEST
     */
    @ExceptionHandler(EmptyFileException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> handleEmptyFileException(
            EmptyFileException ex,
            HttpServletRequest request) {

        log.error("Empty file exception: {} | Path: {}", ex.getMessage(), request.getRequestURI());

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("timestamp", LocalDateTime.now());
        errorDetails.put("path", request.getRequestURI());
        errorDetails.put("method", request.getMethod());
        errorDetails.put("error", "Empty File");
        errorDetails.put("message", ex.getMessage());

        ApiResponseDTO<Map<String, Object>> response = new ApiResponseDTO<>(
                "ERROR",
                String.valueOf(HttpStatus.BAD_REQUEST.value()),
                ex.getMessage(),
                errorDetails
        );

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle Invalid File Format Exception
     * Status Code: 400 BAD_REQUEST
     */
    @ExceptionHandler(InvalidFileFormatException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> handleInvalidFileFormatException(
            InvalidFileFormatException ex,
            HttpServletRequest request) {

        log.error("Invalid file format exception: {} | Path: {}", ex.getMessage(), request.getRequestURI());

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("timestamp", LocalDateTime.now());
        errorDetails.put("path", request.getRequestURI());
        errorDetails.put("method", request.getMethod());
        errorDetails.put("error", "Invalid File Format");
        errorDetails.put("message", ex.getMessage());

        ApiResponseDTO<Map<String, Object>> response = new ApiResponseDTO<>(
                "ERROR",
                String.valueOf(HttpStatus.BAD_REQUEST.value()),
                ex.getMessage(),
                errorDetails
        );

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // ==================== FILE HANDLING EXCEPTIONS ====================

    /**
     * Handle File Not Found Exception
     * Status Code: 404 NOT_FOUND
     */
    @ExceptionHandler({FileNotFoundException.class, NoSuchElementException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> handleFileNotFoundException(
            Exception ex,
            HttpServletRequest request) {

        log.error("File not found exception: {} | Path: {}", ex.getMessage(), request.getRequestURI());

        String message = ex.getMessage() != null && !ex.getMessage().isEmpty()
                ? ex.getMessage()
                : "The requested resource was not found";

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("timestamp", LocalDateTime.now());
        errorDetails.put("path", request.getRequestURI());
        errorDetails.put("method", request.getMethod());
        errorDetails.put("error", "Not Found");
        errorDetails.put("message", message);

        ApiResponseDTO<Map<String, Object>> response = new ApiResponseDTO<>(
                "ERROR",
                String.valueOf(HttpStatus.NOT_FOUND.value()),
                message,
                errorDetails
        );

        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    /**
     * Handle Max Upload Size Exceeded Exception
     * Status Code: 413 PAYLOAD_TOO_LARGE
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException ex,
            HttpServletRequest request) {

        log.error("Max upload size exceeded: {} | Path: {}", ex.getMessage(), request.getRequestURI());

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("timestamp", LocalDateTime.now());
        errorDetails.put("path", request.getRequestURI());
        errorDetails.put("method", request.getMethod());
        errorDetails.put("error", "File Too Large");
        errorDetails.put("message", "File size exceeds the maximum allowed limit");
        errorDetails.put("maxSize", "10MB");

        ApiResponseDTO<Map<String, Object>> response = new ApiResponseDTO<>(
                "ERROR",
                String.valueOf(HttpStatus.PAYLOAD_TOO_LARGE.value()),
                "File size exceeds the maximum allowed limit",
                errorDetails
        );

        return new ResponseEntity<>(response, HttpStatus.PAYLOAD_TOO_LARGE);
    }

    /**
     * Handle Multipart Exception
     * Status Code: 400 BAD_REQUEST
     */
    @ExceptionHandler(MultipartException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> handleMultipartException(
            MultipartException ex,
            HttpServletRequest request) {

        log.error("Multipart exception: {} | Path: {}", ex.getMessage(), request.getRequestURI());

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("timestamp", LocalDateTime.now());
        errorDetails.put("path", request.getRequestURI());
        errorDetails.put("method", request.getMethod());
        errorDetails.put("error", "Invalid File Upload");
        errorDetails.put("message", "Error processing file upload. Please ensure you're uploading a valid file");

        ApiResponseDTO<Map<String, Object>> response = new ApiResponseDTO<>(
                "ERROR",
                String.valueOf(HttpStatus.BAD_REQUEST.value()),
                "Error processing file upload",
                errorDetails
        );

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle IO Exception
     * Status Code: 500 INTERNAL_SERVER_ERROR
     */
    @ExceptionHandler(IOException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> handleIOException(
            IOException ex,
            HttpServletRequest request) {

        log.error("IO exception: {} | Path: {}", ex.getMessage(), request.getRequestURI(), ex);

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("timestamp", LocalDateTime.now());
        errorDetails.put("path", request.getRequestURI());
        errorDetails.put("method", request.getMethod());
        errorDetails.put("error", "File Processing Error");
        errorDetails.put("message", "Error reading or writing file. Please try again");

        ApiResponseDTO<Map<String, Object>> response = new ApiResponseDTO<>(
                "ERROR",
                String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()),
                "Error reading or writing file",
                errorDetails
        );

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Handle Access Denied Exception
     * Status Code: 403 FORBIDDEN
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> handleAccessDeniedException(
            AccessDeniedException ex,
            HttpServletRequest request) {

        log.error("Access denied exception: {} | Path: {}", ex.getMessage(), request.getRequestURI());

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("timestamp", LocalDateTime.now());
        errorDetails.put("path", request.getRequestURI());
        errorDetails.put("method", request.getMethod());
        errorDetails.put("error", "Access Denied");
        errorDetails.put("message", "You don't have permission to access this resource");

        ApiResponseDTO<Map<String, Object>> response = new ApiResponseDTO<>(
                "ERROR",
                String.valueOf(HttpStatus.FORBIDDEN.value()),
                "Access denied",
                errorDetails
        );

        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }

    // ==================== DATABASE EXCEPTIONS ====================

    /**
     * Handle Data Access Exception (General Database Errors)
     * Status Code: 500 INTERNAL_SERVER_ERROR
     */
    @ExceptionHandler(DataAccessException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> handleDataAccessException(
            DataAccessException ex,
            HttpServletRequest request) {

        log.error("Data access exception: {} | Path: {}", ex.getMessage(), request.getRequestURI(), ex);

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("timestamp", LocalDateTime.now());
        errorDetails.put("path", request.getRequestURI());
        errorDetails.put("method", request.getMethod());
        errorDetails.put("error", "Database Error");
        errorDetails.put("message", "Database error occurred while processing your request");

        ApiResponseDTO<Map<String, Object>> response = new ApiResponseDTO<>(
                "ERROR",
                String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()),
                "Database error occurred",
                errorDetails
        );

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Handle Data Integrity Violation Exception
     * Status Code: 409 CONFLICT
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> handleDataIntegrityViolationException(
            DataIntegrityViolationException ex,
            HttpServletRequest request) {

        log.error("Data integrity violation: {} | Path: {}", ex.getMessage(), request.getRequestURI());

        String message = "Data integrity violation. The operation conflicts with existing data";

        // Check for specific constraint violations
        if (ex.getMessage() != null) {
            if (ex.getMessage().contains("Duplicate entry") ||
                    ex.getMessage().contains("duplicate key") ||
                    ex.getMessage().contains("unique constraint")) {
                message = "Duplicate entry. A record with the same key already exists";
            }
        }

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("timestamp", LocalDateTime.now());
        errorDetails.put("path", request.getRequestURI());
        errorDetails.put("method", request.getMethod());
        errorDetails.put("error", "Data Conflict");
        errorDetails.put("message", message);

        ApiResponseDTO<Map<String, Object>> response = new ApiResponseDTO<>(
                "ERROR",
                String.valueOf(HttpStatus.CONFLICT.value()),
                message,
                errorDetails
        );

        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    /**
     * Handle Empty Result Data Access Exception
     * Status Code: 404 NOT_FOUND
     */
    @ExceptionHandler(EmptyResultDataAccessException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> handleEmptyResultDataAccessException(
            EmptyResultDataAccessException ex,
            HttpServletRequest request) {

        log.error("Empty result data access exception: {} | Path: {}", ex.getMessage(), request.getRequestURI());

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("timestamp", LocalDateTime.now());
        errorDetails.put("path", request.getRequestURI());
        errorDetails.put("method", request.getMethod());
        errorDetails.put("error", "Not Found");
        errorDetails.put("message", "No data found for the requested operation");

        ApiResponseDTO<Map<String, Object>> response = new ApiResponseDTO<>(
                "ERROR",
                String.valueOf(HttpStatus.NOT_FOUND.value()),
                "No data found",
                errorDetails
        );

        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    /**
     * Handle SQL Exception
     * Status Code: 500 INTERNAL_SERVER_ERROR
     */
    @ExceptionHandler(SQLException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> handleSQLException(
            SQLException ex,
            HttpServletRequest request) {

        log.error("SQL exception: Code={}, State={}, Message={} | Path: {}",
                ex.getErrorCode(), ex.getSQLState(), ex.getMessage(), request.getRequestURI(), ex);

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("timestamp", LocalDateTime.now());
        errorDetails.put("path", request.getRequestURI());
        errorDetails.put("method", request.getMethod());
        errorDetails.put("error", "Database Error");
        errorDetails.put("message", "Database error occurred. Please try again later");
        errorDetails.put("sqlErrorCode", ex.getErrorCode());
        errorDetails.put("sqlState", ex.getSQLState());

        ApiResponseDTO<Map<String, Object>> response = new ApiResponseDTO<>(
                "ERROR",
                String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()),
                "Database error occurred",
                errorDetails
        );

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // ==================== VALIDATION EXCEPTIONS ====================

    /**
     * Handle Method Argument Not Valid Exception (Bean Validation)
     * Status Code: 400 BAD_REQUEST
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        log.error("Validation error: {} | Path: {}", ex.getMessage(), request.getRequestURI());

        List<Map<String, String>> fieldErrors = new ArrayList<>();

        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            Map<String, String> fieldError = new HashMap<>();
            fieldError.put("field", error.getField());
            fieldError.put("message", error.getDefaultMessage());
            fieldError.put("rejectedValue", error.getRejectedValue() != null ?
                    error.getRejectedValue().toString() : "null");
            fieldErrors.add(fieldError);
        }

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("timestamp", LocalDateTime.now());
        errorDetails.put("path", request.getRequestURI());
        errorDetails.put("method", request.getMethod());
        errorDetails.put("error", "Validation Failed");
        errorDetails.put("message", "Validation failed for one or more fields");
        errorDetails.put("fieldErrors", fieldErrors);
        errorDetails.put("errorCount", fieldErrors.size());

        ApiResponseDTO<Map<String, Object>> response = new ApiResponseDTO<>(
                "ERROR",
                String.valueOf(HttpStatus.BAD_REQUEST.value()),
                "Validation failed",
                errorDetails
        );

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle Illegal Argument Exception
     * Status Code: 400 BAD_REQUEST
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> handleIllegalArgumentException(
            IllegalArgumentException ex,
            HttpServletRequest request) {

        log.error("Illegal argument: {} | Path: {}", ex.getMessage(), request.getRequestURI());

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("timestamp", LocalDateTime.now());
        errorDetails.put("path", request.getRequestURI());
        errorDetails.put("method", request.getMethod());
        errorDetails.put("error", "Invalid Argument");
        errorDetails.put("message", ex.getMessage());

        ApiResponseDTO<Map<String, Object>> response = new ApiResponseDTO<>(
                "ERROR",
                String.valueOf(HttpStatus.BAD_REQUEST.value()),
                ex.getMessage(),
                errorDetails
        );

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle Missing Servlet Request Parameter Exception
     * Status Code: 400 BAD_REQUEST
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException ex,
            HttpServletRequest request) {

        log.error("Missing parameter: {} | Path: {}", ex.getMessage(), request.getRequestURI());

        String message = String.format("Required parameter '%s' of type '%s' is missing",
                ex.getParameterName(), ex.getParameterType());

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("timestamp", LocalDateTime.now());
        errorDetails.put("path", request.getRequestURI());
        errorDetails.put("method", request.getMethod());
        errorDetails.put("error", "Missing Parameter");
        errorDetails.put("message", message);
        errorDetails.put("parameterName", ex.getParameterName());
        errorDetails.put("parameterType", ex.getParameterType());

        ApiResponseDTO<Map<String, Object>> response = new ApiResponseDTO<>(
                "ERROR",
                String.valueOf(HttpStatus.BAD_REQUEST.value()),
                message,
                errorDetails
        );

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle Method Argument Type Mismatch Exception
     * Status Code: 400 BAD_REQUEST
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {

        log.error("Type mismatch: {} | Path: {}", ex.getMessage(), request.getRequestURI());

        String message = String.format("Parameter '%s' should be of type %s",
                ex.getName(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("timestamp", LocalDateTime.now());
        errorDetails.put("path", request.getRequestURI());
        errorDetails.put("method", request.getMethod());
        errorDetails.put("error", "Type Mismatch");
        errorDetails.put("message", message);
        errorDetails.put("parameterName", ex.getName());
        errorDetails.put("providedValue", ex.getValue());
        errorDetails.put("requiredType", ex.getRequiredType() != null ?
                ex.getRequiredType().getSimpleName() : "unknown");

        ApiResponseDTO<Map<String, Object>> response = new ApiResponseDTO<>(
                "ERROR",
                String.valueOf(HttpStatus.BAD_REQUEST.value()),
                message,
                errorDetails
        );

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle HTTP Message Not Readable Exception
     * Status Code: 400 BAD_REQUEST
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {

        log.error("Message not readable: {} | Path: {}", ex.getMessage(), request.getRequestURI());

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("timestamp", LocalDateTime.now());
        errorDetails.put("path", request.getRequestURI());
        errorDetails.put("method", request.getMethod());
        errorDetails.put("error", "Malformed Request");
        errorDetails.put("message", "Malformed JSON request or invalid request body format");

        ApiResponseDTO<Map<String, Object>> response = new ApiResponseDTO<>(
                "ERROR",
                String.valueOf(HttpStatus.BAD_REQUEST.value()),
                "Malformed request body",
                errorDetails
        );

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle No Handler Found Exception
     * Status Code: 404 NOT_FOUND
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> handleNoHandlerFoundException(
            NoHandlerFoundException ex,
            HttpServletRequest request) {

        log.error("No handler found: {} | Path: {}", ex.getMessage(), request.getRequestURI());

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("timestamp", LocalDateTime.now());
        errorDetails.put("path", request.getRequestURI());
        errorDetails.put("method", request.getMethod());
        errorDetails.put("error", "Endpoint Not Found");
        errorDetails.put("message", "No endpoint found for " + ex.getHttpMethod() + " " + ex.getRequestURL());

        ApiResponseDTO<Map<String, Object>> response = new ApiResponseDTO<>(
                "ERROR",
                String.valueOf(HttpStatus.NOT_FOUND.value()),
                "Endpoint not found",
                errorDetails
        );

        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    // ==================== RUNTIME EXCEPTIONS ====================

    /**
     * Handle Null Pointer Exception
     * Status Code: 500 INTERNAL_SERVER_ERROR
     */
    @ExceptionHandler(NullPointerException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> handleNullPointerException(
            NullPointerException ex,
            HttpServletRequest request) {

        log.error("Null pointer exception: {} | Path: {}", ex.getMessage(), request.getRequestURI(), ex);

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("timestamp", LocalDateTime.now());
        errorDetails.put("path", request.getRequestURI());
        errorDetails.put("method", request.getMethod());
        errorDetails.put("error", "Internal Server Error");
        errorDetails.put("message", "An unexpected error occurred. Please try again later");

        ApiResponseDTO<Map<String, Object>> response = new ApiResponseDTO<>(
                "ERROR",
                String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()),
                "An unexpected error occurred",
                errorDetails
        );

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Handle Number Format Exception
     * Status Code: 400 BAD_REQUEST
     */
    @ExceptionHandler(NumberFormatException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> handleNumberFormatException(
            NumberFormatException ex,
            HttpServletRequest request) {

        log.error("Number format exception: {} | Path: {}", ex.getMessage(), request.getRequestURI());

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("timestamp", LocalDateTime.now());
        errorDetails.put("path", request.getRequestURI());
        errorDetails.put("method", request.getMethod());
        errorDetails.put("error", "Invalid Number Format");
        errorDetails.put("message", "Invalid number format. Please provide a valid numeric value");

        ApiResponseDTO<Map<String, Object>> response = new ApiResponseDTO<>(
                "ERROR",
                String.valueOf(HttpStatus.BAD_REQUEST.value()),
                "Invalid number format",
                errorDetails
        );

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle Illegal State Exception
     * Status Code: 500 INTERNAL_SERVER_ERROR
     */
    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> handleIllegalStateException(
            IllegalStateException ex,
            HttpServletRequest request) {

        log.error("Illegal state exception: {} | Path: {}", ex.getMessage(), request.getRequestURI(), ex);

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("timestamp", LocalDateTime.now());
        errorDetails.put("path", request.getRequestURI());
        errorDetails.put("method", request.getMethod());
        errorDetails.put("error", "Illegal State");
        errorDetails.put("message", ex.getMessage());

        ApiResponseDTO<Map<String, Object>> response = new ApiResponseDTO<>(
                "ERROR",
                String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()),
                ex.getMessage(),
                errorDetails
        );

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Handle Runtime Exception (Generic)
     * Status Code: 500 INTERNAL_SERVER_ERROR
     */
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> handleRuntimeException(
            RuntimeException ex,
            HttpServletRequest request) {

        log.error("Runtime exception: {} | Path: {}", ex.getMessage(), request.getRequestURI(), ex);

        String message = ex.getMessage() != null && !ex.getMessage().isEmpty()
                ? ex.getMessage()
                : "An unexpected error occurred while processing your request";

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("timestamp", LocalDateTime.now());
        errorDetails.put("path", request.getRequestURI());
        errorDetails.put("method", request.getMethod());
        errorDetails.put("error", "Runtime Error");
        errorDetails.put("message", message);
        errorDetails.put("exceptionType", ex.getClass().getSimpleName());

        ApiResponseDTO<Map<String, Object>> response = new ApiResponseDTO<>(
                "ERROR",
                String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()),
                message,
                errorDetails
        );

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // ==================== GENERIC EXCEPTION (CATCH-ALL) ====================

    /**
     * Handle Generic Exception (Catch-all for any unhandled exceptions)
     * Status Code: 500 INTERNAL_SERVER_ERROR
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> handleGlobalException(
            Exception ex,
            HttpServletRequest request) {

        log.error("Unexpected error occurred: {} | Path: {}", ex.getMessage(), request.getRequestURI(), ex);

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("timestamp", LocalDateTime.now());
        errorDetails.put("path", request.getRequestURI());
        errorDetails.put("method", request.getMethod());
        errorDetails.put("error", "Internal Server Error");
        errorDetails.put("message", "An unexpected error occurred. Please try again later");
        errorDetails.put("exceptionType", ex.getClass().getSimpleName());

        ApiResponseDTO<Map<String, Object>> response = new ApiResponseDTO<>(
                "ERROR",
                String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()),
                "Internal server error",
                errorDetails
        );

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}