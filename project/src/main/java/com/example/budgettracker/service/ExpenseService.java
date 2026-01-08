package com.example.budgettracker.service;

import com.example.budgettracker.dto.ExpenseRequest;
import com.example.budgettracker.dto.ExpenseResponse;
import java.util.List;

/**
 * FR-7: Expense Recording Service Interface
 */
public interface ExpenseService {
    /**
     * Records an expense transaction
     * Validates inputs, saves expense, updates account balance and reports

     * @param userId authenticated user's ID

     * @param request expense details
     * @return confirmation with updated balance
     */
    ExpenseResponse recordExpense(Long userId, ExpenseRequest request);

    /**
     * Retrieves all expense transactions for a specific user

     * @param userId authenticated user's ID
     * @return list of user's expenses
     */
    List<ExpenseResponse> getAllExpenses(Long userId);
}
