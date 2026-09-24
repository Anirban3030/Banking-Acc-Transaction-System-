package com.tcs.banking.dto;

import lombok.Getter;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class TransactionResponse {
    private String transactionId;
    private String type;
    private BigDecimal amount;
    private String status;
    private LocalDateTime timestamp;
    private String description;
}