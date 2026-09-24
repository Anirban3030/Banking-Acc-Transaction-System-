package com.tcs.banking.service;

import com.tcs.banking.dto.LoginRequest;
import com.tcs.banking.dto.RegisterRequest;
import com.tcs.banking.entity.Account;
import com.tcs.banking.entity.AccountStatus;
import com.tcs.banking.entity.AccountType;
import com.tcs.banking.entity.Customer;
import com.tcs.banking.entity.User;
import com.tcs.banking.exception.DuplicateEmailException;
import com.tcs.banking.exception.InvalidTransactionException;
import com.tcs.banking.repository.AccountRepository;
import com.tcs.banking.repository.CustomerRepository;
import com.tcs.banking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException("Email already registered");
        }

        Customer customer = new Customer();
        customer.setName(request.getName());
        customer.setEmail(request.getEmail());
        customer.setPhone(request.getPhone());
        customerRepository.save(customer);

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setCustomer(customer);
        userRepository.save(user);

        // Open a default Savings account with zero balance
        Account account = new Account();
        account.setAccountNumber("AC-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase());
        account.setAccountType(AccountType.SAVINGS);
        account.setBalance(BigDecimal.ZERO);
        account.setStatus(AccountStatus.ACTIVE);
        account.setCustomer(customer);
        accountRepository.save(account);
    }

    public Customer login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidTransactionException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidTransactionException("Invalid email or password");
        }

        return user.getCustomer();
    }
}