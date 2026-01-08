package com.example.budgettracker.dto.Goal;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
@Getter @Setter
public class GoalRequest {
    private String name;
    private BigDecimal targetAmount;
    private LocalDate dueDate;
}
