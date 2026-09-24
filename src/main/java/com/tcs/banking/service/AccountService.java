package com.tcs.banking.service;

import com.tcs.banking.dto.AccountResponse;
import com.tcs.banking.dto.AmountRequest;
import com.tcs.banking.entity.*;
import com.tcs.banking.exception.AccountBlockedException;
import com.tcs.banking.exception.InsufficientBalanceException;
import com.tcs.banking.exception.ResourceNotFoundException;
import com.tcs.banking.exception.UnauthorizedAccountAccessException;
import com.tcs.banking.repository.AccountRepository;
import com.tcs.banking.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public List<AccountResponse> getMyAccounts(Long customerId) {
        return accountRepository.findByCustomerId(customerId).stream()
                .map(this::toResponse)
                .toList();
    }

    public AccountResponse getMyAccount(Long accountId, Long customerId) {
        return toResponse(getOwnedAccountForViewing(accountId, customerId));
    }

    @Transactional
    public AccountResponse deposit(Long accountId, Long customerId, AmountRequest request) {
        Account account = getOwnedAccount(accountId, customerId);
        account.setBalance(account.getBalance().add(request.getAmount()));
        accountRepository.save(account);
        recordTransaction(account, TransactionType.DEPOSIT, request.getAmount(), "Deposit");
        return toResponse(account);
    }

    @Transactional
    public AccountResponse withdraw(Long accountId, Long customerId, AmountRequest request) {
        Account account = getOwnedAccount(accountId, customerId);

        if (account.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientBalanceException("Insufficient balance");
        }

        account.setBalance(account.getBalance().subtract(request.getAmount()));
        accountRepository.save(account);
        recordTransaction(account, TransactionType.WITHDRAWAL, request.getAmount(), "Withdrawal");
        return toResponse(account);
    }

    // Central ownership check — reused everywhere an accountId comes in from a URL
    Account getOwnedAccount(Long accountId, Long customerId) {
        Account account = getOwnedAccountForViewing(accountId, customerId);

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountBlockedException("Account is not active");
        }

        return account;
    }

    private Account getOwnedAccountForViewing(Long accountId, Long customerId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        if (!account.getCustomer().getId().equals(customerId)) {
            throw new UnauthorizedAccountAccessException("You do not own this account");
        }
        return account;
    }

    private void recordTransaction(Account account, TransactionType type, java.math.BigDecimal amount, String description) {
        Transaction txn = new Transaction();
        txn.setTransactionId("TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        txn.setType(type);
        txn.setAmount(amount);
        txn.setStatus(TransactionStatus.SUCCESS);
        txn.setTimestamp(LocalDateTime.now());
        txn.setDescription(description);
        txn.setAccount(account);
        transactionRepository.save(txn);
    }

    private AccountResponse toResponse(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getAccountType().name(),
                account.getBalance(),
                account.getStatus().name()
        );
    }
}