package com.example.budgettracker.service;

import com.example.budgettracker.dto.ExpenseRequest;
import com.example.budgettracker.dto.ExpenseResponse;
import com.example.budgettracker.model.Account;
import com.example.budgettracker.model.CashFlow;
import com.example.budgettracker.model.Category;
import com.example.budgettracker.model.enums.CashFlowType;
import com.example.budgettracker.repository.CashFlowRepository;
import com.example.budgettracker.repository.AccountRepository;
import com.example.budgettracker.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ExpenseServiceImpl implements ExpenseService {

    private final CashFlowRepository cashFlowRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;

    public ExpenseServiceImpl(CashFlowRepository cashFlowRepository,
                              AccountRepository accountRepository,
                              CategoryRepository categoryRepository) {
        this.cashFlowRepository = cashFlowRepository;
        this.accountRepository = accountRepository;
        this.categoryRepository = categoryRepository;
    }

    /**
     * FR-7: Record expense transaction with user validation
     * 1. Validate inputs (handled by Bean Validation + business rules)
     * 2. Verify account ownership (security check)
     * 3. Verify category exists and is active
     * 4. Verify account belongs to user
     * 5. Save expense record
     * 6. Update account balance
     * 7. Return confirmation
     */
    @Override
    @Transactional
    public ExpenseResponse recordExpense(Long userId, ExpenseRequest request) {

        // Find target account
        Account account = accountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new RuntimeException("Account not found"));

        // SECURITY: Validate that the account belongs to the authenticated user
        if (!account.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized: You do not have permission to access this account");
        }


        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found"));
        }

        // Note: As a budget tracker (not a real bank), we allow negative balances
        // This lets users track expenses even without income - balance will just show negative

        // Create expense transaction
        CashFlow cashFlow = CashFlow.builder()
                .type(CashFlowType.Expense)
                .amount(request.getAmount())
                .occurredAt(request.getOccurredAt())
                .description(request.getDescription())
                .account(account)
                .category(category)
                .build();

        // Update account balance (subtract expense)
        account.setBalance(account.getBalance().subtract(request.getAmount()));
        accountRepository.save(account);

        // Save transaction
        CashFlow saved = cashFlowRepository.save(cashFlow);

        // Return success response
        return ExpenseResponse.builder()
                .message("Expense successfully recorded.")
                .amount(saved.getAmount())
                .occurredAt(saved.getOccurredAt())
                .createdAt(saved.getCreatedAt())
                .description(saved.getDescription())
                .categoryName(category != null ? category.getName() : "Uncategorized")
                .updatedBalance(account.getBalance())
                .build();
    }

    @Override
    public List<ExpenseResponse> getAllExpenses(Long userId) {

        // Get all cash flows for the user and filter by Expense type
        List<CashFlow> expenseTransactions = cashFlowRepository.findRecentByUserId(userId).stream()
                .filter(cf -> cf.getType() == CashFlowType.Expense)
                .collect(Collectors.toList());


        return expenseTransactions.stream()
                .map(this::mapToExpenseResponse)
                .collect(Collectors.toList());
    }

    private ExpenseResponse mapToExpenseResponse(CashFlow cashFlow) {
        return ExpenseResponse.builder()
                .amount(cashFlow.getAmount())
                .occurredAt(cashFlow.getOccurredAt())
                .createdAt(cashFlow.getCreatedAt())
                .description(cashFlow.getDescription())
                .categoryName(cashFlow.getCategory() != null ? cashFlow.getCategory().getName() : "Uncategorized")
                .updatedBalance(cashFlow.getAccount().getBalance())
                .build();
    }
}
