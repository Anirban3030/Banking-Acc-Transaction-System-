package com.tcs.banking.dto;

import lombok.Getter;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class AccountResponse {
    private Long id;
    private String accountNumber;
    private String accountType;
    private BigDecimal balance;
    private String status;
}