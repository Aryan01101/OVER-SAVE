package com.example.budgettracker.service;

import com.example.budgettracker.dto.Category.*;
import com.example.budgettracker.model.Account;
import com.example.budgettracker.model.CashFlow;
import com.example.budgettracker.model.Category;
import com.example.budgettracker.model.CategoryBudget;
import com.example.budgettracker.model.User;
import com.example.budgettracker.model.enums.CashFlowType;
import com.example.budgettracker.repository.CashFlowRepository;
import com.example.budgettracker.repository.CategoryBudgetRepository;
import com.example.budgettracker.repository.CategoryRepository;
import jakarta.persistence.EntityManager;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mockito.Mockito;

@RunWith(MockitoJUnitRunner.class)
public class CategoryServiceImplTest {
    @Mock
    private CategoryRepository categoryRepo;
    @Mock private CashFlowRepository cashFlowRepo;
    @Mock private CategoryBudgetRepository categoryBudgetRepository;
    @Mock private EntityManager entityManager;

    @InjectMocks
    private CategoryServiceImpl service;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    // ---------- helpers ----------
    private static User user(long userId) {
        User u = new User();
        u.setUserId(userId);
        return u;
    }

    private static Category cat(long id, long userId, String name, boolean system) {
        Category c = new Category();
        c.setCategoryId(id);
        c.setUser(user(userId));
        c.setName(name);
        c.setSystem(system);
        return c;
    }

    private static CashFlow flow(long id, CashFlowType type, BigDecimal amount, LocalDate date, Long acctId, Long catId) {
        CashFlow f = new CashFlow();
        f.setCashFlowId(id);
        f.setType(type);
        f.setAmount(amount);
        f.setOccurredAt(date.atStartOfDay());
        f.setDescription("desc-" + id);
        if (acctId != null) {
            Account a = new Account();
            a.setAccountId(acctId);
            f.setAccount(a);
        }
        if (catId != null) {
            Category c = new Category();
            c.setCategoryId(catId);
            f.setCategory(c);
        }
        return f;
    }

    // ---------- list ----------
    @Test
    public void list_ok() {
        List<Category> existing = new ArrayList<>();
        long id = 1L;
        for (String name : SystemCategoryDefaults.NAMES) {
            existing.add(cat(id++, 1L, name, true));
        }
        existing.add(cat(id, 1L, "Play", false));

        when(categoryRepo.findByUser_UserIdOrderByNameAsc(1L)).thenReturn(existing);

        List<CategoryResponse> out = service.list(1L);
        assertThat(out.size(), is(SystemCategoryDefaults.NAMES.size() + 1));
        assertTrue(out.stream().anyMatch(c -> "Play".equals(c.getName()) && !c.isSystem()));
        verify(categoryRepo, never()).save(any(Category.class));
    }

    @Test
    public void list_missingSystemCategories_seeded() {
        when(categoryRepo.findByUser_UserIdOrderByNameAsc(1L)).thenReturn(new ArrayList<>());
        when(categoryRepo.save(any(Category.class))).thenAnswer(invocation -> {
            Category c = invocation.getArgument(0);
            c.setCategoryId(Math.abs((long) c.getName().hashCode()));
            return c;
        });

        List<CategoryResponse> out = service.list(1L);

        long systemCount = out.stream().filter(CategoryResponse::isSystem).count();
        assertThat(systemCount, is((long) SystemCategoryDefaults.NAMES.size()));

        verify(categoryRepo, times(SystemCategoryDefaults.NAMES.size())).save(any(Category.class));
        verify(entityManager, times(1)).flush();
        verify(entityManager, times(1)).clear();
    }

    // ---------- create ----------
    @Test
    public void create_ok_normalizesAndSaves() {
        CategoryRequest req = new CategoryRequest();
        req.setName("  New    Cat  ");

        when(categoryRepo.existsByUser_UserIdAndNameIgnoreCase(1L, "New Cat"))
                .thenReturn(false);

        ArgumentCaptor<Category> cap = ArgumentCaptor.forClass(Category.class);

        when(categoryRepo.save(any(Category.class)))
                .thenAnswer(inv -> {
                    Category c = inv.getArgument(0);
                    c.setCategoryId(99L);
                    return c;
                });

        CategoryResponse resp = service.create(1L, req);

        verify(categoryRepo).save(cap.capture());
        Category saved = cap.getValue();
        assertThat(saved.getName(), is("New Cat"));
        assertFalse(saved.isSystem());
        assertThat(saved.getUser().getUserId(), is(1L));

        assertThat(resp.getId(), is(99L));
        assertThat(resp.getName(), is("New Cat"));
    }

    @Test
    public void create_dupName_throws() {
        when(categoryRepo.existsByUser_UserIdAndNameIgnoreCase(1L, "Food")).thenReturn(true);

        CategoryRequest req = new CategoryRequest();
        req.setName("Food");

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Category already exists for this user");
        service.create(1L, req);
    }

    // ---------- rename ----------
    @Test
    public void rename_ok() {
        Category existing = cat(10L, 1L, "Old", false);
        when(categoryRepo.findById(10L)).thenReturn(Optional.of(existing));
        when(categoryRepo.existsByUser_UserIdAndNameIgnoreCaseAndCategoryIdNot(1L, "New Name", 10L))
                .thenReturn(false);
        when(categoryRepo.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        CategoryRequest req = new CategoryRequest();
        req.setName(" New   Name ");

        CategoryResponse out = service.rename(1L, 10L, req);
        assertThat(out.getName(), is("New Name"));
        verify(categoryRepo).save(Mockito.argThat(c -> c.getName().equals("New Name")));
    }
    @Test
    public void rename_notFound_throws() {
        when(categoryRepo.findById(10L)).thenReturn(Optional.empty());

        CategoryRequest req = new CategoryRequest();
        req.setName("X");

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Category not found");
        service.rename(1L, 10L, req);
    }

    @Test
    public void rename_forbidden_throws() {
        when(categoryRepo.findById(10L)).thenReturn(Optional.of(cat(10L, 2L, "X", false)));

        CategoryRequest req = new CategoryRequest();
        req.setName("Y");

        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("Forbidden");
        service.rename(1L, 10L, req);
    }

    @Test
    public void rename_system_throws() {
        when(categoryRepo.findById(10L)).thenReturn(Optional.of(cat(10L, 1L, "X", true)));

        CategoryRequest req = new CategoryRequest();
        req.setName("Y");

        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("System category cannot be edited");
        service.rename(1L, 10L, req);
    }

    @Test
    public void rename_dupName_throws() {
        when(categoryRepo.findById(10L)).thenReturn(Optional.of(cat(10L, 1L, "X", false)));
        when(categoryRepo.existsByUser_UserIdAndNameIgnoreCaseAndCategoryIdNot(1L, "X2", 10L)).thenReturn(true);

        CategoryRequest req = new CategoryRequest();
        req.setName("X2");

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Category already exists for this user");
        service.rename(1L, 10L, req);
    }

    // ---------- delete ----------
    @Test
    public void delete_ok_deletesBudgetsCashFlowsAndCategory() {
        Category own = cat(10L, 1L, "X", false);
        when(categoryRepo.findById(10L)).thenReturn(Optional.of(own));
        when(categoryBudgetRepository.deleteByUser_UserIdAndCategory_CategoryId(1L, 10L)).thenReturn(2);
        when(cashFlowRepo.deleteByUserAndCategory(1L, 10L)).thenReturn(5);

        service.delete(1L, 10L);

        verify(categoryBudgetRepository).deleteByUser_UserIdAndCategory_CategoryId(1L, 10L);
        verify(cashFlowRepo).deleteByUserAndCategory(1L, 10L);
        verify(entityManager).flush();
        verify(entityManager).clear();
        verify(categoryRepo).delete(own);
    }

    @Test
    public void delete_notFound_throws() {
        when(categoryRepo.findById(10L)).thenReturn(Optional.empty());
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Category not found");
        service.delete(1L, 10L);
    }

    @Test
    public void delete_forbidden_throws() {
        when(categoryRepo.findById(10L)).thenReturn(Optional.of(cat(10L, 2L, "X", false)));

        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("Forbidden: category does not belong to this user");
        service.delete(1L, 10L);
    }

    @Test
    public void delete_system_throws() {
        when(categoryRepo.findById(10L)).thenReturn(Optional.of(cat(10L, 1L, "X", true)));

        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("System category cannot be deleted");
        service.delete(1L, 10L);
    }

    // ---------- merge ----------
    @Test
    public void merge_ok_filtersTargetAndDeletesNonSystem() {
        Category target = cat(100L, 1L, "Target", false);
        when(categoryRepo.findById(100L)).thenReturn(Optional.of(target));

        Category s1 = cat(101L, 1L, "A", false);
        Category s2 = cat(102L, 1L, "B", true); // 系统类目保留
        when(categoryRepo.findById(101L)).thenReturn(Optional.of(s1));
        when(categoryRepo.findById(102L)).thenReturn(Optional.of(s2));

        when(categoryBudgetRepository.deleteByUser_UserIdAndCategory_CategoryId(eq(1L), eq(101L))).thenReturn(0);
        when(cashFlowRepo.reassignUserCategoryIn(eq(1L), Mockito.anyList(), eq(100L)))
                .thenReturn(7);

        CategoryMergeRequest req = new CategoryMergeRequest();
        req.setTargetId(100L);
        req.setSourceIds(Arrays.asList(100L, 101L, 102L));

        int affected = service.merge(1L, req, null);

        assertThat(affected, is(7));
        verify(categoryRepo).delete(s1);
        verify(categoryRepo, never()).delete(s2);
    }

    @Test
    public void merge_targetNotFound_throws() {
        when(categoryRepo.findById(100L)).thenReturn(Optional.empty());

        CategoryMergeRequest req = new CategoryMergeRequest();
        req.setTargetId(100L);
        req.setSourceIds(Arrays.asList(101L));

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Target category not found");
        service.merge(1L, req, null);
    }

    @Test
    public void merge_targetForbidden_throws() {
        when(categoryRepo.findById(100L)).thenReturn(Optional.of(cat(100L, 2L, "T", false)));

        CategoryMergeRequest req = new CategoryMergeRequest();
        req.setTargetId(100L);
        req.setSourceIds(Arrays.asList(101L));

        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("Forbidden: target category not owned by this user");
        service.merge(1L, req, null);
    }

    @Test
    public void merge_sourcesEmptyAfterFilter_returnsZero() {
        when(categoryRepo.findById(100L)).thenReturn(Optional.of(cat(100L, 1L, "T", false)));

        CategoryMergeRequest req = new CategoryMergeRequest();
        req.setTargetId(100L);
        req.setSourceIds(Arrays.asList(100L)); // 只包含 targetId -> 过滤后为空

        int out = service.merge(1L, req, null);
        assertThat(out, is(0));
        verify(cashFlowRepo, never()).reassignUserExpenseCategoryIn(Mockito.anyLong(), Mockito.anyList(), Mockito.anyLong());
    }

    @Test
    public void merge_sourceNotFound_throws() {
        when(categoryRepo.findById(100L)).thenReturn(Optional.of(cat(100L, 1L, "T", false)));
        when(categoryRepo.findById(101L)).thenReturn(Optional.empty());

        CategoryMergeRequest req = new CategoryMergeRequest();
        req.setTargetId(100L);
        req.setSourceIds(Arrays.asList(101L));

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Source category not found: 101");
        service.merge(1L, req, null);
    }

    @Test
    public void merge_sourceForbidden_throws() {
        when(categoryRepo.findById(100L)).thenReturn(Optional.of(cat(100L, 1L, "T", false)));
        when(categoryRepo.findById(101L)).thenReturn(Optional.of(cat(101L, 2L, "A", false)));

        CategoryMergeRequest req = new CategoryMergeRequest();
        req.setTargetId(100L);
        req.setSourceIds(Arrays.asList(101L));

        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("Forbidden: source category 101 not owned by this user");
        service.merge(1L, req, null);
    }

    // ---------- categorySummary ----------
    @Test
    public void categorySummary_notOwned_throws() {
        when(categoryRepo.existsByCategoryIdAndUser_UserId(10L, 1L)).thenReturn(false);
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Category not found for user");
        service.categorySummary(1L, 10L, null);
    }


    @Test
    public void categorySummary_ok_withBudget_pctAndRemaining() {
        when(categoryRepo.existsByCategoryIdAndUser_UserId(10L, 1L)).thenReturn(true);
        when(categoryRepo.findById(10L)).thenReturn(Optional.of(cat(10L, 1L, "Food", false)));

        YearMonth ym = YearMonth.now();
        LocalDate s = ym.atDay(1), e = ym.atEndOfMonth();

        when(cashFlowRepo.countByUserAndCategoryAndOccurredAtBetween(1L, 10L, s.atStartOfDay(), e.atTime(LocalTime.MAX)))
                .thenReturn(5L);
        when(cashFlowRepo.sumByTypeInRange(1L, 10L, s.atStartOfDay(), e.atTime(LocalTime.MAX), CashFlowType.Expense))
                .thenReturn(new BigDecimal("150.55"));
        when(cashFlowRepo.sumByTypeInRange(1L, 10L, s.atStartOfDay(), e.atTime(LocalTime.MAX), CashFlowType.Income))
                .thenReturn(BigDecimal.ZERO);

        CategoryBudget cb = new CategoryBudget();
        cb.setAmount(new BigDecimal("200.00"));
        when(categoryBudgetRepository.findByUser_UserIdAndCategory_CategoryIdAndYearMonth(1L, 10L, ym.toString()))
                .thenReturn(Optional.of(cb));

        CategorySummaryResponse out = service.categorySummary(1L, 10L, ym.toString());
        assertThat(out.yearMonth(), is(ym.toString()));
        assertThat(out.recordCount(), is(5L));
        assertThat(out.totalExpense(), is(new BigDecimal("150.55")));
        assertThat(out.totalIncome(), is(BigDecimal.ZERO));
        assertThat(out.budgetAmount(), is(new BigDecimal("200.00")));
        assertThat(out.remainingBudget(), is(new BigDecimal("49.45")));
        assertThat(out.expenseVsBudgetPct(), is(new BigDecimal("75.28")));
    }

    @Test
    public void categorySummary_ok_noBudget_negativeRemainingClamped() {
        when(categoryRepo.existsByCategoryIdAndUser_UserId(10L, 1L)).thenReturn(true);
        when(categoryRepo.findById(10L)).thenReturn(Optional.of(cat(10L, 1L, "Food", false)));

        YearMonth ym = YearMonth.now();
        LocalDate s = ym.atDay(1), e = ym.atEndOfMonth();

        when(cashFlowRepo.countByUserAndCategoryAndOccurredAtBetween(1L, 10L, s.atStartOfDay(), e.atTime(LocalTime.MAX)))
                .thenReturn(3L);
        when(cashFlowRepo.sumByTypeInRange(1L, 10L, s.atStartOfDay(), e.atTime(LocalTime.MAX), CashFlowType.Expense))
                .thenReturn(new BigDecimal("20.00"));
        when(cashFlowRepo.sumByTypeInRange(1L, 10L, s.atStartOfDay(), e.atTime(LocalTime.MAX), CashFlowType.Income))
                .thenReturn(null); // nz -> ZERO

        when(categoryBudgetRepository.findByUser_UserIdAndCategory_CategoryIdAndYearMonth(1L, 10L, ym.toString()))
                .thenReturn(Optional.empty());

        CategorySummaryResponse out = service.categorySummary(1L, 10L, ym.toString());
        assertThat(out.budgetAmount(), is(BigDecimal.ZERO));
        assertThat(out.remainingBudget(), is(BigDecimal.ZERO)); // 0 - 20 => clamp 0
        assertThat(out.expenseVsBudgetPct(), is(BigDecimal.ZERO));
    }

    @Test
    public void categorySummary_ok_monthNull_usesCurrentMonth() {
        // given 属于用户
        when(categoryRepo.existsByCategoryIdAndUser_UserId(10L, 1L)).thenReturn(true);
        when(categoryRepo.findById(10L)).thenReturn(Optional.of(cat(10L, 1L, "Food", false)));

        when(cashFlowRepo.countByUserAndCategoryAndOccurredAtBetween(eq(1L), eq(10L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(1L);
        when(cashFlowRepo.sumByTypeInRange(eq(1L), eq(10L), any(LocalDateTime.class), any(LocalDateTime.class), eq(CashFlowType.Expense)))
                .thenReturn(new BigDecimal("10"));
        when(cashFlowRepo.sumByTypeInRange(eq(1L), eq(10L), any(LocalDateTime.class), any(LocalDateTime.class), eq(CashFlowType.Income)))
                .thenReturn(null);
        when(categoryBudgetRepository.findByUser_UserIdAndCategory_CategoryIdAndYearMonth(eq(1L), eq(10L), anyString()))
                .thenReturn(Optional.empty());

        // when
        CategorySummaryResponse out = service.categorySummary(1L, 10L, null);

        assertEquals(YearMonth.now().toString(), out.yearMonth());
        assertEquals(new BigDecimal("10"), out.totalExpense());
        assertEquals(BigDecimal.ZERO, out.totalIncome());
        assertEquals(BigDecimal.ZERO, out.budgetAmount());
        assertEquals(BigDecimal.ZERO, out.remainingBudget());
        assertEquals(BigDecimal.ZERO, out.expenseVsBudgetPct());
    }

    @Test
    public void categorySummary_existsTrueButEntityMissing_throws() {
        when(categoryRepo.existsByCategoryIdAndUser_UserId(10L, 1L)).thenReturn(true);
        when(categoryRepo.findById(10L)).thenReturn(Optional.empty());

        try {
            service.categorySummary(1L, 10L, YearMonth.now().toString());
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            assertThat(ex.getMessage(), is("Category not found"));
        }
    }

    // ---------- listCategoryRecords ----------
    @Test
    public void listCategoryRecords_notOwned_throws() {
        when(categoryRepo.existsByCategoryIdAndUser_UserId(10L, 1L)).thenReturn(false);
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Category not found for user");
        service.listCategoryRecords(1L, 10L, null, null);
    }

    @Test
    public void listCategoryRecords_ok_allTypes() {
        when(categoryRepo.existsByCategoryIdAndUser_UserId(10L, 1L)).thenReturn(true);

        YearMonth ym = YearMonth.now();
        LocalDate s = ym.atDay(1), e = ym.atEndOfMonth();

        List<CashFlow> rows = Arrays.asList(
                flow(1, CashFlowType.Expense, new BigDecimal("10.00"), s.plusDays(1), 100L, 10L),
                flow(2, CashFlowType.Income, new BigDecimal("50.00"), s.plusDays(2), 100L, null)
        );
        when(cashFlowRepo.findByUserCategoryPeriodAndOptionalType(1L, 10L, s.atStartOfDay(), e.atTime(LocalTime.MAX), null)).thenReturn(rows);

        List<CategoryRecordResponse> out = service.listCategoryRecords(1L, 10L, ym.toString(), null);
        assertThat(out.size(), is(2));
        assertThat(out.get(0).id(), is(1L));
        assertThat(out.get(0).type(), is("Expense"));
        assertThat(out.get(0).accountId(), is(100L));
        assertThat(out.get(0).categoryId(), is(10L));

        assertThat(out.get(1).type(), is("Income"));
        assertThat(out.get(1).accountId(), is(100L));
        assertThat(out.get(1).categoryId(), is(nullValue()));
    }

    @Test
    public void listCategoryRecords_ok_filterExpense() {
        when(categoryRepo.existsByCategoryIdAndUser_UserId(10L, 1L)).thenReturn(true);

        YearMonth ym = YearMonth.now();
        LocalDate s = ym.atDay(1), e = ym.atEndOfMonth();

        List<CashFlow> rows = Collections.singletonList(
                flow(3, CashFlowType.Expense, new BigDecimal("12.34"), s.plusDays(3), 201L, 10L)
        );
        when(cashFlowRepo.findByUserCategoryPeriodAndOptionalType(1L, 10L, s.atStartOfDay(), e.atTime(LocalTime.MAX), CashFlowType.Expense)).thenReturn(rows);

        List<CategoryRecordResponse> out = service.listCategoryRecords(1L, 10L, ym.toString(), "Expense");
        assertThat(out.size(), is(1));
        assertThat(out.get(0).type(), is("Expense"));
        assertThat(out.get(0).amount(), is(new BigDecimal("12.34")));
    }

    @Test
    public void listCategoryRecords_ok_monthNull_allTypes() {
        when(categoryRepo.existsByCategoryIdAndUser_UserId(10L, 1L)).thenReturn(true);

        when(cashFlowRepo.findByUserCategoryPeriodAndOptionalType(
                eq(1L), eq(10L), any(LocalDateTime.class), any(LocalDateTime.class), isNull()))
                .thenReturn(Collections.emptyList());

        List<CategoryRecordResponse> out = service.listCategoryRecords(1L, 10L, null, null);
        assertTrue(out.isEmpty());

        verify(cashFlowRepo).findByUserCategoryPeriodAndOptionalType(
                eq(1L), eq(10L), any(LocalDateTime.class), any(LocalDateTime.class), isNull());
    }

    @Test
    public void listCategoryRecords_typeInvalid_throws() {
        when(categoryRepo.existsByCategoryIdAndUser_UserId(10L, 1L)).thenReturn(true);

        try {
            service.listCategoryRecords(1L, 10L, YearMonth.now().toString(), "INVALID-TYPE");
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
        }
    }

}
