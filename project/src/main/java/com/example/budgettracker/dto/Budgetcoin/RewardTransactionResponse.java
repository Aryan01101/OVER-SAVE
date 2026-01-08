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
public class RewardTransactionResponse {
    private String type; // EARN or SPEND
    private String title;
    private String reference;
    private BigDecimal amount;
    private LocalDateTime occurredAt;
}
