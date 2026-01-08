package com.example.budgettracker.dto.Dashboard;

import com.example.budgettracker.model.enums.CashFlowType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * FR-14: Transaction response for recent transactions display
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionResponse {

    private Long cashFlowId;
    private CashFlowType type; // INCOME or EXPENSE
    private BigDecimal amount;
    private String description;
    private LocalDateTime occurredAt;
    private LocalDateTime createdAt;

    // Category information
    private String categoryName;
    private String categoryIcon; // Icon/emoji for the category

    // Subscription information (if applicable)
    private boolean isSubscription;
    private String subscriptionMerchant;
}
