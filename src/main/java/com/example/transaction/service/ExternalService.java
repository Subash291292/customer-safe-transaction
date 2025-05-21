package com.example.transaction.service;

import com.example.transaction.domain.Customer;

import java.util.List;

public interface ExternalService {
    void handleAfterCommit(List<Customer> data);
}
