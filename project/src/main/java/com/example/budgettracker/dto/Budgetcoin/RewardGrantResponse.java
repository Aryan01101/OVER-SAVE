package com.example.budgettracker.dto.Budgetcoin;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RewardGrantResponse {
    private Long grantId;
    private Long userId;
    private BigDecimal amount;
    private String sourceType;
    private String rewardEventId;
    private LocalDateTime createdAt;
    private Long balanceAfter;
}
