package com.example.transaction.service;

import com.example.transaction.domain.Customer;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.List;

@Getter
public class TransactionCommittedEvent extends ApplicationEvent {

     List<Customer> data;

    public TransactionCommittedEvent(List<Customer> source) {
        super(source);
        this.data = source;
    }

}

