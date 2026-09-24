package com.tcs.banking.service;

import com.tcs.banking.dto.TransactionResponse;
import com.tcs.banking.dto.TransferRequest;
import com.tcs.banking.entity.*;
import com.tcs.banking.exception.AccountBlockedException;
import com.tcs.banking.exception.InsufficientBalanceException;
import com.tcs.banking.exception.InvalidTransactionException;
import com.tcs.banking.exception.ResourceNotFoundException;
import com.tcs.banking.repository.AccountRepository;
import com.tcs.banking.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final AccountService accountService;

    @Transactional
    public void transfer(Long customerId, TransferRequest request) {
        Account from = accountService.getOwnedAccount(request.getFromAccountId(), customerId);
        Account to = accountRepository.findById(request.getToAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Destination account not found"));

        if (to.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountBlockedException("Destination account is not active");
        }

        if (from.getId().equals(to.getId())) {
            throw new InvalidTransactionException("Cannot transfer to the same account");
        }
        if (from.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientBalanceException("Insufficient balance");
        }

        // Debit
        from.setBalance(from.getBalance().subtract(request.getAmount()));
        // Credit
        to.setBalance(to.getBalance().add(request.getAmount()));
        accountRepository.save(from);
        accountRepository.save(to);

        // Record — one row per side, so each account's history is self-contained.
        // Same TRANSFER type on both; the description says which direction.
        recordTransaction(from, TransactionType.TRANSFER, request.getAmount(),
                "Transfer to " + to.getAccountNumber());
        recordTransaction(to, TransactionType.TRANSFER, request.getAmount(),
                "Transfer from " + from.getAccountNumber());
    }

    public List<TransactionResponse> getHistory(Long accountId, Long customerId) {
        accountService.getOwnedAccount(accountId, customerId); // ownership check, result unused
        return transactionRepository.findByAccountIdOrderByTimestampDesc(accountId).stream()
                .map(t -> new TransactionResponse(
                        t.getTransactionId(),
                        t.getType().name(),
                        t.getAmount(),
                        t.getStatus().name(),
                        t.getTimestamp(),
                        t.getDescription()
                ))
                .toList();
    }

    private void recordTransaction(Account account, TransactionType type, BigDecimal amount, String description) {
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
}