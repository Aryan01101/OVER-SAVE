package com.example.budgettracker.controller;

import com.example.budgettracker.dto.ExpenseRequest;
import com.example.budgettracker.dto.ExpenseResponse;
import com.example.budgettracker.service.ExpenseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * FR-7: Expense Recording Controller
 * Handles HTTP requests for expense transactions
 */
@RestController
@RequestMapping("/api/expenses")
public class ExpenseController extends BaseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    /**
     * POST /api/expenses
     * Records a new expense transaction
     * @param authHeader Authorization header with Bearer token
     * @param request expense details (validated)
     * @return expense confirmation
     */
    @PostMapping
    public ResponseEntity<ExpenseResponse> addExpense(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody ExpenseRequest request) {
        try {
            Long userId = getUserIdFromToken(authHeader);
            ExpenseResponse response = expenseService.recordExpense(userId, request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            // Authentication/authorization errors
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                ExpenseResponse.builder()
                    .message("Error: " + e.getMessage())
                    .build()
            );
        } catch (RuntimeException e) {
            // Handle business logic errors (insufficient balance, category not found, etc.)
            return ResponseEntity.badRequest().body(
                ExpenseResponse.builder()
                    .message("Error: " + e.getMessage())
                    .build()
            );
        }
    }

    /**
     * GET /api/expenses
     * Retrieves all expense transactions for the authenticated user
     * @param authHeader Authorization header with Bearer token

     * @return list of user's expenses
     */
    @GetMapping
    public ResponseEntity<List<ExpenseResponse>> getAllExpenses(
            @RequestHeader("Authorization") String authHeader) {
        try {
            Long userId = getUserIdFromToken(authHeader);
            List<ExpenseResponse> expenseList = expenseService.getAllExpenses(userId);
            return ResponseEntity.ok(expenseList);
        } catch (IllegalArgumentException e) {
          return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
       } catch (RuntimeException e) {
          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
       }
    }
}
