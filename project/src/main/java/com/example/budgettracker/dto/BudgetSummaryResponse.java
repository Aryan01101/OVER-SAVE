package com.example.budgettracker.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class BudgetSummaryResponse {
    private Long categoryId;
    private Long userId;
    private String yearMonth;
    private BigDecimal budget;
    private BigDecimal spent;
    private BigDecimal remaining;
    private String customName;

    public BudgetSummaryResponse(Long categoryId, Long userId, String yearMonth,
                                 BigDecimal budget, BigDecimal spent) {
        this.categoryId = categoryId;
        this.userId = userId;
        this.yearMonth = yearMonth;
        this.budget = budget == null ? BigDecimal.ZERO : budget;
        this.spent = spent == null ? BigDecimal.ZERO : spent;
        this.remaining = this.budget.subtract(this.spent);
    }

    public BudgetSummaryResponse(Long categoryId, Long userId, String yearMonth,
                                 BigDecimal budget, BigDecimal spent, String customName) {
        this(categoryId, userId, yearMonth, budget, spent);
        this.customName = customName;
    }

}
