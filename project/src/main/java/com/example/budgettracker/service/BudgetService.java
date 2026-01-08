package com.example.budgettracker.service;

import com.example.budgettracker.dto.BudgetSummaryResponse;

import java.math.BigDecimal;
import java.time.YearMonth;

public interface BudgetService {
    BudgetSummaryResponse setUserCategoryMonthlyBudget(Long userId, Long categoryId, YearMonth ym, BigDecimal budget);
    BudgetSummaryResponse setUserCategoryMonthlyBudget(Long userId, Long categoryId, YearMonth ym, BigDecimal budget, String customName);
    BudgetSummaryResponse getUserCategoryBudgetSummary(Long userId, Long categoryId, YearMonth ym);
    boolean deleteUserCategoryMonthlyBudget(Long userId, Long categoryId, YearMonth ym);
}
