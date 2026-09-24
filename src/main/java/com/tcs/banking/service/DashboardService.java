package com.tcs.banking.service;

import com.tcs.banking.dto.AccountResponse;
import com.tcs.banking.dto.DashboardData;
import com.tcs.banking.dto.TransactionResponse;
import com.tcs.banking.entity.Customer;
import com.tcs.banking.entity.Transaction;
import com.tcs.banking.exception.ResourceNotFoundException;
import com.tcs.banking.repository.CustomerRepository;
import com.tcs.banking.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final CustomerRepository customerRepository;
    private final AccountService accountService;
    private final TransactionRepository transactionRepository;

    public DashboardData getDashboard(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        List<AccountResponse> accounts = accountService.getMyAccounts(customerId);
        BigDecimal totalBalance = accounts.stream()
                .map(AccountResponse::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<TransactionResponse> recentTransactions = transactionRepository
                .findTop5ByAccountCustomerIdOrderByTimestampDesc(customerId)
                .stream()
                .map(this::toResponse)
                .toList();

        return new DashboardData(customer.getName(), totalBalance, accounts, recentTransactions);
    }

    private TransactionResponse toResponse(Transaction transaction) {
        return new TransactionResponse(
                transaction.getTransactionId(),
                transaction.getType().name(),
                transaction.getAmount(),
                transaction.getStatus().name(),
                transaction.getTimestamp(),
                transaction.getDescription()
        );
    }
}
