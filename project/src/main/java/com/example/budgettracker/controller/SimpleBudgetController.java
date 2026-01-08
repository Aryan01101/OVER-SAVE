package com.example.budgettracker.controller;

import com.example.budgettracker.dto.BudgetSummaryResponse;
import com.example.budgettracker.dto.Category.CategoryResponse;
import com.example.budgettracker.service.BudgetService;
import com.example.budgettracker.service.CategoryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

/**
 * FR-11 Budget Setting - Simplified API for frontend
 */
@RestController
@RequestMapping("/api/budget")
public class SimpleBudgetController extends BaseController {

    private final BudgetService budgetService;
    private final CategoryService categoryService;

    public SimpleBudgetController(BudgetService budgetService, CategoryService categoryService) {
        this.budgetService = budgetService;
        this.categoryService = categoryService;
    }

    /**
     * FR-11: Set monthly category budget
     */
    @PostMapping("/set")
    public ResponseEntity<BudgetSummaryResponse> setBudget(@RequestHeader("Authorization") String authHeader,
                                                            @Valid @RequestBody BudgetSetRequest request) {
        Long userId = getUserIdFromToken(authHeader);
        YearMonth yearMonth = YearMonth.now(); // Current month

        BudgetSummaryResponse response = budgetService.setUserCategoryMonthlyBudget(
            userId,
            request.getCategoryId(),
            yearMonth,
            request.getAmount(),
            request.getCustomName()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Get all available categories for budget setting
     */
    @GetMapping("/categories")
    public ResponseEntity<List<CategoryResponse>> getCategories(@RequestHeader("Authorization") String authHeader) {
        Long userId = getUserIdFromToken(authHeader);
        return ResponseEntity.ok(categoryService.list(userId));
    }

    /**
     * Get budget summary for a category
     */
    @GetMapping("/summary/{categoryId}")
    public ResponseEntity<BudgetSummaryResponse> getBudgetSummary(@RequestHeader("Authorization") String authHeader,
                                                                    @PathVariable Long categoryId) {
        Long userId = getUserIdFromToken(authHeader);
        YearMonth yearMonth = YearMonth.now();

        BudgetSummaryResponse summary = budgetService.getUserCategoryBudgetSummary(
            userId,
            categoryId,
            yearMonth
        );

        return ResponseEntity.ok(summary);
    }

    /**
     * Delete budget for a category
     */
    @DeleteMapping("/delete/{categoryId}")
    public ResponseEntity<Map<String, String>> deleteBudget(@RequestHeader("Authorization") String authHeader,
                                                             @PathVariable Long categoryId) {
        Long userId = getUserIdFromToken(authHeader);
        YearMonth yearMonth = YearMonth.now();

        try {
            boolean deleted = budgetService.deleteUserCategoryMonthlyBudget(userId, categoryId, yearMonth);

            if (deleted) {
                return ResponseEntity.ok(Map.of(
                    "message", "Budget deleted successfully",
                    "categoryId", categoryId.toString()
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Budget not found or already deleted"
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to delete budget: " + e.getMessage()
            ));
        }
    }

    /**
     * FR-11 Budget Set Request DTO
     */
    @Data
    public static class BudgetSetRequest {
        @NotNull(message = "Category is required")
        private Long categoryId;

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
        private BigDecimal amount;

        private String customName;
    }
}
