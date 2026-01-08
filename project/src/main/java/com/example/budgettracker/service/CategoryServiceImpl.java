package com.example.budgettracker.service;

import com.example.budgettracker.dto.Category.*;
import com.example.budgettracker.model.Category;
import com.example.budgettracker.model.User;
import com.example.budgettracker.model.enums.CashFlowType;
import com.example.budgettracker.repository.CashFlowRepository;
import com.example.budgettracker.repository.CategoryBudgetRepository;
import com.example.budgettracker.repository.CategoryRepository;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;

@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepo;
    private final CashFlowRepository cashFlowRepo;
    private final CategoryBudgetRepository categoryBudgetRepository;
    private final EntityManager entityManager;



    public CategoryServiceImpl(CategoryRepository categoryRepo,
                               CashFlowRepository cashFlowRepo,
                               CategoryBudgetRepository categoryBudgetRepository,
                               EntityManager entityManager) {
        this.categoryRepo = categoryRepo;
        this.cashFlowRepo = cashFlowRepo;
        this.categoryBudgetRepository = categoryBudgetRepository;
        this.entityManager = entityManager;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    @Override
    @Transactional
    public List<CategoryResponse> list(Long userId) {
        List<Category> categories = categoryRepo.findByUser_UserIdOrderByNameAsc(userId);
        categories = ensureSystemCategories(userId, categories);
        return categories.stream().map(this::toResp).toList();
    }

    @Override
    @Transactional
    public CategoryResponse create(Long userId, CategoryRequest req) {
        String norm = normalize(req.getName());
        if (categoryRepo.existsByUser_UserIdAndNameIgnoreCase(userId, norm)) {
            throw new IllegalArgumentException("Category already exists for this user");
        }
        Category saved = categoryRepo.save(Category.builder()
                .user(User.builder().userId(userId).build())
                .name(norm)
                .system(false)
                .build());
        return toResp(saved);
    }

    @Override
    @Transactional
    public CategoryResponse rename(Long userId, Long categoryId, CategoryRequest req) {
        Category c = categoryRepo.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));
        if (!c.getUser().getUserId().equals(userId)) {
            throw new IllegalStateException("Forbidden");
        }
        if (c.isSystem()) throw new IllegalStateException("System category cannot be edited");

        String norm = normalize(req.getName());
        if (categoryRepo.existsByUser_UserIdAndNameIgnoreCaseAndCategoryIdNot(userId, norm, categoryId)) {
            throw new IllegalArgumentException("Category already exists for this user");
        }
        c.setName(norm);
        Category saved = categoryRepo.save(c);
        return toResp(saved);
    }

    @Override
    @Transactional
    public void delete(Long userId, Long categoryId) {
        Category c = categoryRepo.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));

        if (!c.getUser().getUserId().equals(userId)) {
            throw new IllegalStateException("Forbidden: category does not belong to this user");
        }
        if (c.isSystem()) {
            throw new IllegalStateException("System category cannot be deleted");
        }

        int budgetsDeleted = categoryBudgetRepository.deleteByUser_UserIdAndCategory_CategoryId(userId, categoryId);
        if (budgetsDeleted > 0) {
            System.out.println("✓ Deleted " + budgetsDeleted + " budget records for category " + categoryId);
        }

        int cashFlowsDeleted = cashFlowRepo.deleteByUserAndCategory(userId, categoryId);
        if (cashFlowsDeleted > 0) {
            System.out.println("✓ Deleted " + cashFlowsDeleted + " cash flow records for category " + categoryId);
        }

        // Ensure deletions hit the database before removing the category itself
        entityManager.flush();
        entityManager.clear();

        categoryRepo.delete(c);
    }

    @Override
    @Transactional
    public int merge(Long userId, CategoryMergeRequest req, Boolean mergeBudgets) {
        try {
            System.out.println("🔀 Starting category merge - userId: " + userId + ", sources: " + req.getSourceIds() + ", target: " + req.getTargetId());

            Category target = categoryRepo.findById(req.getTargetId())
                    .orElseThrow(() -> new IllegalArgumentException("Target category not found"));
            if (!target.getUser().getUserId().equals(userId)) {
                throw new IllegalStateException("Forbidden: target category not owned by this user");
            }

            List<Long> sources = req.getSourceIds().stream()
                    .filter(id -> !id.equals(req.getTargetId()))
                    .toList();
            if (sources.isEmpty()) {
                System.out.println("⚠️ No valid source categories after filtering");
                return 0;
            }

            System.out.println("✓ Target category validated: " + target.getName());


            List<Long> nonSystemSources = new java.util.ArrayList<>();
            for (Long sid : sources) {
                Category src = categoryRepo.findById(sid)
                        .orElseThrow(() -> new IllegalArgumentException("Source category not found: " + sid));
                if (!src.getUser().getUserId().equals(userId)) {
                    throw new IllegalStateException("Forbidden: source category " + sid + " not owned by this user");
                }
                if (src.isSystem()) {
                    System.out.println("⚠️ Skipping system category: " + src.getName() + " (ID: " + sid + ")");
                    continue;
                }
                nonSystemSources.add(sid);
                System.out.println("✓ Source category validated: " + src.getName() + " (ID: " + sid + ")");
            }


            if (nonSystemSources.isEmpty()) {
                System.out.println("⚠️ No non-system source categories to merge");
                return 0;
            }


            int totalBudgetsAffected = 0;
            for (Long sid : nonSystemSources) {
                if (mergeBudgets != null && mergeBudgets) {

                    System.out.println("📊 Processing budgets for category " + sid);


                    List<com.example.budgettracker.model.CategoryBudget> sourceBudgets =
                        categoryBudgetRepository.findByUser_UserIdAndCategory_CategoryId(userId, sid);

                    int budgetsMerged = 0;
                    int budgetsSummed = 0;

                    for (com.example.budgettracker.model.CategoryBudget sourceBudget : sourceBudgets) {
                        String yearMonth = sourceBudget.getYearMonth();


                        var targetBudgetOpt = categoryBudgetRepository
                            .findByUser_UserIdAndCategory_CategoryIdAndYearMonth(userId, req.getTargetId(), yearMonth);

                        if (targetBudgetOpt.isPresent()) {

                            com.example.budgettracker.model.CategoryBudget targetBudget = targetBudgetOpt.get();
                            BigDecimal originalTargetAmount = targetBudget.getAmount();
                            BigDecimal combinedAmount = originalTargetAmount.add(sourceBudget.getAmount());
                            targetBudget.setAmount(combinedAmount);
                            categoryBudgetRepository.save(targetBudget);


                            categoryBudgetRepository.delete(sourceBudget);

                            budgetsSummed++;
                            System.out.println("  ✓ Summed budgets for " + yearMonth + ": " +
                                sourceBudget.getAmount() + " + " + originalTargetAmount +
                                " = " + combinedAmount);
                        } else {

                            sourceBudget.setCategory(com.example.budgettracker.model.Category.builder()
                                .categoryId(req.getTargetId()).build());
                            categoryBudgetRepository.save(sourceBudget);

                            budgetsMerged++;
                            System.out.println("  ✓ Reassigned budget for " + yearMonth + ": " + sourceBudget.getAmount());
                        }
                    }

                    totalBudgetsAffected += budgetsMerged + budgetsSummed;
                    System.out.println("✓ Category " + sid + ": " + budgetsMerged + " reassigned, " +
                        budgetsSummed + " summed (total: " + sourceBudgets.size() + ")");
                } else {

                    int budgetsDeleted = categoryBudgetRepository.deleteByUser_UserIdAndCategory_CategoryId(userId, sid);
                    totalBudgetsAffected += budgetsDeleted;
                    System.out.println("✓ Deleted " + budgetsDeleted + " budget records from category " + sid);
                }
            }
            if (totalBudgetsAffected > 0) {
                System.out.println("✓ Total budgets affected: " + totalBudgetsAffected);
            }

            // Force flush budget changes to database immediately to preserve ordering
            entityManager.flush();
            entityManager.clear();
            System.out.println("✓ Budget changes flushed to database");


            System.out.println("🔄 Reassigning cash flows from sources to target...");
            int affected = cashFlowRepo.reassignUserCategoryIn(userId, nonSystemSources, req.getTargetId());
            System.out.println("✓ Reassigned " + affected + " cash flow records");


            entityManager.flush();
            entityManager.clear();
            System.out.println("✓ Changes flushed to database");


            System.out.println("🗑️ Deleting source categories...");
            for (Long sid : nonSystemSources) {
                Category src = categoryRepo.findById(sid).orElseThrow();
                categoryRepo.delete(src);
                System.out.println("✓ Deleted category: " + src.getName() + " (ID: " + sid + ")");
            }

            System.out.println("✅ Category merge completed successfully - " + affected + " cash flow records affected, " + totalBudgetsAffected + " budget records affected");
            return affected;

        } catch (IllegalArgumentException | IllegalStateException e) {
            System.err.println("❌ Merge validation failed: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            System.err.println("❌ Unexpected error during category merge: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to merge categories: " + e.getMessage(), e);
        }
    }

    @Override
    public CategorySummaryResponse categorySummary(Long userId, Long categoryId, String month) {

        boolean ok = categoryRepo.existsByCategoryIdAndUser_UserId(categoryId, userId);
        if (!ok) throw new IllegalArgumentException("Category not found for user");


        YearMonth ym = (month == null || month.isBlank()) ? YearMonth.now() : YearMonth.parse(month);
        LocalDate start = ym.atDay(1);
        LocalDate end   = ym.atEndOfMonth();
        LocalDateTime rangeStart = start.atStartOfDay();
        LocalDateTime rangeEnd = end.atTime(LocalTime.MAX);


        Category cat = categoryRepo.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));


        long count = cashFlowRepo.countByUserAndCategoryAndOccurredAtBetween(userId, categoryId, rangeStart, rangeEnd);
        BigDecimal expense = nz(cashFlowRepo.sumByTypeInRange(userId, categoryId, rangeStart, rangeEnd, CashFlowType.Expense));
        BigDecimal income  = nz(cashFlowRepo.sumByTypeInRange(userId, categoryId, rangeStart, rangeEnd, CashFlowType.Income));


        BigDecimal budget = categoryBudgetRepository
                .findByUser_UserIdAndCategory_CategoryIdAndYearMonth(userId, categoryId, ym.toString())
                .map(b -> b.getAmount())
                .orElse(BigDecimal.ZERO);

        BigDecimal remaining = budget.subtract(expense);
        if (remaining.signum() < 0) remaining = BigDecimal.ZERO;

        BigDecimal pct = BigDecimal.ZERO;
        if (budget.signum() > 0) {
            pct = expense.multiply(BigDecimal.valueOf(100))
                    .divide(budget, 2, RoundingMode.HALF_UP);
        }

        CategoryResponse catResp = new CategoryResponse(cat.getCategoryId(), cat.getName(), cat.isSystem());

        return new CategorySummaryResponse(
                catResp,
                ym.toString(),
                count,
                expense,
                income,
                budget,
                remaining,
                pct
        );
    }

    @Override
    public List<CategoryRecordResponse> listCategoryRecords(Long userId, Long categoryId, String month, String type) {

        boolean ok = categoryRepo.existsByCategoryIdAndUser_UserId(categoryId, userId);
        if (!ok) throw new IllegalArgumentException("Category not found for user");

        YearMonth ym = (month == null || month.isBlank()) ? YearMonth.now() : YearMonth.parse(month);
        LocalDate start = ym.atDay(1), end = ym.atEndOfMonth();
        LocalDateTime rangeStart = start.atStartOfDay();
        LocalDateTime rangeEnd = end.atTime(LocalTime.MAX);

        CashFlowType t = null;
        if (type != null && !type.isBlank()) {
            t = CashFlowType.valueOf(type);
        }

        return cashFlowRepo.findByUserCategoryPeriodAndOptionalType(userId, categoryId, rangeStart, rangeEnd, t)
                .stream()
                .map(e -> new CategoryRecordResponse(
                        e.getCashFlowId(),
                        e.getType().name(),
                        e.getAmount(),
                        e.getOccurredAt(),
                        e.getDescription(),
                        e.getAccount() == null ? null : e.getAccount().getAccountId(),
                        e.getCategory() == null ? null : e.getCategory().getCategoryId()
                ))
                .toList();
    }

    @Override
    @Transactional
    public void ensureSystemCategoriesForUser(Long userId) {
        ensureSystemCategories(userId, null);
    }


    /* ---------------- private helpers ---------------- */

    private List<Category> ensureSystemCategories(Long userId, List<Category> existing) {
        List<Category> categories = existing != null
                ? new ArrayList<>(existing)
                : new ArrayList<>(categoryRepo.findByUser_UserIdOrderByNameAsc(userId));

        Set<String> existingNames = new HashSet<>();
        for (Category category : categories) {
            if (category != null && category.getName() != null) {
                existingNames.add(category.getName().trim().toLowerCase());
            }
        }

        List<Category> added = new ArrayList<>();
        for (String defaultName : SystemCategoryDefaults.NAMES) {
            String normalized = defaultName.trim().toLowerCase();
            if (!existingNames.contains(normalized)) {
                Category newCategory = Category.builder()
                        .user(User.builder().userId(userId).build())
                        .name(defaultName)
                        .system(true)
                        .build();
                Category saved = categoryRepo.save(newCategory);
                added.add(saved);
                existingNames.add(normalized);
            }
        }

        if (!added.isEmpty()) {
            entityManager.flush();
            entityManager.clear();
            categories.addAll(added);
            categories.sort(Comparator.comparing(Category::getName, String.CASE_INSENSITIVE_ORDER));
        }

        return categories;
    }

    private CategoryResponse toResp(Category c) {
        return new CategoryResponse(c.getCategoryId(), c.getName(), c.isSystem());
    }

    private String normalize(String name) {
        return name.trim().replaceAll("\\s+", " ");
    }
}
