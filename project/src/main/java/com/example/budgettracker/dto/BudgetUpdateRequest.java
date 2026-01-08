package com.example.budgettracker.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class BudgetUpdateRequest {

    @Pattern(regexp = "^\\d{4}-\\d{2}$", message = "yearMonth must be YYYY-MM")
    private String yearMonth;

    @NotNull(message = "budget is required")
    @DecimalMin(value = "0.01", message = "budget must be > 0")
    @Digits(integer = 12, fraction = 2)
    private BigDecimal budget;

}
