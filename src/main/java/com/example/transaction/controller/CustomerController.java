package com.example.transaction.controller;

import com.example.transaction.domain.Customer;
import com.example.transaction.service.CustomerTransactionService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class CustomerController {

    @NonNull
    private final CustomerTransactionService transactionService;

    @PostMapping
    public String processTransaction(@RequestBody List<Customer> data) {
        transactionService.processData(data);
        return "Transaction completed for";
    }


    @GetMapping("/list")
    public ResponseEntity<List<Customer>> getList() {
        return ResponseEntity.ok(transactionService.getAllCustomerList());
    }

    @GetMapping("/get/customer/{id}")
    public ResponseEntity<Customer> getCustomer(@PathVariable String id) {
        return ResponseEntity.ok(transactionService.getCustomer(id));
    }
}
