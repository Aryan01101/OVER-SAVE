package com.example.budgettracker.dto.Category;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CategoryRecordResponse(
        Long id,
        String type,          // "Income" / "Expense"
        BigDecimal amount,
        LocalDateTime occurredAt,
        String description,
        Long accountId,
        Long categoryId
) {}
