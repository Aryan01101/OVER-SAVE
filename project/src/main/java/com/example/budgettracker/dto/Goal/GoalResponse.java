package com.example.budgettracker.dto.Goal;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
@Getter
@Setter
public class GoalResponse {
    private Long id;
    private String name;
    private BigDecimal targetAmount;
    private BigDecimal currentAmount;
    private LocalDate dueDate;
    private String status;
    private Double progress;

    // Additional fields for DashboardServiceImpl compatibility
    private LocalDate targetDate;
    private BigDecimal savedAmount;
    private Integer progressPercent;
    private Long accountId;
    private Long categoryId;
}
