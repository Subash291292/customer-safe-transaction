package com.example.transaction.repository;

import com.example.transaction.repository.entity.CustomerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CustomerEntityRepository extends JpaRepository<CustomerEntity, Long> {
    CustomerEntity findByUniqueId(String uniqueId);
    List<CustomerEntity> findByStatus(String status);
    List<CustomerEntity> findByStatusAndCreatedAtBefore(String failed, LocalDateTime oneDayAgo);
}
