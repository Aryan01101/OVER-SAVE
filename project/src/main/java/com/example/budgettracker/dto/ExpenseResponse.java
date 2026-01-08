package com.example.budgettracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * FR-7 Expense Recording Response DTO
 * Returns confirmation with updated balance
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseResponse {
    private String message;
    private BigDecimal amount;
    private LocalDateTime occurredAt;
    private LocalDateTime createdAt;
    private String description;
    private String categoryName;
    private BigDecimal updatedBalance;
}
