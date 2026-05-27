package com.mphasis.tfp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponseDTO<T> {

    private LocalDateTime timeStamp;
    private String status;
    private String code;
    private String message;
    private T data;

    public ApiResponseDTO(String status, String code, String message, T data) {
        this.timeStamp = LocalDateTime.now();
        this.status = status;
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResponseDTO<T> success(T data, String message) {
        return new ApiResponseDTO<>("SUCCESS", "200", message, data);
    }

    public static <T> ApiResponseDTO<T> error(String code, String message) {
        return new ApiResponseDTO<>("ERROR", code, message, null);
    }
}