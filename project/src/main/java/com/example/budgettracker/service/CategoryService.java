package com.example.budgettracker.service;

import com.example.budgettracker.dto.Category.*;

import java.util.List;

public interface CategoryService {

    List<CategoryResponse> list(Long userId);

    CategoryResponse create(Long userId, CategoryRequest req);

    CategoryResponse rename(Long userId, Long categoryId, CategoryRequest req);

    void delete(Long userId, Long categoryId);

    int merge(Long userId, CategoryMergeRequest req, Boolean mergeBudgets);

    void ensureSystemCategoriesForUser(Long userId);

    CategorySummaryResponse categorySummary(Long userId, Long categoryId, String month);

    List<CategoryRecordResponse> listCategoryRecords(Long userId, Long categoryId, String month, String type);

}
