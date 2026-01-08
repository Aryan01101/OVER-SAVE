package com.example.budgettracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionResponse {

    private Long subscriptionId;
    private String merchant;
    private BigDecimal amount;
    private String frequency;
    private LocalDateTime startDate;
    private Boolean isActive;
    private LocalDateTime nextPostAt;
    private BigDecimal monthlyEquivalent;

}