package com.example.budgettracker.service;

import com.example.budgettracker.dto.BudgetSummaryResponse;
import com.example.budgettracker.model.Category;
import com.example.budgettracker.model.CategoryBudget;
import com.example.budgettracker.model.User;
import com.example.budgettracker.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;

@Service
public class BudgetServiceImpl implements BudgetService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final CategoryBudgetRepository categoryBudgetRepository;
    private final CashFlowRepository cashFlowRepository;

    public BudgetServiceImpl(CategoryRepository categoryRepository, UserRepository userRepository, CategoryBudgetRepository categoryBudgetRepository, CashFlowRepository cashFlowRepository) {
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.categoryBudgetRepository = categoryBudgetRepository;
        this.cashFlowRepository = cashFlowRepository;
    }

    @Override
    @Transactional
    public BudgetSummaryResponse setUserCategoryMonthlyBudget(Long userId, Long categoryId, YearMonth ym, BigDecimal budget) {
        return setUserCategoryMonthlyBudget(userId, categoryId, ym, budget, null);
    }

    @Override
    @Transactional
    public BudgetSummaryResponse setUserCategoryMonthlyBudget(Long userId, Long categoryId, YearMonth ym, BigDecimal budget, String customName) {
        if (budget == null || budget.signum() <= 0) {
            throw new IllegalArgumentException("Budget must be > 0");
        }
        YearMonth targetYm = (ym == null) ? YearMonth.now() : ym;

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("Category not found: " + categoryId));


        CategoryBudget cb = categoryBudgetRepository
                .findByUser_UserIdAndCategory_CategoryIdAndYearMonth(userId, categoryId, targetYm.toString())
                .orElseGet(() -> {
                    CategoryBudget x = new CategoryBudget();
                    x.setUser(user);
                    x.setCategory(category);
                    x.setYearMonth(targetYm.toString());
                    return x;
                });
        cb.setAmount(budget);
        cb.setCustomName(customName);
        categoryBudgetRepository.save(cb);

        return buildSummary(userId, categoryId, targetYm, cb.getAmount(), cb.getCustomName());
    }

    @Override
    @Transactional(readOnly = true)
    public BudgetSummaryResponse getUserCategoryBudgetSummary(Long userId, Long categoryId, YearMonth ym) {
        YearMonth targetYm = (ym == null) ? YearMonth.now() : ym;

        categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("Category not found: " + categoryId));

        CategoryBudget categoryBudget = categoryBudgetRepository
                .findByUser_UserIdAndCategory_CategoryIdAndYearMonth(userId, categoryId, targetYm.toString())
                .orElse(null);

        BigDecimal budget = categoryBudget != null ? categoryBudget.getAmount() : BigDecimal.ZERO;
        String customName = categoryBudget != null ? categoryBudget.getCustomName() : null;

        return buildSummary(userId, categoryId, targetYm, budget, customName);
    }

    @Override
    @Transactional
    public boolean deleteUserCategoryMonthlyBudget(Long userId, Long categoryId, YearMonth ym) {
        YearMonth targetYm = (ym == null) ? YearMonth.now() : ym;

        // Check if user and category exist
        userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));

        categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("Category not found: " + categoryId));

        // Find and delete the budget
        return categoryBudgetRepository
                .findByUser_UserIdAndCategory_CategoryIdAndYearMonth(userId, categoryId, targetYm.toString())
                .map(categoryBudget -> {
                    categoryBudgetRepository.delete(categoryBudget);
                    return true;
                })
                .orElse(false); // Budget doesn't exist, nothing to delete
    }

    private BudgetSummaryResponse buildSummary(Long userId, Long categoryId, YearMonth ym, BigDecimal budget) {
        return buildSummary(userId, categoryId, ym, budget, null);
    }

    private BudgetSummaryResponse buildSummary(Long userId, Long categoryId, YearMonth ym, BigDecimal budget, String customName) {
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        LocalDateTime rangeStart = start.atStartOfDay();
        LocalDateTime rangeEnd = end.atTime(LocalTime.MAX);
        BigDecimal spent = cashFlowRepository
                .sumUserExpenseByCategoryAndPeriod(userId, categoryId, rangeStart, rangeEnd);
        return new BudgetSummaryResponse(categoryId, userId, ym.toString(), budget, spent, customName);
    }
}
