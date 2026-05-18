package com.mphasis.tfp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorRequestDTO {

    private Long errorId;
    private String transactionId;
    private String accountNumber;
    private String errorField;
    private String status;
}