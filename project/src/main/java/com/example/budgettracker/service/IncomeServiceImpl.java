package com.example.budgettracker.service;

import com.example.budgettracker.dto.IncomeRequest;
import com.example.budgettracker.dto.IncomeResponse;
import com.example.budgettracker.model.Account;
import com.example.budgettracker.model.CashFlow;
import com.example.budgettracker.model.enums.CashFlowType;
import com.example.budgettracker.model.Category;
import com.example.budgettracker.repository.CategoryRepository;
import com.example.budgettracker.repository.CashFlowRepository;
import com.example.budgettracker.repository.AccountRepository;
import com.example.budgettracker.service.IncomeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class IncomeServiceImpl implements IncomeService {

    private final CashFlowRepository cashFlowRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;

    public IncomeServiceImpl(CashFlowRepository cashFlowRepository,
                             AccountRepository accountRepository,
                             CategoryRepository categoryRepository) {
        this.cashFlowRepository = cashFlowRepository;
        this.accountRepository = accountRepository;
        this.categoryRepository = categoryRepository;
    }

    /**
     * FR-8: Record income transaction
     * 1. Validate inputs (handled by Bean Validation)
     * 2. Verify account belongs to user
     * 3. Save income record
     * 4. Update account balance
     * 5. Return confirmation
     */
    @Override
    @Transactional
    public IncomeResponse recordIncome(Long userId, IncomeRequest request) {
        // Find target account and verify it belongs to the user
        Account account = accountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new RuntimeException("Account not found"));

        // Security check: Ensure account belongs to the authenticated user
        if (!account.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized: Account does not belong to user");
        }

        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new IllegalArgumentException("Category not found"));
            if (!category.getUser().getUserId().equals(userId)) {
                throw new IllegalStateException("Forbidden: Category does not belong to user");
            }
        }

        // Create income transaction
        CashFlow cashFlow = CashFlow.builder()
                .type(CashFlowType.Income)
                .amount(request.getAmount())
                .occurredAt(request.getOccurredAt())
                .description(request.getDescription())
                .account(account)
                .category(category)
                .build();

        // Update account balance
        account.setBalance(account.getBalance().add(request.getAmount()));
        accountRepository.save(account);

        // Save transaction
        CashFlow saved = cashFlowRepository.save(cashFlow);

        // Return success response
        return IncomeResponse.builder()
                .message("Income successfully recorded.")
                .amount(saved.getAmount())
                .occurredAt(saved.getOccurredAt())
                .createdAt(saved.getCreatedAt())
                .description(saved.getDescription())
                .categoryName(category != null ? category.getName() : "Income")
                .updatedBalance(account.getBalance())
                .build();
    }

    @Override
    public List<IncomeResponse> getAllIncome(Long userId) {
        // Get all accounts for this user
        List<Account> userAccounts = accountRepository.findByUser_UserId(userId);

        // Get all income transactions for user's accounts
        List<CashFlow> incomeTransactions = cashFlowRepository.findByTypeOrderByCreatedAtDesc(CashFlowType.Income);

        // Filter to only include transactions for this user's accounts
        return incomeTransactions.stream()
                .filter(cf -> userAccounts.stream()
                        .anyMatch(acc -> acc.getAccountId().equals(cf.getAccount().getAccountId())))
                .map(this::mapToIncomeResponse)
                .collect(Collectors.toList());
    }

    private IncomeResponse mapToIncomeResponse(CashFlow cashFlow) {
        return IncomeResponse.builder()
                .amount(cashFlow.getAmount())
                .occurredAt(cashFlow.getOccurredAt())
                .createdAt(cashFlow.getCreatedAt())
                .description(cashFlow.getDescription())
                .categoryName(cashFlow.getCategory() != null ? cashFlow.getCategory().getName() : "Income")
                .updatedBalance(cashFlow.getAccount().getBalance())
                .build();
    }

    // Removed applyCashFlowToAccount - simplified inline for income only
}
