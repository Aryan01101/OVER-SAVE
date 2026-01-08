package com.example.budgettracker.repository;

import com.example.budgettracker.model.Category;
import com.example.budgettracker.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByUser_UserIdOrderByNameAsc(Long userId);

    boolean existsByUser_UserIdAndNameIgnoreCase(Long userId, String name);

    boolean existsByUser_UserIdAndNameIgnoreCaseAndCategoryIdNot(Long userId, String name, Long excludeId);

    Optional<Category> findByUser_UserIdAndNameIgnoreCase(Long userId, String name);

    boolean existsByCategoryIdAndUser_UserId(Long categoryId, Long userId);

}

