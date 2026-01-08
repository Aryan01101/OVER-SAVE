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
public class RewardRedeemResponse {
    private Long orderId;
    private Long userId;
    private Long itemId;
    private String itemName;
    private BigDecimal amount;
    private LocalDateTime redeemedAt;
    private Long balanceAfter;
}
