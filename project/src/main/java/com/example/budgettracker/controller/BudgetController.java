package com.example.budgettracker.controller;

import com.example.budgettracker.dto.BudgetSummaryResponse;
import com.example.budgettracker.dto.BudgetUpdateRequest;
import com.example.budgettracker.service.BudgetService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.YearMonth;

@RestController
@RequestMapping("/api/categories/{categoryId}/budget")
public class BudgetController extends BaseController {

    private final BudgetService budgetService;
    public BudgetController(BudgetService budgetService) { this.budgetService = budgetService; }

    @PutMapping
    public BudgetSummaryResponse setBudget(@RequestHeader("Authorization") String authHeader,
                                           @PathVariable Long categoryId,
                                           @Valid @RequestBody BudgetUpdateRequest request) {
        Long userId = getUserIdFromToken(authHeader);
        YearMonth ym = (request.getYearMonth() == null) ? null : YearMonth.parse(request.getYearMonth());
        BigDecimal budget = request.getBudget();
        return budgetService.setUserCategoryMonthlyBudget(userId, categoryId, ym, budget);
    }

    @GetMapping("/summary")
    public BudgetSummaryResponse getSummary(@RequestHeader("Authorization") String authHeader,
                                            @PathVariable Long categoryId,
                                            @RequestParam(value = "yearMonth", required = false)
                                            @DateTimeFormat(pattern = "yyyy-MM") YearMonth yearMonth) {
        Long userId = getUserIdFromToken(authHeader);
        return budgetService.getUserCategoryBudgetSummary(userId, categoryId, yearMonth);
    }
}
