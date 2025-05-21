package com.example.transaction.domain;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@NonNull
public class Customer {
    private String uniqueId;
    private String payload;
}
