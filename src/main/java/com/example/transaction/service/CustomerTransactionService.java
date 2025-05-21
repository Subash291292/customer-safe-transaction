package com.example.transaction.service;

import com.example.transaction.domain.Customer;

import java.util.List;

public interface CustomerTransactionService {
    void processData(List<Customer> data);

    List<Customer> getAllCustomerList();

    Customer getCustomer(String uniqueId);
}
