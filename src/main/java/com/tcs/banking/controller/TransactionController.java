package com.tcs.banking.controller;

import com.tcs.banking.dto.TransactionResponse;
import com.tcs.banking.dto.TransferRequest;
import com.tcs.banking.service.TransactionService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/transfer")
    public ResponseEntity<String> transfer(@Valid @RequestBody TransferRequest request, HttpSession session) {
        Long customerId = requireLogin(session);
        transactionService.transfer(customerId, request);
        return ResponseEntity.ok("Transfer successful");
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<List<TransactionResponse>> history(@PathVariable Long accountId, HttpSession session) {
        Long customerId = requireLogin(session);
        return ResponseEntity.ok(transactionService.getHistory(accountId, customerId));
    }

    private Long requireLogin(HttpSession session) {
        Long customerId = (Long) session.getAttribute("customerId");
        if (customerId == null) {
            throw new SecurityException("Not logged in");
        }
        return customerId;
    }
}