package com.tcs.banking.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@AllArgsConstructor
public class DashboardData {
    private String customerName;
    private BigDecimal totalBalance;
    private List<AccountResponse> accounts;
    private List<TransactionResponse> recentTransactions;
}
