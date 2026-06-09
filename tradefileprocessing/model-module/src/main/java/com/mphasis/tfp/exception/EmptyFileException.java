package com.mphasis.tfp.exception;


public class EmptyFileException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public EmptyFileException() {
        super("The uploaded file is empty. Please upload a valid file with data.");
    }

    public EmptyFileException(String message) {
        super(message);
    }

    public EmptyFileException(String message, Throwable cause) {
        super(message, cause);
    }
}