package com.example.budgettracker.dto.Dashboard;

import lombok.*;

import java.math.BigDecimal;

/**
 * FR-14: Financial aggregates for dashboard display
 * Contains monthly income, expenses, current balance, and total savings
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinancialAggregatesResponse {

    // Monthly statistics
    private BigDecimal monthlyIncome;
    private BigDecimal monthlyExpenses;
    private BigDecimal currentBalance;
    private BigDecimal totalSavings;

    // Change percentages compared to previous month
    private Double incomeChangePercent;
    private Double expenseChangePercent;
    private Double savingsRate; // Percentage of income saved this month

    // Progress towards goals
    private Double goalsProgressPercent; // Overall progress across all active goals

    // Helper method to ensure valid numeric values (FR-14 requirement)
    public void validateNumericValues() {
        if (monthlyIncome == null) monthlyIncome = BigDecimal.ZERO;
        if (monthlyExpenses == null) monthlyExpenses = BigDecimal.ZERO;
        if (currentBalance == null) currentBalance = BigDecimal.ZERO;
        if (totalSavings == null) totalSavings = BigDecimal.ZERO;
        if (incomeChangePercent == null) incomeChangePercent = 0.0;
        if (expenseChangePercent == null) expenseChangePercent = 0.0;
        if (savingsRate == null) savingsRate = 0.0;
        if (goalsProgressPercent == null) goalsProgressPercent = 0.0;
    }
}
