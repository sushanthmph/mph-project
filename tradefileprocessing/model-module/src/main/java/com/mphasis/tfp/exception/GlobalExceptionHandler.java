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

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final String KEY_TIMESTAMP   = "timestamp";
    private static final String KEY_PATH        = "path";
    private static final String KEY_METHOD      = "method";
    private static final String KEY_ERROR       = "error";
    private static final String KEY_MESSAGE     = "message";
    private static final String STATUS_ERROR    = "ERROR";


    private Map<String, Object> buildErrorDetails(HttpServletRequest request,
                                                  String error,
                                                  String message) {
        Map<String, Object> details = new HashMap<>();
        details.put(KEY_TIMESTAMP, LocalDateTime.now());
        details.put(KEY_PATH, request.getRequestURI());
        details.put(KEY_METHOD, request.getMethod());
        details.put(KEY_ERROR, error);
        details.put(KEY_MESSAGE, message);
        return details;
    }


    private ResponseEntity<ApiResponseDTO<Map<String, Object>>> buildResponse(
            HttpStatus status, String error, String message,
            HttpServletRequest request) {
        Map<String, Object> errorDetails = buildErrorDetails(request, error, message);
        ApiResponseDTO<Map<String, Object>> response = new ApiResponseDTO<>(
                STATUS_ERROR,
                String.valueOf(status.value()),
                message,
                errorDetails
        );
        return new ResponseEntity<>(response, status);
    }


    @ExceptionHandler(EmptyFileException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> handleEmptyFileException(
            EmptyFileException ex, HttpServletRequest request) {
        log.error("Empty file exception: {} | Path: {}", ex.getMessage(), request.getRequestURI());
        return buildResponse(HttpStatus.BAD_REQUEST, "Empty File", ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidFileFormatException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> handleInvalidFileFormatException(
            InvalidFileFormatException ex, HttpServletRequest request) {
        log.error("Invalid file format exception: {} | Path: {}", ex.getMessage(), request.getRequestURI());
        return buildResponse(HttpStatus.BAD_REQUEST, "Invalid File Format", ex.getMessage(), request);
    }

    @ExceptionHandler({FileNotFoundException.class, NoSuchElementException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> handleFileNotFoundException(
            Exception ex, HttpServletRequest request) {
        log.error("File not found exception: {} | Path: {}", ex.getMessage(), request.getRequestURI());
        String message = ex.getMessage() != null && !ex.getMessage().isEmpty()
                ? ex.getMessage()
                : "The requested resource was not found";
        return buildResponse(HttpStatus.NOT_FOUND, "Not Found", message, request);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException ex, HttpServletRequest request) {
        log.error("Max upload size exceeded: {} | Path: {}", ex.getMessage(), request.getRequestURI());
        Map<String, Object> errorDetails = buildErrorDetails(request,
                "File Too Large", "File size exceeds the maximum allowed limit");
        errorDetails.put("maxSize", "10MB");
        ApiResponseDTO<Map<String, Object>> response = new ApiResponseDTO<>(
                STATUS_ERROR,
                String.valueOf(HttpStatus.PAYLOAD_TOO_LARGE.value()),
                "File size exceeds the maximum allowed limit",
                errorDetails
        );
        return new ResponseEntity<>(response, HttpStatus.PAYLOAD_TOO_LARGE);
    }

    @ExceptionHandler(MultipartException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> handleMultipartException(
            MultipartException ex, HttpServletRequest request) {
        log.error("Multipart exception: {} | Path: {}", ex.getMessage(), request.getRequestURI());
        return buildResponse(HttpStatus.BAD_REQUEST, "Invalid File Upload",
                "Error processing file upload. Please ensure you're uploading a valid file", request);
    }

    @ExceptionHandler(IOException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> handleIOException(
            IOException ex, HttpServletRequest request) {
        log.error("IO exception: {} | Path: {}", ex.getMessage(), request.getRequestURI(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "File Processing Error",
                "Error reading or writing file. Please try again", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> handleAccessDeniedException(
            AccessDeniedException ex, HttpServletRequest request) {
        log.error("Access denied exception: {} | Path: {}", ex.getMessage(), request.getRequestURI());
        return buildResponse(HttpStatus.FORBIDDEN, "Access Denied",
                "You don't have permission to access this resource", request);
    }

    @ExceptionHandler(DataAccessException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> handleDataAccessException(
            DataAccessException ex, HttpServletRequest request) {
        log.error("Data access exception: {} | Path: {}", ex.getMessage(), request.getRequestURI(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Database Error",
                "Database error occurred while processing your request", request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> handleDataIntegrityViolationException(
            DataIntegrityViolationException ex, HttpServletRequest request) {
        log.error("Data integrity violation: {} | Path: {}", ex.getMessage(), request.getRequestURI());

        String message = (ex.getMessage() != null &&
                (ex.getMessage().contains("Duplicate entry") ||
                        ex.getMessage().contains("duplicate key") ||
                        ex.getMessage().contains("unique constraint")))
                ? "Duplicate entry. A record with the same key already exists"
                : "Data integrity violation. The operation conflicts with existing data";

        return buildResponse(HttpStatus.CONFLICT, "Data Conflict", message, request);
    }

    @ExceptionHandler(EmptyResultDataAccessException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> handleEmptyResultDataAccessException(
            EmptyResultDataAccessException ex, HttpServletRequest request) {
        log.error("Empty result data access exception: {} | Path: {}", ex.getMessage(), request.getRequestURI());
        return buildResponse(HttpStatus.NOT_FOUND, "Not Found",
                "No data found for the requested operation", request);
    }

    @ExceptionHandler(SQLException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> handleSQLException(
            SQLException ex, HttpServletRequest request) {
        log.error("SQL exception: Code={}, State={}, Message={} | Path: {}",
                ex.getErrorCode(), ex.getSQLState(), ex.getMessage(), request.getRequestURI(), ex);
        Map<String, Object> errorDetails = buildErrorDetails(request,
                "Database Error", "Database error occurred. Please try again later");
        errorDetails.put("sqlErrorCode", ex.getErrorCode());
        errorDetails.put("sqlState", ex.getSQLState());
        ApiResponseDTO<Map<String, Object>> response = new ApiResponseDTO<>(
                STATUS_ERROR,
                String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()),
                "Database error occurred",
                errorDetails
        );
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        log.error("Validation error: {} | Path: {}", ex.getMessage(), request.getRequestURI());

        List<Map<String, String>> fieldErrors = new ArrayList<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            Map<String, String> fieldError = new HashMap<>();
            fieldError.put("field", error.getField());
            fieldError.put(KEY_MESSAGE, error.getDefaultMessage());
            fieldError.put("rejectedValue", error.getRejectedValue() != null ?
                    error.getRejectedValue().toString() : "null");
            fieldErrors.add(fieldError);
        }

        Map<String, Object> errorDetails = buildErrorDetails(request,
                "Validation Failed", "Validation failed for one or more fields");
        errorDetails.put("fieldErrors", fieldErrors);
        errorDetails.put("errorCount", fieldErrors.size());

        ApiResponseDTO<Map<String, Object>> response = new ApiResponseDTO<>(
                STATUS_ERROR,
                String.valueOf(HttpStatus.BAD_REQUEST.value()),
                "Validation failed",
                errorDetails
        );
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> handleIllegalArgumentException(
            IllegalArgumentException ex, HttpServletRequest request) {
        log.error("Illegal argument: {} | Path: {}", ex.getMessage(), request.getRequestURI());
        return buildResponse(HttpStatus.BAD_REQUEST, "Invalid Argument", ex.getMessage(), request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException ex, HttpServletRequest request) {
        log.error("Missing parameter: {} | Path: {}", ex.getMessage(), request.getRequestURI());
        String message = String.format("Required parameter '%s' of type '%s' is missing",
                ex.getParameterName(), ex.getParameterType());
        Map<String, Object> errorDetails = buildErrorDetails(request, "Missing Parameter", message);
        errorDetails.put("parameterName", ex.getParameterName());
        errorDetails.put("parameterType", ex.getParameterType());
        ApiResponseDTO<Map<String, Object>> response = new ApiResponseDTO<>(
                STATUS_ERROR,
                String.valueOf(HttpStatus.BAD_REQUEST.value()),
                message,
                errorDetails
        );
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        log.error("Type mismatch: {} | Path: {}", ex.getMessage(), request.getRequestURI());
        String message = String.format("Parameter '%s' should be of type %s",
                ex.getName(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");
        Map<String, Object> errorDetails = buildErrorDetails(request, "Type Mismatch", message);
        errorDetails.put("parameterName", ex.getName());
        errorDetails.put("providedValue", ex.getValue());
        errorDetails.put("requiredType", ex.getRequiredType() != null ?
                ex.getRequiredType().getSimpleName() : "unknown");
        ApiResponseDTO<Map<String, Object>> response = new ApiResponseDTO<>(
                STATUS_ERROR,
                String.valueOf(HttpStatus.BAD_REQUEST.value()),
                message,
                errorDetails
        );
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.error("Message not readable: {} | Path: {}", ex.getMessage(), request.getRequestURI());
        return buildResponse(HttpStatus.BAD_REQUEST, "Malformed Request",
                "Malformed JSON request or invalid request body format", request);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> handleNoHandlerFoundException(
            NoHandlerFoundException ex, HttpServletRequest request) {
        log.error("No handler found: {} | Path: {}", ex.getMessage(), request.getRequestURI());
        return buildResponse(HttpStatus.NOT_FOUND, "Endpoint Not Found",
                "No endpoint found for " + ex.getHttpMethod() + " " + ex.getRequestURL(), request);
    }

    @ExceptionHandler(NullPointerException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> handleNullPointerException(
            NullPointerException ex, HttpServletRequest request) {
        log.error("Null pointer exception: {} | Path: {}", ex.getMessage(), request.getRequestURI(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "An unexpected error occurred. Please try again later", request);
    }

    @ExceptionHandler(NumberFormatException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> handleNumberFormatException(
            NumberFormatException ex, HttpServletRequest request) {
        log.error("Number format exception: {} | Path: {}", ex.getMessage(), request.getRequestURI());
        return buildResponse(HttpStatus.BAD_REQUEST, "Invalid Number Format",
                "Invalid number format. Please provide a valid numeric value", request);
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> handleIllegalStateException(
            IllegalStateException ex, HttpServletRequest request) {
        log.error("Illegal state exception: {} | Path: {}", ex.getMessage(), request.getRequestURI(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Illegal State", ex.getMessage(), request);
    }

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> handleRuntimeException(
            RuntimeException ex, HttpServletRequest request) {
        log.error("Runtime exception: {} | Path: {}", ex.getMessage(), request.getRequestURI(), ex);
        String message = ex.getMessage() != null && !ex.getMessage().isEmpty()
                ? ex.getMessage()
                : "An unexpected error occurred while processing your request";
        Map<String, Object> errorDetails = buildErrorDetails(request, "Runtime Error", message);
        errorDetails.put("exceptionType", ex.getClass().getSimpleName());
        ApiResponseDTO<Map<String, Object>> response = new ApiResponseDTO<>(
                STATUS_ERROR,
                String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()),
                message,
                errorDetails
        );
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiResponseDTO<String>> handleDuplicateResource(
            DuplicateResourceException ex) {
        log.error("Duplicate resource error: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponseDTO.error("409", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> handleGlobalException(
            Exception ex, HttpServletRequest request) {
        log.error("Unexpected error occurred: {} | Path: {}", ex.getMessage(), request.getRequestURI(), ex);
        Map<String, Object> errorDetails = buildErrorDetails(request,
                "Internal Server Error", "An unexpected error occurred. Please try again later");
        errorDetails.put("exceptionType", ex.getClass().getSimpleName());
        ApiResponseDTO<Map<String, Object>> response = new ApiResponseDTO<>(
                STATUS_ERROR,
                String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()),
                "Internal server error",
                errorDetails
        );
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}