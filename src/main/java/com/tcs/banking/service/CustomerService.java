package com.tcs.banking.service;

import com.tcs.banking.entity.Customer;
import com.tcs.banking.exception.ResourceNotFoundException;
import com.tcs.banking.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    public Customer getProfile(Long customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
    }
}
