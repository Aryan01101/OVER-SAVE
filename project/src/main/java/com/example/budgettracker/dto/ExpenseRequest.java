package com.example.budgettracker.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * FR-7 Expense Recording Request DTO
 * Fields: amount (>0), date (valid), category (exists), description
 */
@Data
public class ExpenseRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @NotNull(message = "Date is required")
    private LocalDateTime occurredAt;

    @NotBlank(message = "Description is required")
    private String description;

    private Long categoryId;

    // Account ID for balance update
    @NotNull(message = "Account is required")
    private Long accountId;
}
