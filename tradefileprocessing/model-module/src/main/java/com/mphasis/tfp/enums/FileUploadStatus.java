package com.mphasis.tfp.enums;

public enum FileUploadStatus {
    COMPLETED("COMPLETED"),
    PARTIALLY_COMPLETED("PARTIALLY_COMPLETED"),
    DELETED("DELETED"),
    ARCHIVED("ARCHIVED"),
    FAILED("FAILED"),
    PROCESSING("PROCESSING"),
    PENDING("PENDING");

    private final String value;

    FileUploadStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static FileUploadStatus fromValue(String value) {
        for (FileUploadStatus status : FileUploadStatus.values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid FileUploadStatus: " + value);
    }
}