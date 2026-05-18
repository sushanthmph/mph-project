package com.mphasis.tfp.exception;

/**
 * Exception thrown when file format is not valid or supported.
 */
public class InvalidFileFormatException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvalidFileFormatException() {
        super("Invalid file format. Only CSV files are accepted.");
    }

    public InvalidFileFormatException(String message) {
        super(message);
    }

    public InvalidFileFormatException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidFileFormatException(String fileName, String expectedFormat) {
        super(String.format("Invalid file format for '%s'. Expected format: %s", fileName, expectedFormat));
    }
}