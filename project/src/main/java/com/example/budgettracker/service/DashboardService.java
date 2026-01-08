package com.example.budgettracker.service;

import com.example.budgettracker.dto.Dashboard.*;

/**
 * FR-14: Dashboard Service Interface
 * Provides aggregated data for dashboard display
 */
public interface DashboardService {

    /**
     * Get complete dashboard data for a user
     * @param userId User ID
     * @return DashboardResponse containing all dashboard components
     */
    DashboardResponse getDashboardData(Long userId);

    /**
     * Get financial aggregates (income, expenses, balance, savings)
     * @param userId User ID
     * @return FinancialAggregatesResponse
     */
    FinancialAggregatesResponse getFinancialAggregates(Long userId);

    /**
     * Get spending trend data for charts
     * @param userId User ID
     * @param period Period type: "WEEK", "MONTH", "YEAR"
     * @return SpendingTrendData
     */
    SpendingTrendData getSpendingTrend(Long userId, String period);
}
