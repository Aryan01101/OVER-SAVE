package com.example.budgettracker.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SubscriptionRequest {

    @NotBlank @Size(max = 60)
    private String merchant;

    @NotNull @DecimalMin("0.00")
    private BigDecimal amount;

    // e.g. WEEKLY, FORTNIGHTLY, MONTHLY, QUARTERLY, YEARLY
    @NotBlank @Size(max = 20)
    private String frequency;

    @NotNull
    private LocalDateTime startDate;

    // Optional
    private LocalDateTime firstPostAt;

    private Boolean isActive;

}
