package com.example.budgettracker.dto.Dashboard;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * FR-14: Spending trend data for charts
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpendingTrendData {

    private String period; // "WEEK", "MONTH", "YEAR"
    private List<DataPoint> dataPoints;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DataPoint {
        private String label; // e.g., "Mon", "Jan", "2024"
        private LocalDate date;
        private BigDecimal amount;
        private boolean isToday; // Highlight current day/period
    }
}
