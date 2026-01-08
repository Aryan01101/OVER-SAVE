package com.example.budgettracker.dto.Dashboard;

import com.example.budgettracker.dto.BudgetSummaryResponse;
import com.example.budgettracker.dto.Goal.GoalResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * FR-14: Complete dashboard response containing all dashboard data
 * This is the main response object that aggregates all dashboard components
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardResponse {

    // Financial aggregates (monthly income, expenses, balance, savings)
    private FinancialAggregatesResponse financialAggregates;

    // Budget data
    private List<BudgetSummaryResponse> budgets;

    // Active subscriptions
    private List<SubscriptionSummaryResponse> subscriptions;

    // Recent transactions (latest 10)
    private List<TransactionResponse> recentTransactions;

    // Spending trend data for charts
    private SpendingTrendData spendingTrend;

    // Active savings goals
    private List<GoalResponse> savingsGoals;

    // Data availability flags for fallback handling (FR-14 requirement)
    @Builder.Default
    private boolean dataAvailable = true;
    private String message;

    // Convenience getters for direct access to financial data (for API compatibility)
    @JsonProperty("totalIncome")
    public BigDecimal getTotalIncome() {
        return financialAggregates != null ? financialAggregates.getMonthlyIncome() : BigDecimal.ZERO;
    }

    @JsonProperty("totalExpenses")
    public BigDecimal getTotalExpenses() {
        return financialAggregates != null ? financialAggregates.getMonthlyExpenses() : BigDecimal.ZERO;
    }

    @JsonProperty("balance")
    public BigDecimal getBalance() {
        return financialAggregates != null ? financialAggregates.getCurrentBalance() : BigDecimal.ZERO;
    }
}
