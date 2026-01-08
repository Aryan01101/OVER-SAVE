package com.example.budgettracker.controller;

import com.example.budgettracker.dto.Category.*;
import com.example.budgettracker.model.CategoryBudget;
import com.example.budgettracker.repository.CategoryBudgetRepository;
import com.example.budgettracker.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController extends BaseController {

    private final CategoryService service;
    private final CategoryBudgetRepository categoryBudgetRepository;

    public CategoryController(CategoryService service, CategoryBudgetRepository categoryBudgetRepository) {
        this.service = service;
        this.categoryBudgetRepository = categoryBudgetRepository;
    }

    @GetMapping
    public List<CategoryResponse> list(@RequestHeader("Authorization") String authHeader) {
        Long userId = getUserIdFromToken(authHeader);
        return service.list(userId);
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> create(@RequestHeader("Authorization") String authHeader,
                                                   @Valid @RequestBody CategoryRequest req) {
        Long userId = getUserIdFromToken(authHeader);
        return ResponseEntity.ok(service.create(userId, req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> rename(@RequestHeader("Authorization") String authHeader,
                                                   @PathVariable Long id,
                                                   @Valid @RequestBody CategoryRequest req) {
        Long userId = getUserIdFromToken(authHeader);
        return ResponseEntity.ok(service.rename(userId, id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@RequestHeader("Authorization") String authHeader,
                                    @PathVariable Long id) {
        Long userId = getUserIdFromToken(authHeader);
        service.delete(userId, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/merge")
    public ResponseEntity<?> merge(@RequestHeader("Authorization") String authHeader,
                                   @Valid @RequestBody CategoryMergeRequest req,
                                   @RequestParam(required = false) Boolean mergeBudgets) {
        Long userId = getUserIdFromToken(authHeader);
        int affected = service.merge(userId, req, mergeBudgets);
        return ResponseEntity.ok().body(new Object(){ public final int reassigned = affected; });
    }

    @GetMapping("/{id}/summary")
    public CategorySummaryResponse categorySummary(@RequestHeader("Authorization") String authHeader,
                                                   @PathVariable Long id,
                                                   @RequestParam(required = false) String month) {
        Long userId = getUserIdFromToken(authHeader);
        return service.categorySummary(userId, id, month);
    }

    @GetMapping("/{id}/records")
    public List<CategoryRecordResponse> listCategoryRecords(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id,
            @RequestParam(required = false) String month,
            @RequestParam(required = false) String type // Can be Income / Expense
    ) {
        Long userId = getUserIdFromToken(authHeader);
        return service.listCategoryRecords(userId, id, month, type);
    }

    @GetMapping("/{id}/budgets")
    public List<CategoryBudgetInfo> listBudgets(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
        Long userId = getUserIdFromToken(authHeader);
        return categoryBudgetRepository
                .findByUser_UserIdAndCategory_CategoryId(userId, id)
                .stream()
                .map(cb -> new CategoryBudgetInfo(cb.getYearMonth(), cb.getAmount(), cb.getCustomName()))
                .toList();
    }

    // Simple DTO for budget information
    public record CategoryBudgetInfo(String yearMonth, BigDecimal amount, String customName) {}
}
