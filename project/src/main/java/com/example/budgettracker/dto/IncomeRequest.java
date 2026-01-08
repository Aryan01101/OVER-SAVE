package com.example.budgettracker.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * FR-8 Income Recording Request DTO
 * Fields: amount (>0), date (valid), source (description)
 */
@Data
public class IncomeRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @NotNull(message = "Date is required")
    private LocalDateTime occurredAt;

    @NotBlank(message = "Source description is required")
    private String description;

    // Account ID for balance update (required for MVP)
    @NotNull(message = "Account is required")
    private Long accountId;

    // Optional category assignment
    private Long categoryId;
}
