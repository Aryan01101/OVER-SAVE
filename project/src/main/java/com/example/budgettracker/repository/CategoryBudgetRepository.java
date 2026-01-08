package com.example.budgettracker.repository;

import com.example.budgettracker.model.CategoryBudget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CategoryBudgetRepository extends JpaRepository<CategoryBudget, Long> {
    Optional<CategoryBudget> findByUser_UserIdAndCategory_CategoryIdAndYearMonth(
            Long userId, Long categoryId, String yearMonth
    );

    // Find all budgets for a specific category
    List<CategoryBudget> findByUser_UserIdAndCategory_CategoryId(Long userId, Long categoryId);

    // Delete all budgets for a specific category
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from CategoryBudget cb where cb.user.userId = :userId and cb.category.categoryId = :categoryId")
    int deleteByUser_UserIdAndCategory_CategoryId(@Param("userId") Long userId, @Param("categoryId") Long categoryId);

    // Reassign budgets from one category to another (for merge option)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update CategoryBudget cb set cb.category.categoryId = :toId where cb.user.userId = :userId and cb.category.categoryId = :fromId")
    int reassignCategoryBudgets(@Param("userId") Long userId, @Param("fromId") Long fromId, @Param("toId") Long toId);
}
