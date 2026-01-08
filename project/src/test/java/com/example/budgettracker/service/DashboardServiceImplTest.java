package com.example.budgettracker.service;

import com.example.budgettracker.dto.BudgetSummaryResponse;
import com.example.budgettracker.dto.Dashboard.*;
import com.example.budgettracker.dto.Goal.GoalResponse;
import com.example.budgettracker.model.*;
import com.example.budgettracker.model.enums.CashFlowType;
import com.example.budgettracker.model.enums.GoalStatus;
import com.example.budgettracker.repository.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DashboardServiceImplTest {

    @Mock
    private CashFlowRepository cashFlowRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private GoalRepository goalRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private BudgetService budgetService;

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    // ========== Helper Methods ==========

    private User createUser(Long userId) {
        User user = new User();
        user.setUserId(userId);
        user.setEmail("user" + userId + "@test.com");
        user.setFirstName("Test");
        user.setLastName("User" + userId);
        return user;
    }

    private Category createCategory(Long categoryId, Long userId, String name) {
        Category category = new Category();
        category.setCategoryId(categoryId);
        category.setUser(createUser(userId));
        category.setName(name);
        return category;
    }

    private CashFlow createCashFlow(Long id, CashFlowType type, BigDecimal amount, LocalDateTime occurredAt, Category category) {
        CashFlow cashFlow = new CashFlow();
        cashFlow.setCashFlowId(id);
        cashFlow.setType(type);
        cashFlow.setAmount(amount);
        cashFlow.setOccurredAt(occurredAt);
        cashFlow.setDescription("Test " + type);
        cashFlow.setCategory(category);
        cashFlow.setCreatedAt(LocalDateTime.now());
        return cashFlow;
    }

    private Subscription createSubscription(Long id, String merchant, BigDecimal amount, String frequency, boolean active) {
        Subscription subscription = new Subscription();
        subscription.setSubscriptionId(id);
        subscription.setMerchant(merchant);
        subscription.setAmount(amount);
        subscription.setFrequency(frequency);
        subscription.setIsActive(active);
        subscription.setNextPostAt(LocalDate.now().plusDays(7).atStartOfDay());
        return subscription;
    }

    private Goal createGoal(Long id, String name, BigDecimal targetAmount, BigDecimal currentAmount, GoalStatus status, LocalDate dueDate) {
        Goal goal = new Goal();
        goal.setId(id);
        goal.setName(name);
        goal.setTargetAmount(targetAmount);
        goal.setCurrentAmount(currentAmount);
        goal.setStatus(status);
        goal.setDueDate(dueDate);
        goal.setUser(createUser(1L));
        return goal;
    }

    // ========== getDashboardData Tests ==========

    @Test
    public void getDashboardData_ok_withAllData() {
        // Given
        Long userId = 1L;

        when(cashFlowRepository.sumAllIncomeByUser(userId)).thenReturn(new BigDecimal("5000"));
        when(cashFlowRepository.sumAllExpenseByUser(userId)).thenReturn(new BigDecimal("3000"));
        when(cashFlowRepository.computeTotalBalanceByUserId(userId)).thenReturn(new BigDecimal("2000"));
        when(goalRepository.findAllByUser_UserIdOrderByIdDesc(userId)).thenReturn(Collections.emptyList());

        when(categoryRepository.findByUser_UserIdOrderByNameAsc(userId)).thenReturn(Collections.emptyList());
        when(subscriptionRepository.findByUser_UserIdAndIsActiveTrue(userId)).thenReturn(Collections.emptyList());
        when(cashFlowRepository.findRecentByUserId(userId)).thenReturn(Collections.emptyList());
        when(cashFlowRepository.findByUserIdAndDateRange(anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        // When
        DashboardResponse response = dashboardService.getDashboardData(userId);

        // Then
        assertNotNull(response);
        assertTrue(response.isDataAvailable());
        assertNull(response.getMessage());
        assertNotNull(response.getFinancialAggregates());
        assertNotNull(response.getBudgets());
        assertNotNull(response.getSubscriptions());
        assertNotNull(response.getRecentTransactions());
        assertNotNull(response.getSpendingTrend());
        assertNotNull(response.getSavingsGoals());

        verify(cashFlowRepository, times(1)).sumAllIncomeByUser(userId);
        verify(cashFlowRepository, times(1)).sumAllExpenseByUser(userId);
        verify(cashFlowRepository, times(1)).computeTotalBalanceByUserId(userId);
    }

    @Test
    public void getDashboardData_ok_handleException_returnsFallback() {
        // Given
        Long userId = 1L;
        when(cashFlowRepository.sumAllIncomeByUser(userId)).thenThrow(new RuntimeException("Database error"));

        // When
        DashboardResponse response = dashboardService.getDashboardData(userId);

        // Then
        assertNotNull(response);
        assertFalse(response.isDataAvailable());
        assertEquals("Unable to load dashboard data. Please try again later.", response.getMessage());
        assertNull(response.getFinancialAggregates());
        assertNull(response.getBudgets());
    }

    // ========== getFinancialAggregates Tests ==========

    @Test
    public void getFinancialAggregates_ok_calculatesCorrectly() {
        // Given
        Long userId = 1L;
        BigDecimal totalIncome = new BigDecimal("10000");
        BigDecimal totalExpenses = new BigDecimal("7000");
        BigDecimal currentBalance = new BigDecimal("3000");

        when(cashFlowRepository.sumAllIncomeByUser(userId)).thenReturn(totalIncome);
        when(cashFlowRepository.sumAllExpenseByUser(userId)).thenReturn(totalExpenses);
        when(cashFlowRepository.computeTotalBalanceByUserId(userId)).thenReturn(currentBalance);
        when(goalRepository.findAllByUser_UserIdOrderByIdDesc(userId)).thenReturn(Collections.emptyList());

        // When
        FinancialAggregatesResponse response = dashboardService.getFinancialAggregates(userId);

        // Then
        assertNotNull(response);
        assertEquals(totalIncome, response.getMonthlyIncome());
        assertEquals(totalExpenses, response.getMonthlyExpenses());
        assertEquals(currentBalance, response.getCurrentBalance());
        assertEquals(BigDecimal.ZERO, response.getTotalSavings());
        // validateNumericValues() converts nulls to 0.0
        assertEquals(0.0, response.getIncomeChangePercent(), 0.01);
        assertEquals(0.0, response.getExpenseChangePercent(), 0.01);
        assertEquals(0.0, response.getGoalsProgressPercent(), 0.01);

        // Savings rate = (10000 - 7000) / 10000 * 100 = 30%
        assertThat(response.getSavingsRate(), is(closeTo(30.0, 0.01)));
    }

    @Test
    public void getFinancialAggregates_ok_zeroIncome_savingsRateIsZero() {
        // Given
        Long userId = 1L;
        when(cashFlowRepository.sumAllIncomeByUser(userId)).thenReturn(BigDecimal.ZERO);
        when(cashFlowRepository.sumAllExpenseByUser(userId)).thenReturn(new BigDecimal("100"));
        when(cashFlowRepository.computeTotalBalanceByUserId(userId)).thenReturn(new BigDecimal("-100"));
        when(goalRepository.findAllByUser_UserIdOrderByIdDesc(userId)).thenReturn(Collections.emptyList());

        // When
        FinancialAggregatesResponse response = dashboardService.getFinancialAggregates(userId);

        // Then
        assertEquals(0.0, response.getSavingsRate(), 0.001);
    }

    @Test
    public void getFinancialAggregates_ok_withGoals_calculatesCorrectly() {
        // Given
        Long userId = 1L;
        when(cashFlowRepository.sumAllIncomeByUser(userId)).thenReturn(new BigDecimal("5000"));
        when(cashFlowRepository.sumAllExpenseByUser(userId)).thenReturn(new BigDecimal("3000"));
        when(cashFlowRepository.computeTotalBalanceByUserId(userId)).thenReturn(new BigDecimal("2000"));

        List<Goal> goals = Arrays.asList(
                createGoal(1L, "Goal 1", new BigDecimal("1000"), new BigDecimal("500"), GoalStatus.IN_PROGRESS, LocalDate.now().plusMonths(6)),
                createGoal(2L, "Goal 2", new BigDecimal("2000"), new BigDecimal("1000"), GoalStatus.IN_PROGRESS, LocalDate.now().plusMonths(12))
        );
        when(goalRepository.findAllByUser_UserIdOrderByIdDesc(userId)).thenReturn(goals);

        // When
        FinancialAggregatesResponse response = dashboardService.getFinancialAggregates(userId);

        // Then
        assertEquals(new BigDecimal("1500"), response.getTotalSavings()); // 500 + 1000
        // Average progress = ((500/1000 * 100) + (1000/2000 * 100)) / 2 = (50 + 50) / 2 = 50%
        assertThat(response.getGoalsProgressPercent(), is(closeTo(50.0, 0.01)));
    }

    @Test
    public void getFinancialAggregates_ok_goalsWithZeroTarget_handledCorrectly() {
        // Given
        Long userId = 1L;
        when(cashFlowRepository.sumAllIncomeByUser(userId)).thenReturn(new BigDecimal("1000"));
        when(cashFlowRepository.sumAllExpenseByUser(userId)).thenReturn(new BigDecimal("500"));
        when(cashFlowRepository.computeTotalBalanceByUserId(userId)).thenReturn(new BigDecimal("500"));

        List<Goal> goals = Arrays.asList(
                createGoal(1L, "Goal 1", BigDecimal.ZERO, new BigDecimal("100"), GoalStatus.IN_PROGRESS, LocalDate.now().plusMonths(6)),
                createGoal(2L, "Goal 2", null, new BigDecimal("200"), GoalStatus.IN_PROGRESS, LocalDate.now().plusMonths(12))
        );
        when(goalRepository.findAllByUser_UserIdOrderByIdDesc(userId)).thenReturn(goals);

        // When
        FinancialAggregatesResponse response = dashboardService.getFinancialAggregates(userId);

        // Then
        assertEquals(new BigDecimal("300"), response.getTotalSavings());
        assertEquals(0.0, response.getGoalsProgressPercent(), 0.01); // Both goals have 0% progress
    }

    // ========== getSpendingTrend Tests ==========

    @Test
    public void getSpendingTrend_week_ok_generatesSevenDays() {
        // Given
        Long userId = 1L;
        LocalDate now = LocalDate.now();
        LocalDate startDate = now.minusDays(6);

        List<CashFlow> cashFlows = Arrays.asList(
                createCashFlow(1L, CashFlowType.Expense, new BigDecimal("50"), now.minusDays(2).atStartOfDay(), null),
                createCashFlow(2L, CashFlowType.Expense, new BigDecimal("100"), now.atStartOfDay(), null)
        );

        when(cashFlowRepository.findByUserIdAndDateRange(eq(userId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(cashFlows);

        // When
        SpendingTrendData response = dashboardService.getSpendingTrend(userId, "WEEK");

        // Then
        assertNotNull(response);
        assertEquals("WEEK", response.getPeriod());
        assertEquals(7, response.getDataPoints().size());

        // Check that one of the data points is marked as today
        boolean hasToday = response.getDataPoints().stream().anyMatch(SpendingTrendData.DataPoint::isToday);
        assertTrue(hasToday);
    }

    @Test
    public void getSpendingTrend_month_ok_generatesCurrentMonth() {
        // Given
        Long userId = 1L;
        LocalDate now = LocalDate.now();

        when(cashFlowRepository.findByUserIdAndDateRange(eq(userId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        // When
        SpendingTrendData response = dashboardService.getSpendingTrend(userId, "MONTH");

        // Then
        assertNotNull(response);
        assertEquals("MONTH", response.getPeriod());
        assertTrue(response.getDataPoints().size() >= now.getDayOfMonth());
    }

    @Test
    public void getSpendingTrend_year_ok_generatesMonths() {
        // Given
        Long userId = 1L;

        when(cashFlowRepository.findByUserIdAndDateRange(eq(userId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        // When
        SpendingTrendData response = dashboardService.getSpendingTrend(userId, "YEAR");

        // Then
        assertNotNull(response);
        assertEquals("YEAR", response.getPeriod());
        assertTrue(response.getDataPoints().size() <= 12);
        assertTrue(response.getDataPoints().size() >= YearMonth.now().getMonthValue());
    }

    @Test
    public void getSpendingTrend_invalidPeriod_defaultsToWeek() {
        // Given
        Long userId = 1L;

        when(cashFlowRepository.findByUserIdAndDateRange(eq(userId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        // When
        SpendingTrendData response = dashboardService.getSpendingTrend(userId, "INVALID");

        // Then
        assertNotNull(response);
        assertEquals("INVALID", response.getPeriod());
        assertEquals(7, response.getDataPoints().size());
    }

    @Test
    public void getSpendingTrend_week_ok_onlyIncludesExpenses() {
        // Given
        Long userId = 1L;
        LocalDate now = LocalDate.now();

        List<CashFlow> cashFlows = Arrays.asList(
                createCashFlow(1L, CashFlowType.Expense, new BigDecimal("50"), now.atStartOfDay(), null),
                createCashFlow(2L, CashFlowType.Income, new BigDecimal("1000"), now.atStartOfDay(), null)
        );

        when(cashFlowRepository.findByUserIdAndDateRange(eq(userId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(cashFlows);

        // When
        SpendingTrendData response = dashboardService.getSpendingTrend(userId, "WEEK");

        // Then
        SpendingTrendData.DataPoint todayPoint = response.getDataPoints().stream()
                .filter(SpendingTrendData.DataPoint::isToday)
                .findFirst()
                .orElse(null);

        assertNotNull(todayPoint);
        assertEquals(new BigDecimal("50"), todayPoint.getAmount()); // Only expense is included
    }

    // ========== Private Helper Method Tests (via getDashboardData) ==========

    @Test
    public void getBudgetSummaries_ok_returnsTop5Budgets() {
        // Given
        Long userId = 1L;
        YearMonth currentMonth = YearMonth.now();

        List<Category> categories = Arrays.asList(
                createCategory(1L, userId, "Food"),
                createCategory(2L, userId, "Transport"),
                createCategory(3L, userId, "Entertainment"),
                createCategory(4L, userId, "Shopping"),
                createCategory(5L, userId, "Utilities"),
                createCategory(6L, userId, "Healthcare")
        );

        when(categoryRepository.findByUser_UserIdOrderByNameAsc(userId)).thenReturn(categories);

        BudgetSummaryResponse budgetResponse = new BudgetSummaryResponse(
                1L, userId, currentMonth.toString(), new BigDecimal("500"), new BigDecimal("300")
        );

        when(budgetService.getUserCategoryBudgetSummary(eq(userId), anyLong(), eq(currentMonth)))
                .thenReturn(budgetResponse);

        when(cashFlowRepository.sumAllIncomeByUser(userId)).thenReturn(new BigDecimal("1000"));
        when(cashFlowRepository.sumAllExpenseByUser(userId)).thenReturn(new BigDecimal("500"));
        when(cashFlowRepository.computeTotalBalanceByUserId(userId)).thenReturn(new BigDecimal("500"));
        when(goalRepository.findAllByUser_UserIdOrderByIdDesc(userId)).thenReturn(Collections.emptyList());
        when(subscriptionRepository.findByUser_UserIdAndIsActiveTrue(userId)).thenReturn(Collections.emptyList());
        when(cashFlowRepository.findRecentByUserId(userId)).thenReturn(Collections.emptyList());
        when(cashFlowRepository.findByUserIdAndDateRange(anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        // When
        DashboardResponse response = dashboardService.getDashboardData(userId);

        // Then
        assertNotNull(response.getBudgets());
        assertTrue(response.getBudgets().size() <= 5);
    }

    @Test
    public void getActiveSubscriptions_ok_returnsTop5ActiveSubscriptions() {
        // Given
        Long userId = 1L;

        List<Subscription> subscriptions = Arrays.asList(
                createSubscription(1L, "Netflix", new BigDecimal("15.99"), "MONTHLY", true),
                createSubscription(2L, "Spotify", new BigDecimal("9.99"), "MONTHLY", true),
                createSubscription(3L, "Gym", new BigDecimal("50"), "MONTHLY", true),
                createSubscription(4L, "Amazon Prime", new BigDecimal("12.99"), "MONTHLY", true),
                createSubscription(5L, "NYT", new BigDecimal("17.00"), "MONTHLY", true),
                createSubscription(6L, "Hulu", new BigDecimal("7.99"), "MONTHLY", true)
        );

        when(subscriptionRepository.findByUser_UserIdAndIsActiveTrue(userId)).thenReturn(subscriptions);
        when(categoryRepository.findByUser_UserIdOrderByNameAsc(userId)).thenReturn(Collections.emptyList());
        when(cashFlowRepository.sumAllIncomeByUser(userId)).thenReturn(new BigDecimal("1000"));
        when(cashFlowRepository.sumAllExpenseByUser(userId)).thenReturn(new BigDecimal("500"));
        when(cashFlowRepository.computeTotalBalanceByUserId(userId)).thenReturn(new BigDecimal("500"));
        when(goalRepository.findAllByUser_UserIdOrderByIdDesc(userId)).thenReturn(Collections.emptyList());
        when(cashFlowRepository.findRecentByUserId(userId)).thenReturn(Collections.emptyList());
        when(cashFlowRepository.findByUserIdAndDateRange(anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        // When
        DashboardResponse response = dashboardService.getDashboardData(userId);

        // Then
        assertNotNull(response.getSubscriptions());
        assertEquals(5, response.getSubscriptions().size());
        assertTrue(response.getSubscriptions().stream().allMatch(SubscriptionSummaryResponse::isActive));
    }

    @Test
    public void getRecentTransactions_ok_returnsTop10Transactions() {
        // Given
        Long userId = 1L;
        Category foodCategory = createCategory(1L, userId, "Food");

        List<CashFlow> cashFlows = new ArrayList<>();
        for (int i = 1; i <= 15; i++) {
            cashFlows.add(createCashFlow((long) i, CashFlowType.Expense, new BigDecimal("10"), LocalDateTime.now().minusDays(i), foodCategory));
        }

        when(cashFlowRepository.findRecentByUserId(userId)).thenReturn(cashFlows);
        when(categoryRepository.findByUser_UserIdOrderByNameAsc(userId)).thenReturn(Collections.emptyList());
        when(cashFlowRepository.sumAllIncomeByUser(userId)).thenReturn(new BigDecimal("1000"));
        when(cashFlowRepository.sumAllExpenseByUser(userId)).thenReturn(new BigDecimal("500"));
        when(cashFlowRepository.computeTotalBalanceByUserId(userId)).thenReturn(new BigDecimal("500"));
        when(goalRepository.findAllByUser_UserIdOrderByIdDesc(userId)).thenReturn(Collections.emptyList());
        when(subscriptionRepository.findByUser_UserIdAndIsActiveTrue(userId)).thenReturn(Collections.emptyList());
        when(cashFlowRepository.findByUserIdAndDateRange(anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        // When
        DashboardResponse response = dashboardService.getDashboardData(userId);

        // Then
        assertNotNull(response.getRecentTransactions());
        assertEquals(10, response.getRecentTransactions().size());
        assertEquals("Food", response.getRecentTransactions().get(0).getCategoryName());
    }

    @Test
    public void getRecentTransactions_ok_uncategorizedTransaction() {
        // Given
        Long userId = 1L;

        List<CashFlow> cashFlows = Arrays.asList(
                createCashFlow(1L, CashFlowType.Expense, new BigDecimal("50"), LocalDateTime.now(), null)
        );

        when(cashFlowRepository.findRecentByUserId(userId)).thenReturn(cashFlows);
        when(categoryRepository.findByUser_UserIdOrderByNameAsc(userId)).thenReturn(Collections.emptyList());
        when(cashFlowRepository.sumAllIncomeByUser(userId)).thenReturn(new BigDecimal("1000"));
        when(cashFlowRepository.sumAllExpenseByUser(userId)).thenReturn(new BigDecimal("500"));
        when(cashFlowRepository.computeTotalBalanceByUserId(userId)).thenReturn(new BigDecimal("500"));
        when(goalRepository.findAllByUser_UserIdOrderByIdDesc(userId)).thenReturn(Collections.emptyList());
        when(subscriptionRepository.findByUser_UserIdAndIsActiveTrue(userId)).thenReturn(Collections.emptyList());
        when(cashFlowRepository.findByUserIdAndDateRange(anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        // When
        DashboardResponse response = dashboardService.getDashboardData(userId);

        // Then
        assertNotNull(response.getRecentTransactions());
        assertEquals(1, response.getRecentTransactions().size());
        assertEquals("Uncategorized", response.getRecentTransactions().get(0).getCategoryName());
    }

    @Test
    public void getActiveSavingsGoals_ok_returnsTop4InProgressGoals() {
        // Given
        Long userId = 1L;

        List<Goal> goals = Arrays.asList(
                createGoal(1L, "Vacation", new BigDecimal("2000"), new BigDecimal("1000"), GoalStatus.IN_PROGRESS, LocalDate.now().plusMonths(6)),
                createGoal(2L, "Car", new BigDecimal("10000"), new BigDecimal("3000"), GoalStatus.IN_PROGRESS, LocalDate.now().plusMonths(24)),
                createGoal(3L, "Emergency Fund", new BigDecimal("5000"), new BigDecimal("2500"), GoalStatus.IN_PROGRESS, LocalDate.now().plusMonths(12)),
                createGoal(4L, "Laptop", new BigDecimal("1500"), new BigDecimal("500"), GoalStatus.IN_PROGRESS, LocalDate.now().plusMonths(3)),
                createGoal(5L, "Old Goal", new BigDecimal("1000"), new BigDecimal("1000"), GoalStatus.COMPLETED, LocalDate.now().minusMonths(1))
        );

        when(goalRepository.findAllByUser_UserIdOrderByIdDesc(userId)).thenReturn(goals);
        when(categoryRepository.findByUser_UserIdOrderByNameAsc(userId)).thenReturn(Collections.emptyList());
        when(cashFlowRepository.sumAllIncomeByUser(userId)).thenReturn(new BigDecimal("1000"));
        when(cashFlowRepository.sumAllExpenseByUser(userId)).thenReturn(new BigDecimal("500"));
        when(cashFlowRepository.computeTotalBalanceByUserId(userId)).thenReturn(new BigDecimal("500"));
        when(subscriptionRepository.findByUser_UserIdAndIsActiveTrue(userId)).thenReturn(Collections.emptyList());
        when(cashFlowRepository.findRecentByUserId(userId)).thenReturn(Collections.emptyList());
        when(cashFlowRepository.findByUserIdAndDateRange(anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        // When
        DashboardResponse response = dashboardService.getDashboardData(userId);

        // Then
        assertNotNull(response.getSavingsGoals());
        assertEquals(4, response.getSavingsGoals().size());
        assertTrue(response.getSavingsGoals().stream()
                .allMatch(goal -> goal.getStatus().equals("IN_PROGRESS")));
    }

    @Test
    public void getActiveSavingsGoals_ok_calculatesProgressCorrectly() {
        // Given
        Long userId = 1L;

        List<Goal> goals = Arrays.asList(
                createGoal(1L, "Test Goal", new BigDecimal("1000"), new BigDecimal("250"), GoalStatus.IN_PROGRESS, LocalDate.now().plusMonths(6))
        );

        when(goalRepository.findAllByUser_UserIdOrderByIdDesc(userId)).thenReturn(goals);
        when(categoryRepository.findByUser_UserIdOrderByNameAsc(userId)).thenReturn(Collections.emptyList());
        when(cashFlowRepository.sumAllIncomeByUser(userId)).thenReturn(new BigDecimal("1000"));
        when(cashFlowRepository.sumAllExpenseByUser(userId)).thenReturn(new BigDecimal("500"));
        when(cashFlowRepository.computeTotalBalanceByUserId(userId)).thenReturn(new BigDecimal("500"));
        when(subscriptionRepository.findByUser_UserIdAndIsActiveTrue(userId)).thenReturn(Collections.emptyList());
        when(cashFlowRepository.findRecentByUserId(userId)).thenReturn(Collections.emptyList());
        when(cashFlowRepository.findByUserIdAndDateRange(anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        // When
        DashboardResponse response = dashboardService.getDashboardData(userId);

        // Then
        assertNotNull(response.getSavingsGoals());
        assertEquals(1, response.getSavingsGoals().size());
        GoalResponse goalResponse = response.getSavingsGoals().get(0);
        assertThat(goalResponse.getProgress(), is(closeTo(25.0, 0.01))); // 250/1000 * 100 = 25%
    }

    // ========== Subscription Monthly Equivalent Calculation Tests ==========

    @Test
    public void subscriptionMonthlyEquivalent_weekly_calculatesCorrectly() {
        // Given
        Long userId = 1L;
        Subscription weeklySubscription = createSubscription(1L, "Weekly Gym", new BigDecimal("10"), "WEEKLY", true);

        when(subscriptionRepository.findByUser_UserIdAndIsActiveTrue(userId)).thenReturn(Arrays.asList(weeklySubscription));
        when(categoryRepository.findByUser_UserIdOrderByNameAsc(userId)).thenReturn(Collections.emptyList());
        when(cashFlowRepository.sumAllIncomeByUser(userId)).thenReturn(new BigDecimal("1000"));
        when(cashFlowRepository.sumAllExpenseByUser(userId)).thenReturn(new BigDecimal("500"));
        when(cashFlowRepository.computeTotalBalanceByUserId(userId)).thenReturn(new BigDecimal("500"));
        when(goalRepository.findAllByUser_UserIdOrderByIdDesc(userId)).thenReturn(Collections.emptyList());
        when(cashFlowRepository.findRecentByUserId(userId)).thenReturn(Collections.emptyList());
        when(cashFlowRepository.findByUserIdAndDateRange(anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        // When
        DashboardResponse response = dashboardService.getDashboardData(userId);

        // Then
        SubscriptionSummaryResponse sub = response.getSubscriptions().get(0);
        assertThat(sub.getMonthlyEquivalent(), is(closeTo(new BigDecimal("43.30"), new BigDecimal("0.01")))); // 10 * 4.33
    }

    @Test
    public void subscriptionMonthlyEquivalent_yearly_calculatesCorrectly() {
        // Given
        Long userId = 1L;
        Subscription yearlySubscription = createSubscription(1L, "Annual Plan", new BigDecimal("120"), "YEARLY", true);

        when(subscriptionRepository.findByUser_UserIdAndIsActiveTrue(userId)).thenReturn(Arrays.asList(yearlySubscription));
        when(categoryRepository.findByUser_UserIdOrderByNameAsc(userId)).thenReturn(Collections.emptyList());
        when(cashFlowRepository.sumAllIncomeByUser(userId)).thenReturn(new BigDecimal("1000"));
        when(cashFlowRepository.sumAllExpenseByUser(userId)).thenReturn(new BigDecimal("500"));
        when(cashFlowRepository.computeTotalBalanceByUserId(userId)).thenReturn(new BigDecimal("500"));
        when(goalRepository.findAllByUser_UserIdOrderByIdDesc(userId)).thenReturn(Collections.emptyList());
        when(cashFlowRepository.findRecentByUserId(userId)).thenReturn(Collections.emptyList());
        when(cashFlowRepository.findByUserIdAndDateRange(anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        // When
        DashboardResponse response = dashboardService.getDashboardData(userId);

        // Then
        SubscriptionSummaryResponse sub = response.getSubscriptions().get(0);
        assertEquals(new BigDecimal("10.00"), sub.getMonthlyEquivalent()); // 120 / 12
    }

    @Test
    public void subscriptionMonthlyEquivalent_monthly_returnsSameAmount() {
        // Given
        Long userId = 1L;
        Subscription monthlySubscription = createSubscription(1L, "Monthly Service", new BigDecimal("25.00"), "MONTHLY", true);

        when(subscriptionRepository.findByUser_UserIdAndIsActiveTrue(userId)).thenReturn(Arrays.asList(monthlySubscription));
        when(categoryRepository.findByUser_UserIdOrderByNameAsc(userId)).thenReturn(Collections.emptyList());
        when(cashFlowRepository.sumAllIncomeByUser(userId)).thenReturn(new BigDecimal("1000"));
        when(cashFlowRepository.sumAllExpenseByUser(userId)).thenReturn(new BigDecimal("500"));
        when(cashFlowRepository.computeTotalBalanceByUserId(userId)).thenReturn(new BigDecimal("500"));
        when(goalRepository.findAllByUser_UserIdOrderByIdDesc(userId)).thenReturn(Collections.emptyList());
        when(cashFlowRepository.findRecentByUserId(userId)).thenReturn(Collections.emptyList());
        when(cashFlowRepository.findByUserIdAndDateRange(anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        // When
        DashboardResponse response = dashboardService.getDashboardData(userId);

        // Then
        SubscriptionSummaryResponse sub = response.getSubscriptions().get(0);
        assertEquals(new BigDecimal("25.00"), sub.getMonthlyEquivalent());
    }

    // ========== Category Icon Tests (via Recent Transactions) ==========

    @Test
    public void getCategoryIcon_ok_returnsCorrectIcons() {
        // Given
        Long userId = 1L;
        Category food = createCategory(1L, userId, "Food");
        Category transport = createCategory(2L, userId, "Transport");

        List<CashFlow> cashFlows = Arrays.asList(
                createCashFlow(1L, CashFlowType.Expense, new BigDecimal("50"), LocalDateTime.now(), food),
                createCashFlow(2L, CashFlowType.Expense, new BigDecimal("30"), LocalDateTime.now(), transport)
        );

        when(cashFlowRepository.findRecentByUserId(userId)).thenReturn(cashFlows);
        when(categoryRepository.findByUser_UserIdOrderByNameAsc(userId)).thenReturn(Collections.emptyList());
        when(cashFlowRepository.sumAllIncomeByUser(userId)).thenReturn(new BigDecimal("1000"));
        when(cashFlowRepository.sumAllExpenseByUser(userId)).thenReturn(new BigDecimal("500"));
        when(cashFlowRepository.computeTotalBalanceByUserId(userId)).thenReturn(new BigDecimal("500"));
        when(goalRepository.findAllByUser_UserIdOrderByIdDesc(userId)).thenReturn(Collections.emptyList());
        when(subscriptionRepository.findByUser_UserIdAndIsActiveTrue(userId)).thenReturn(Collections.emptyList());
        when(cashFlowRepository.findByUserIdAndDateRange(anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        // When
        DashboardResponse response = dashboardService.getDashboardData(userId);

        // Then
        assertEquals(2, response.getRecentTransactions().size());
        assertEquals("🍔", response.getRecentTransactions().get(0).getCategoryIcon());
        assertEquals("🚗", response.getRecentTransactions().get(1).getCategoryIcon());
    }

    @Test
    public void getCategoryIcon_nullCategory_returnsDefaultIcon() {
        // Given
        Long userId = 1L;

        List<CashFlow> cashFlows = Arrays.asList(
                createCashFlow(1L, CashFlowType.Expense, new BigDecimal("50"), LocalDateTime.now(), null)
        );

        when(cashFlowRepository.findRecentByUserId(userId)).thenReturn(cashFlows);
        when(categoryRepository.findByUser_UserIdOrderByNameAsc(userId)).thenReturn(Collections.emptyList());
        when(cashFlowRepository.sumAllIncomeByUser(userId)).thenReturn(new BigDecimal("1000"));
        when(cashFlowRepository.sumAllExpenseByUser(userId)).thenReturn(new BigDecimal("500"));
        when(cashFlowRepository.computeTotalBalanceByUserId(userId)).thenReturn(new BigDecimal("500"));
        when(goalRepository.findAllByUser_UserIdOrderByIdDesc(userId)).thenReturn(Collections.emptyList());
        when(subscriptionRepository.findByUser_UserIdAndIsActiveTrue(userId)).thenReturn(Collections.emptyList());
        when(cashFlowRepository.findByUserIdAndDateRange(anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        // When
        DashboardResponse response = dashboardService.getDashboardData(userId);

        // Then
        assertEquals("💰", response.getRecentTransactions().get(0).getCategoryIcon());
    }

    // ========== DashboardResponse Convenience Getters Tests ==========

    @Test
    public void dashboardResponse_convenienceGetters_ok() {
        // Given
        Long userId = 1L;

        when(cashFlowRepository.sumAllIncomeByUser(userId)).thenReturn(new BigDecimal("5000"));
        when(cashFlowRepository.sumAllExpenseByUser(userId)).thenReturn(new BigDecimal("3000"));
        when(cashFlowRepository.computeTotalBalanceByUserId(userId)).thenReturn(new BigDecimal("2000"));
        when(goalRepository.findAllByUser_UserIdOrderByIdDesc(userId)).thenReturn(Collections.emptyList());
        when(categoryRepository.findByUser_UserIdOrderByNameAsc(userId)).thenReturn(Collections.emptyList());
        when(subscriptionRepository.findByUser_UserIdAndIsActiveTrue(userId)).thenReturn(Collections.emptyList());
        when(cashFlowRepository.findRecentByUserId(userId)).thenReturn(Collections.emptyList());
        when(cashFlowRepository.findByUserIdAndDateRange(anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        // When
        DashboardResponse response = dashboardService.getDashboardData(userId);

        // Then
        assertEquals(new BigDecimal("5000"), response.getTotalIncome());
        assertEquals(new BigDecimal("3000"), response.getTotalExpenses());
        assertEquals(new BigDecimal("2000"), response.getBalance());
    }
}
