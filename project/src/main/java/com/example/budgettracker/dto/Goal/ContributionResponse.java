package com.example.budgettracker.dto.Goal;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
@Getter
@Setter
public class ContributionResponse {
    private String message;
    private BigDecimal newGoalBalance;
    private BigDecimal newCashBalance;
}
