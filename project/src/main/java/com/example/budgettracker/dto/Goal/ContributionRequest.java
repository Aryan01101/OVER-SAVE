package com.example.budgettracker.dto.Goal;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ContributionRequest {
    private Long fromAccountId; // cash account
    private Long goalId;
    private BigDecimal amount;
}
