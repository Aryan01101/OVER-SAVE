package com.example.budgettracker.service;

// src/main/java/com/example/service/IncomeService.java

import com.example.budgettracker.dto.IncomeRequest;
import com.example.budgettracker.dto.IncomeResponse;
import java.util.List;

public interface IncomeService {
    IncomeResponse recordIncome(Long userId, IncomeRequest request);
    List<IncomeResponse> getAllIncome(Long userId);
}