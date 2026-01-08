package com.example.budgettracker.dto.Dashboard;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * FR-14: Subscription summary for dashboard display
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionSummaryResponse {

    private Long subscriptionId;
    private String merchant;
    private BigDecimal amount;
    private String frequency; // e.g., "MONTHLY", "YEARLY", "WEEKLY"
    private LocalDateTime nextPostAt;
    private boolean isActive;

    // Additional computed fields
    private BigDecimal monthlyEquivalent; // Normalized to monthly amount for comparison
}
