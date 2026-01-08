package com.example.budgettracker.dto.Category;

import java.math.BigDecimal;


public record CategorySummaryResponse(
        CategoryResponse category,
        String yearMonth,
        long recordCount,
        BigDecimal totalExpense,
        BigDecimal totalIncome,
        BigDecimal budgetAmount,
        BigDecimal remainingBudget,
        BigDecimal expenseVsBudgetPct
) {

}
