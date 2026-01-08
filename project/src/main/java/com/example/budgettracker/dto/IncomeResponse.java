package com.example.budgettracker.dto;

import lombok.Data;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * FR-8 Income Recording Response DTO
 * Returns success confirmation and updated state
 */
@Data
@Builder
public class IncomeResponse {
    private String message;
    private BigDecimal amount;
    private LocalDateTime occurredAt;
    private LocalDateTime createdAt;
    private String description;
    private BigDecimal updatedBalance;
    private String categoryName;
}
