package com.example.transaction.service;

import com.example.transaction.domain.Customer;
import com.example.transaction.repository.entity.CustomerEntity;
import com.example.transaction.repository.CustomerEntityRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomerTransactionServiceImpl implements CustomerTransactionService {

    private Logger logger = LoggerFactory.getLogger(CustomerTransactionServiceImpl.class);

    @NonNull
    private CustomerEntityRepository repository;
    
    @NonNull
    private  ExternalService externalService;

    @NonNull
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    @Override
    public void processData(List<Customer> data) {
        try {
            saveData(data);
            logger.info("Saved data and size {}", data.size());
            if(!CollectionUtils.isEmpty(data)) {
                eventPublisher.publishEvent(new TransactionCommittedEvent(data));
            }
        }catch (Exception e){
            logger.error("Something went wrong with data", e);
            throw e;
        }
    }

    @Transactional
    public void saveData(List<Customer> data) {
        List<CustomerEntity> entities = new ArrayList<>();
        for (Customer Customer : data) {
            CustomerEntity customerEntity = new CustomerEntity();
            customerEntity.setPayload(Customer.getPayload());
            customerEntity.setUniqueId(Customer.getUniqueId());
            customerEntity.setStatus("PENDING");
            customerEntity.setUpdatedAt(LocalDateTime.now());
            customerEntity.setCreatedAt(LocalDateTime.now());
            entities.add(customerEntity);
        }
        repository.saveAll(entities);

    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTransactionCommitted(TransactionCommittedEvent event) {
        externalService.handleAfterCommit(event.getData());
    }

    @Override
    public List<Customer> getAllCustomerList() {
       List<CustomerEntity>  customerEntities = repository.findAll();
       List<Customer> customers = new ArrayList<>();
       customerEntities.forEach(customerEntity -> {
          Customer customer = new Customer();
          customer.setPayload(customerEntity.getPayload());
          customer.setUniqueId(customerEntity.getUniqueId());
          customers.add(customer);
       });
       return customers;
    }

    @Override
    public Customer getCustomer(String uniqueId) {
       CustomerEntity customerEntity= repository.findByUniqueId(uniqueId);
        Customer customer = new Customer();
        customer.setPayload(customerEntity.getPayload());
        customer.setUniqueId(customerEntity.getUniqueId());
        return customer;
    }
}
