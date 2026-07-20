package com.example.salonflow.dto.loyalty;

import com.example.salonflow.entity.enums.LoyaltyTransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoyaltyTransactionResponse {
    private Long id;
    private LoyaltyTransactionType transactionType;
    private Integer points;
    private String referenceId;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
}
