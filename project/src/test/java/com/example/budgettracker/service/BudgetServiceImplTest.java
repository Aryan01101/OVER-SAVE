package com.example.budgettracker.service;

import com.example.budgettracker.dto.BudgetSummaryResponse;
import com.example.budgettracker.model.Category;
import com.example.budgettracker.model.CategoryBudget;
import com.example.budgettracker.model.User;
import com.example.budgettracker.repository.CashFlowRepository;
import com.example.budgettracker.repository.CategoryBudgetRepository;
import com.example.budgettracker.repository.CategoryRepository;
import com.example.budgettracker.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(org.mockito.junit.MockitoJUnitRunner.class)
public class BudgetServiceImplTest {

    @Mock private CategoryRepository categoryRepository;
    @Mock private UserRepository userRepository;
    @Mock private CategoryBudgetRepository categoryBudgetRepository;
    @Mock private CashFlowRepository cashFlowRepository;

    @InjectMocks
    private BudgetServiceImpl service;

    private final Long userId = 99L;
    private final Long categoryId = 7L;
    private User user;
    private Category category;

    @Before
    public void setUp() {
        user = new User();
        category = new Category();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
    }

    /* -------------------- setUserCategoryMonthlyBudget -------------------- */

    @Test
    public void setUserCategoryMonthlyBudget_createNew_whenNoExistingRecord() {
        YearMonth ym = YearMonth.of(2025, 9);
        BigDecimal budget = new BigDecimal("1000.00");

        when(categoryBudgetRepository.findByUser_UserIdAndCategory_CategoryIdAndYearMonth(userId, categoryId, ym.toString()))
                .thenReturn(Optional.empty());
        when(cashFlowRepository.sumUserExpenseByCategoryAndPeriod(eq(userId), eq(categoryId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(new BigDecimal("123.45"));

        ArgumentCaptor<CategoryBudget> cbCaptor = ArgumentCaptor.forClass(CategoryBudget.class);

        BudgetSummaryResponse resp = service.setUserCategoryMonthlyBudget(userId, categoryId, ym, budget);

        verify(categoryBudgetRepository).save(cbCaptor.capture());
        CategoryBudget saved = cbCaptor.getValue();
        assertThat(saved.getAmount(), is(budget));
        assertThat(saved.getYearMonth(), is(ym.toString()));
        assertSame(user, saved.getUser());
        assertSame(category, saved.getCategory());

        assertThat(resp.getUserId(), is(userId));
        assertThat(resp.getCategoryId(), is(categoryId));
        assertThat(resp.getYearMonth(), is(ym.toString()));
        assertThat(resp.getBudget(), is(budget));
        assertThat(resp.getSpent(), is(new BigDecimal("123.45")));
    }

    @Test
    public void setUserCategoryMonthlyBudget_updateExisting_whenRecordPresent() {
        YearMonth ym = YearMonth.of(2025, 10);
        BigDecimal newBudget = new BigDecimal("200.00");

        CategoryBudget existing = new CategoryBudget();
        existing.setUser(user);
        existing.setCategory(category);
        existing.setYearMonth(ym.toString());
        existing.setAmount(new BigDecimal("50"));

        when(categoryBudgetRepository.findByUser_UserIdAndCategory_CategoryIdAndYearMonth(userId, categoryId, ym.toString()))
                .thenReturn(Optional.of(existing));
        when(cashFlowRepository.sumUserExpenseByCategoryAndPeriod(eq(userId), eq(categoryId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(BigDecimal.ZERO);

        ArgumentCaptor<CategoryBudget> cbCaptor = ArgumentCaptor.forClass(CategoryBudget.class);

        BudgetSummaryResponse resp = service.setUserCategoryMonthlyBudget(userId, categoryId, ym, newBudget);

        verify(categoryBudgetRepository).save(cbCaptor.capture());
        CategoryBudget saved = cbCaptor.getValue();
        assertSame(existing, saved);
        assertThat(saved.getAmount(), is(newBudget));
        assertThat(resp.getBudget(), is(newBudget));
    }

    @Test(expected = IllegalArgumentException.class)
    public void setUserCategoryMonthlyBudget_throws_whenBudgetNull() {
        service.setUserCategoryMonthlyBudget(userId, categoryId, YearMonth.of(2025, 9), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void setUserCategoryMonthlyBudget_throws_whenBudgetZero() {
        service.setUserCategoryMonthlyBudget(userId, categoryId, YearMonth.of(2025, 9), BigDecimal.ZERO);
    }

    @Test(expected = IllegalArgumentException.class)
    public void setUserCategoryMonthlyBudget_throws_whenBudgetNegative() {
        service.setUserCategoryMonthlyBudget(userId, categoryId, YearMonth.of(2025, 9), new BigDecimal("-1"));
    }

    @Test(expected = EntityNotFoundException.class)
    public void setUserCategoryMonthlyBudget_throws_whenUserNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        service.setUserCategoryMonthlyBudget(userId, categoryId, YearMonth.of(2025, 9), new BigDecimal("10"));
    }

    @Test(expected = EntityNotFoundException.class)
    public void setUserCategoryMonthlyBudget_throws_whenCategoryNotFound() {
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());
        service.setUserCategoryMonthlyBudget(userId, categoryId, YearMonth.of(2025, 9), new BigDecimal("10"));
    }

    @Test
    public void setUserCategoryMonthlyBudget_ymNull_usesNowMonth() {
        String nowYm = YearMonth.now().toString();

        when(categoryBudgetRepository.findByUser_UserIdAndCategory_CategoryIdAndYearMonth(eq(userId), eq(categoryId), eq(nowYm)))
                .thenReturn(Optional.empty());
        when(cashFlowRepository.sumUserExpenseByCategoryAndPeriod(eq(userId), eq(categoryId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(BigDecimal.TEN);

        BudgetSummaryResponse resp = service.setUserCategoryMonthlyBudget(userId, categoryId, null, new BigDecimal("5"));

        assertThat(resp.getYearMonth(), is(nowYm));
        assertThat(resp.getBudget(), is(new BigDecimal("5")));
        assertThat(resp.getSpent(), is(BigDecimal.TEN));
    }

    /* -------------------- getUserCategoryBudgetSummary （with customName） -------------------- */

    @Test
    public void getUserCategoryBudgetSummary_withExistingBudget_andCustomName() {
        YearMonth ym = YearMonth.of(2025, 8);
        CategoryBudget cb = new CategoryBudget();
        cb.setAmount(new BigDecimal("333.33"));
        // 假设 CategoryBudget 有 setCustomName；若字段名不同请替换
        try {
            CategoryBudget.class.getMethod("setCustomName", String.class).invoke(cb, "Groceries-Q3");
        } catch (Exception ignore) {}

        when(categoryBudgetRepository.findByUser_UserIdAndCategory_CategoryIdAndYearMonth(userId, categoryId, ym.toString()))
                .thenReturn(Optional.of(cb));
        when(cashFlowRepository.sumUserExpenseByCategoryAndPeriod(eq(userId), eq(categoryId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(new BigDecimal("111.11"));

        BudgetSummaryResponse resp = service.getUserCategoryBudgetSummary(userId, categoryId, ym);

        assertThat(resp.getBudget(), is(new BigDecimal("333.33")));
        assertThat(resp.getSpent(), is(new BigDecimal("111.11")));
        assertThat(resp.getYearMonth(), is(ym.toString()));
        assertThat(resp.getCustomName(), is("Groceries-Q3"));
    }

    @Test
    public void getUserCategoryBudgetSummary_withExistingBudget_customNameNull() {
        YearMonth ym = YearMonth.of(2025, 8);
        CategoryBudget cb = new CategoryBudget();
        cb.setAmount(new BigDecimal("88.00"));

        when(categoryBudgetRepository.findByUser_UserIdAndCategory_CategoryIdAndYearMonth(userId, categoryId, ym.toString()))
                .thenReturn(Optional.of(cb));
        when(cashFlowRepository.sumUserExpenseByCategoryAndPeriod(eq(userId), eq(categoryId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(new BigDecimal("10.00"));

        BudgetSummaryResponse resp = service.getUserCategoryBudgetSummary(userId, categoryId, ym);

        assertThat(resp.getBudget(), is(new BigDecimal("88.00")));
        assertThat(resp.getCustomName(), is(nullValue()));
    }

    @Test
    public void getUserCategoryBudgetSummary_withoutBudget_returnsZeroBudget() {
        YearMonth ym = YearMonth.of(2025, 7);

        when(categoryBudgetRepository.findByUser_UserIdAndCategory_CategoryIdAndYearMonth(userId, categoryId, ym.toString()))
                .thenReturn(Optional.empty());
        when(cashFlowRepository.sumUserExpenseByCategoryAndPeriod(eq(userId), eq(categoryId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(BigDecimal.ZERO);

        BudgetSummaryResponse resp = service.getUserCategoryBudgetSummary(userId, categoryId, ym);

        assertThat(resp.getBudget(), is(BigDecimal.ZERO));
        assertThat(resp.getSpent(), is(BigDecimal.ZERO));
        assertThat(resp.getCustomName(), is(nullValue()));
    }

    @Test(expected = EntityNotFoundException.class)
    public void getUserCategoryBudgetSummary_throws_whenCategoryNotFound() {
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());
        service.getUserCategoryBudgetSummary(userId, categoryId, YearMonth.of(2025, 5));
    }

    @Test
    public void getUserCategoryBudgetSummary_passesCorrectStartEndDatesToRepository() {
        YearMonth ym = YearMonth.of(2025, 2);

        when(categoryBudgetRepository.findByUser_UserIdAndCategory_CategoryIdAndYearMonth(userId, categoryId, ym.toString()))
                .thenReturn(Optional.empty());

        ArgumentCaptor<LocalDateTime> startCap = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> endCap = ArgumentCaptor.forClass(LocalDateTime.class);

        when(cashFlowRepository.sumUserExpenseByCategoryAndPeriod(eq(userId), eq(categoryId), startCap.capture(), endCap.capture()))
                .thenReturn(new BigDecimal("42"));

        service.getUserCategoryBudgetSummary(userId, categoryId, ym);

        assertThat(startCap.getValue().toLocalDate(), is(LocalDate.of(2025, 2, 1)));
        assertThat(startCap.getValue().toLocalTime(), is(LocalTime.MIDNIGHT));
        assertThat(endCap.getValue().toLocalDate(), is(LocalDate.of(2025, 2, 28))); // 2025-02
        assertThat(endCap.getValue().toLocalTime(), is(LocalTime.MAX));
    }

    /* -------------------- deleteUserCategoryMonthlyBudget -------------------- */

    @Test
    public void deleteUserCategoryMonthlyBudget_whenBudgetExists_deletesAndReturnsTrue() {
        YearMonth ym = YearMonth.of(2025, 9);
        CategoryBudget cb = new CategoryBudget();
        cb.setUser(user);
        cb.setCategory(category);
        cb.setYearMonth(ym.toString());
        cb.setAmount(new BigDecimal("10"));

        when(categoryBudgetRepository.findByUser_UserIdAndCategory_CategoryIdAndYearMonth(userId, categoryId, ym.toString()))
                .thenReturn(Optional.of(cb));

        boolean result = service.deleteUserCategoryMonthlyBudget(userId, categoryId, ym);

        assertTrue(result);
        verify(categoryBudgetRepository, times(1)).delete(cb);
    }

    @Test
    public void deleteUserCategoryMonthlyBudget_whenBudgetNotExists_returnsFalseAndNoDelete() {
        YearMonth ym = YearMonth.of(2025, 9);

        when(categoryBudgetRepository.findByUser_UserIdAndCategory_CategoryIdAndYearMonth(userId, categoryId, ym.toString()))
                .thenReturn(Optional.empty());

        boolean result = service.deleteUserCategoryMonthlyBudget(userId, categoryId, ym);

        assertFalse(result);
        verify(categoryBudgetRepository, never()).delete(any(CategoryBudget.class));
    }

    @Test(expected = EntityNotFoundException.class)
    public void deleteUserCategoryMonthlyBudget_throws_whenUserNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        service.deleteUserCategoryMonthlyBudget(userId, categoryId, YearMonth.of(2025, 9));
    }

    @Test(expected = EntityNotFoundException.class)
    public void deleteUserCategoryMonthlyBudget_throws_whenCategoryNotFound() {
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());
        service.deleteUserCategoryMonthlyBudget(userId, categoryId, YearMonth.of(2025, 9));
    }

    @Test
    public void deleteUserCategoryMonthlyBudget_ymNull_usesNowMonthInQuery() {
        String nowYm = YearMonth.now().toString();

        when(categoryBudgetRepository.findByUser_UserIdAndCategory_CategoryIdAndYearMonth(eq(userId), eq(categoryId), eq(nowYm)))
                .thenReturn(Optional.empty());

        boolean result = service.deleteUserCategoryMonthlyBudget(userId, categoryId, null);

        assertFalse(result);
        // 验证确实按当月字符串调用过查询
        verify(categoryBudgetRepository).findByUser_UserIdAndCategory_CategoryIdAndYearMonth(eq(userId), eq(categoryId), eq(nowYm));
    }
}
