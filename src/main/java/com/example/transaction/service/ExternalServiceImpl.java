package com.example.transaction.service;

import com.example.transaction.domain.Customer;
import com.example.transaction.repository.CustomerAuditRepository;
import com.example.transaction.repository.CustomerEntityRepository;
import com.example.transaction.repository.entity.CustomerAudit;
import com.example.transaction.repository.entity.CustomerEntity;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExternalServiceImpl implements ExternalService {

    private static final Logger logger = LoggerFactory.getLogger(ExternalServiceImpl.class);

    private final Set<String> processedIds = ConcurrentHashMap.newKeySet();

    @NonNull
    private final CustomerAuditRepository customerAuditRepository;

    @NonNull
    private final CustomerEntityRepository customerEntityRepository;

    @Transactional(rollbackFor = Exception.class)
    @Async("CustomerTaskExecutor")
    public void handleAfterCommit(List<Customer> customers) {
        int failureCount = 0;

        for (Customer data : customers) {
            String uniqueId = data.getUniqueId();
            boolean hasFailed = false;

            if (!processedIds.add(uniqueId)) {
                logger.info("Skipping duplicate processing for: {}", uniqueId);
                continue;
            }

            try {
                if (!isStageCompleted(uniqueId, "WRITE_FILE")) {
                    audit(uniqueId, "WRITE_FILE", "STARTED", null);
                    writeFile(data);
                    audit(uniqueId, "WRITE_FILE", "SUCCESS", null);
                }
            } catch (IOException e) {
                logger.error("WRITE_FILE failed for {}: {}", uniqueId, e.getMessage());
                audit(uniqueId, "WRITE_FILE", "FAILED", e.getMessage());
                hasFailed = true;
            }

            try {
                if (!isStageCompleted(uniqueId, "SEND_SQS")) {
                    audit(uniqueId, "SEND_SQS", "STARTED", null);
                    sendToSqs(data);
                    audit(uniqueId, "SEND_SQS", "SUCCESS", null);
                }
            } catch (Exception e) {
                logger.error("SEND_SQS failed for {}: {}", uniqueId, e.getMessage());
                audit(uniqueId, "SEND_SQS", "FAILED", e.getMessage());
                hasFailed = true;
            }

            if (hasFailed) {
                failureCount++;
                updateCustomerStatus(uniqueId, "FAILED");
            } else {
                updateCustomerStatus(uniqueId, "SUCCESS");
            }
        }

        if (failureCount > 1) {
            logger.warn("Rolling back due to multiple failures: count = {}", failureCount);
            throw new RuntimeException("Rolling back due to multiple failures.");
        }
    }

    public void writeFile(Customer data) throws IOException {
        if (Math.random() < 0.5) throw new IOException("Simulated file failure");
        logger.info("File written for: {}", data.getUniqueId());
    }

    public void sendToSqs(Customer data) throws Exception {
        if (Math.random() < 0.5) throw new Exception("Simulated SQS failure");
        logger.info("SQS message sent for: {}", data.getUniqueId());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void audit(String uniqueId, String stage, String status, String message) {
        CustomerAudit audit = new CustomerAudit();
        audit.setUniqueId(uniqueId);
        audit.setStage(stage);
        audit.setStatus(status);
        audit.setErrorMessage(message);
        audit.setTimestamp(LocalDateTime.now());
        customerAuditRepository.save(audit);
        logger.debug("Audit recorded: [id={}, stage={}, status={}]", uniqueId, stage, status);
    }

    private boolean isStageCompleted(String uniqueId, String stage) {
        return customerAuditRepository.findByUniqueId(uniqueId)
                .stream()
                .anyMatch(a -> a.getStage().equals(stage) && "SUCCESS".equals(a.getStatus()));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateCustomerStatus(String uniqueId, String status) {
        CustomerEntity entity = customerEntityRepository.findByUniqueId(uniqueId);
        if (entity != null) {
            entity.setStatus(status);
            entity.setUpdatedAt(LocalDateTime.now());
            customerEntityRepository.save(entity);
            logger.info("Updated status for {} to {}", uniqueId, status);
        } else {
            logger.warn("CustomerEntity not found for uniqueId: {}", uniqueId);
        }
    }

    @Scheduled(fixedDelayString = "${retry.scheduler.fixed-delay-ms}")
    public void processFailedCustomers() {
        List<CustomerEntity> failedEntities = customerEntityRepository.findByStatus("FAILED");

        if (failedEntities.isEmpty()) {
            logger.info("No FAILED customers found for retry.");
            return;
        }

        List<Customer> failedCustomers = failedEntities.stream()
                .map(entity -> new Customer(entity.getUniqueId(), entity.getPayload()))
                .collect(Collectors.toList());

        logger.info("Retrying FAILED customers. Count: {}", failedCustomers.size());
        handleAfterCommit(failedCustomers);
    }

    @Scheduled(cron = "${retry.scheduler.daily.check}")
    public void processFailedRecordMoreThanDay() {
        LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);

        List<CustomerEntity> oldFailedEntities = customerEntityRepository
                .findByStatusAndCreatedAtBefore("FAILED", oneDayAgo);

        if (oldFailedEntities.isEmpty()) {
            logger.info("No old FAILED customers to mark as INVALID.");
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        oldFailedEntities.forEach(entity -> {
            entity.setStatus("INVALID");
            entity.setUpdatedAt(now);
        });

        customerEntityRepository.saveAll(oldFailedEntities);

        logger.info("Marked {} old FAILED customers as INVALID.", oldFailedEntities.size());
    }
}