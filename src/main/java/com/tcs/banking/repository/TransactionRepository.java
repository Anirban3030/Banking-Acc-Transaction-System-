package com.tcs.banking.repository;

import com.tcs.banking.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByAccountIdOrderByTimestampDesc(Long accountId);
    List<Transaction> findTop5ByAccountCustomerIdOrderByTimestampDesc(Long customerId);
}