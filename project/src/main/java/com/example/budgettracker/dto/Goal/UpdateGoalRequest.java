package com.example.budgettracker.dto.Goal;

import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter
public class UpdateGoalRequest {
    private String name;

    @Positive(message = "Target amount must be positive")
    private BigDecimal targetAmount;

    private LocalDate dueDate;
}
