package com.example.transaction.repository.entity;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.time.LocalDateTime;

@Entity
@RequiredArgsConstructor
@Data
public class CustomerAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String uniqueId;

    private String stage; // e.g., "WRITE_FILE", "SEND_SQS"

    private String status; // e.g., "SUCCESS", "FAILED"

    private String errorMessage;

    private LocalDateTime timestamp;
}

