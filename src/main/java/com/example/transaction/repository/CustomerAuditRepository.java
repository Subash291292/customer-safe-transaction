package com.example.transaction.repository;

import com.example.transaction.repository.entity.CustomerAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerAuditRepository extends JpaRepository<CustomerAudit, Long> {
    List<CustomerAudit> findByUniqueId(String uniqueId);
}

