package com.tcs.banking.controller;

import com.tcs.banking.dto.AccountResponse;
import com.tcs.banking.dto.AmountRequest;
import com.tcs.banking.service.AccountService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping("/customers/me/accounts")
    public ResponseEntity<List<AccountResponse>> myAccounts(HttpSession session) {
        Long customerId = requireLogin(session);
        return ResponseEntity.ok(accountService.getMyAccounts(customerId));
    }

    @PostMapping("/accounts/{id}/deposit")
    public ResponseEntity<AccountResponse> deposit(@PathVariable Long id,
                                                   @Valid @RequestBody AmountRequest request,
                                                   HttpSession session) {
        Long customerId = requireLogin(session);
        return ResponseEntity.ok(accountService.deposit(id, customerId, request));
    }

    @PostMapping("/accounts/{id}/withdraw")
    public ResponseEntity<AccountResponse> withdraw(@PathVariable Long id,
                                                    @Valid @RequestBody AmountRequest request,
                                                    HttpSession session) {
        Long customerId = requireLogin(session);
        return ResponseEntity.ok(accountService.withdraw(id, customerId, request));
    }

    private Long requireLogin(HttpSession session) {
        Long customerId = (Long) session.getAttribute("customerId");
        if (customerId == null) {
            throw new SecurityException("Not logged in");
        }
        return customerId;
    }
}