package com.example.budgettracker.service;

import com.example.budgettracker.dto.Goal.*;
import com.example.budgettracker.model.*;
import com.example.budgettracker.model.enums.AccountType;
import com.example.budgettracker.model.enums.CashFlowType;
import com.example.budgettracker.model.enums.GoalStatus;
import com.example.budgettracker.repository.*;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.DataIntegrityViolationException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class GoalServiceImplTest {

    @Mock private UserRepository userRepo;
    @Mock private AccountRepository accountRepo;
    @Mock private GoalRepository goalRepo;
    @Mock private CashFlowRepository cashFlowRepo;
    @Mock private CategoryRepository categoryRepo;
    @Mock private TransferRepository transferRepo;

    @InjectMocks
    private GoalServiceImpl goalService;

    private User user;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        user = new User();
        user.setUserId(1L);
    }

    // ---------------- createGoal ----------------

    @Test(expected = IllegalArgumentException.class)
    public void testCreateGoalUserNotFound() {
        when(userRepo.findById(1L)).thenReturn(Optional.empty());

        GoalRequest req = new GoalRequest();
        req.setName("NewGoal");
        req.setTargetAmount(BigDecimal.TEN);
        req.setDueDate(LocalDate.now());

        goalService.createGoal(1L, req);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateGoalAlreadyExists() {
        when(userRepo.findById(1L)).thenReturn(Optional.of(user));
        when(goalRepo.existsByUser_UserIdAndNameIgnoreCase(1L, "DupGoal"))
                .thenReturn(true);

        GoalRequest req = new GoalRequest();
        req.setName("DupGoal");
        req.setTargetAmount(BigDecimal.TEN);
        req.setDueDate(LocalDate.now());

        goalService.createGoal(1L, req);
    }

    // ---------------- getGoalById ----------------

    @Test(expected = IllegalArgumentException.class)
    public void testGetGoalByIdNotFound() {
        when(goalRepo.findById(99L)).thenReturn(Optional.empty());
        goalService.getGoalById(1L, 99L);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetGoalByIdNotOwned() {
        User otherUser = new User(); otherUser.setUserId(2L);
        Goal goal = new Goal(); goal.setUser(otherUser);
        when(goalRepo.findById(5L)).thenReturn(Optional.of(goal));

        goalService.getGoalById(1L, 5L);
    }

    // ---------------- updateGoal ----------------

    @Test(expected = IllegalArgumentException.class)
    public void testUpdateGoalNotFound() {
        when(goalRepo.findById(77L)).thenReturn(Optional.empty());

        GoalRequest req = new GoalRequest();
        req.setName("X");
        req.setTargetAmount(BigDecimal.ONE);
        req.setDueDate(LocalDate.now());

        goalService.updateGoal(1L, 77L, req);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpdateGoalNotOwned() {
        User otherUser = new User(); otherUser.setUserId(2L);
        Goal goal = new Goal(); goal.setUser(otherUser);
        when(goalRepo.findById(7L)).thenReturn(Optional.of(goal));

        GoalRequest req = new GoalRequest();
        req.setName("X");
        req.setTargetAmount(BigDecimal.ONE);
        req.setDueDate(LocalDate.now());

        goalService.updateGoal(1L, 7L, req);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpdateGoalDuplicateName() {
        Goal goal = new Goal();
        goal.setUser(user);
        goal.setName("OldName");
        when(goalRepo.findById(7L)).thenReturn(Optional.of(goal));
        when(goalRepo.existsByUser_UserIdAndNameIgnoreCase(1L, "NewName"))
                .thenReturn(true);

        GoalRequest req = new GoalRequest();
        req.setName("NewName");
        req.setTargetAmount(BigDecimal.ONE);
        req.setDueDate(LocalDate.now());

        goalService.updateGoal(1L, 7L, req);
    }

    // ---------------- deleteGoal ----------------

    @Test(expected = IllegalArgumentException.class)
    public void testDeleteGoalNotFound() {
        when(goalRepo.findById(66L)).thenReturn(Optional.empty());
        goalService.deleteGoal(1L, 66L);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDeleteGoalNotOwned() {
        User user = new User(); user.setUserId(1L);
        User other = new User(); other.setUserId(2L);

        Goal goal = new Goal(); goal.setId(8L); goal.setUser(other);
        when(goalRepo.findById(8L)).thenReturn(Optional.of(goal));

        goalService.deleteGoal(1L, 8L);
    }
    // ---------------- contributeToGoal ----------------

    @Test
    public void testContributeCompletesGoal() {
        Account cash = new Account();
        cash.setUser(user);
        cash.setBalance(new BigDecimal("1000"));
        cash.setAccountId(1L);

        Goal goal = new Goal();
        goal.setUser(user);
        goal.setTargetAmount(new BigDecimal("500"));
        goal.setCurrentAmount(new BigDecimal("400"));
        Account goalAcc = new Account();
        goalAcc.setUser(user);
        goal.setLinkedAccount(goalAcc);

        when(userRepo.findById(1L)).thenReturn(Optional.of(user));
        when(accountRepo.findById(1L)).thenReturn(Optional.of(cash));
        when(goalRepo.findById(2L)).thenReturn(Optional.of(goal));
        when(categoryRepo.findByUser_UserIdAndNameIgnoreCase(anyLong(), anyString()))
                .thenReturn(Optional.of(new Category()));

        ContributionRequest req = new ContributionRequest();
        req.setFromAccountId(1L);
        req.setGoalId(2L);
        req.setAmount(new BigDecimal("200"));

        ContributionResponse resp = goalService.contributeToGoal(1L, req);

        assertEquals("COMPLETED", goal.getStatus().name());
        assertEquals(new BigDecimal("600"), goal.getCurrentAmount());
        assertEquals("Contribution successful", resp.getMessage());
    }

    // ---------------- createGoal ----------------

    @Test
    public void testCreateGoalSuccess() {
        when(userRepo.findById(1L)).thenReturn(Optional.of(user));
        when(goalRepo.existsByUser_UserIdAndNameIgnoreCase(1L, "MyGoal"))
                .thenReturn(false);

        GoalRequest req = new GoalRequest();
        req.setName("MyGoal");
        req.setTargetAmount(BigDecimal.valueOf(1000));
        req.setDueDate(LocalDate.now());

        GoalResponse resp = goalService.createGoal(1L, req);

        assertEquals("MyGoal", resp.getName());
        verify(accountRepo, times(1)).save(any(Account.class));
        verify(goalRepo, times(1)).save(any(Goal.class));
    }

// ---------------- getAllGoals ----------------

    @Test
    public void testGetAllGoalsSuccess() {
        Goal g = new Goal(); g.setUser(user); g.setName("TestGoal");
        when(userRepo.findById(1L)).thenReturn(Optional.of(user));
        when(goalRepo.findByUser(user)).thenReturn(java.util.List.of(g));

        java.util.List<GoalResponse> list = goalService.getAllGoals(1L);
        assertEquals(1, list.size());
        assertEquals("TestGoal", list.get(0).getName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetAllGoalsUserNotFound() {
        when(userRepo.findById(1L)).thenReturn(Optional.empty());
        goalService.getAllGoals(1L);
    }

// ---------------- getGoalById ----------------

    @Test
    public void testGetGoalByIdSuccess() {
        Goal goal = new Goal();
        goal.setUser(user); goal.setName("GoalX");
        when(goalRepo.findById(10L)).thenReturn(Optional.of(goal));

        GoalResponse resp = goalService.getGoalById(1L, 10L);
        assertEquals("GoalX", resp.getName());
    }

// ---------------- updateGoal ----------------

    @Test
    public void testUpdateGoalSuccess() {
        // 现有 goal（必须补齐这些字段，避免 toResponse 时 NPE）
        Goal goal = new Goal();
        goal.setUser(user);
        goal.setName("Old");
        goal.setStatus(GoalStatus.IN_PROGRESS);
        goal.setTargetAmount(BigDecimal.TEN);
        goal.setCurrentAmount(BigDecimal.ZERO);
        goal.setDueDate(LocalDate.now());

        when(goalRepo.findById(7L)).thenReturn(Optional.of(goal));
        when(goalRepo.existsByUser_UserIdAndNameIgnoreCase(1L, "New"))
                .thenReturn(false);
        when(goalRepo.save(any(Goal.class))).thenAnswer(inv -> inv.getArgument(0));

        GoalRequest req = new GoalRequest();
        req.setName("New");
        req.setTargetAmount(BigDecimal.valueOf(123));
        req.setDueDate(LocalDate.now().plusDays(1));

        GoalResponse resp = goalService.updateGoal(1L, 7L, req);

        assertEquals("New", resp.getName());
        assertEquals(0.0, resp.getProgress(), 0.0001);
    }


// ---------------- deleteGoal ----------------

    @Test
    public void testDeleteGoalRemainZero() {
        Goal goal = new Goal();
        goal.setUser(user);
        goal.setLinkedAccount(new Account());
        goal.setCurrentAmount(BigDecimal.ZERO);
        when(goalRepo.findById(5L)).thenReturn(Optional.of(goal));

        goalService.deleteGoal(1L, 5L);
        verify(goalRepo, times(1)).delete(goal);
    }

    @Test
    public void testDeleteGoalWithRemain() {
        User u = new User(); u.setUserId(1L);

        Account goalAcc = new Account();
        goalAcc.setAccountId(22L);
        goalAcc.setUser(u);
        goalAcc.setBalance(new BigDecimal("10.00"));

        Account cashAcc = new Account();
        cashAcc.setAccountId(33L);
        cashAcc.setUser(u);
        cashAcc.setBalance(BigDecimal.ZERO);

        Goal g = new Goal();
        g.setId(7L);
        g.setUser(u);
        g.setLinkedAccount(goalAcc);
        g.setCurrentAmount(new BigDecimal("5.00")); // remain

        when(goalRepo.findById(7L)).thenReturn(Optional.of(g));
        when(accountRepo.findFirstByUser_UserIdAndAccountType(eq(1L), eq(AccountType.CASH)))
                .thenReturn(Optional.of(cashAcc));
        Category cat = new Category(); cat.setCategoryId(123L);
        when(categoryRepo.findByUser_UserIdAndNameIgnoreCase(eq(1L), anyString()))
                .thenReturn(Optional.of(cat));

        goalService.deleteGoal(1L, 7L);

        assertEquals(new BigDecimal("5.00"), goalAcc.getBalance());
        assertEquals(new BigDecimal("5.00"), cashAcc.getBalance());

        assertEquals(BigDecimal.ZERO, g.getCurrentAmount());
        verify(goalRepo).delete(g);

        verify(transferRepo).save(any(Transfer.class));
        verify(cashFlowRepo, times(2)).save(any(CashFlow.class));
    }

// ---------------- getContributions ----------------

    @Test
    public void testGetContributionsSuccess() {
        Goal g = new Goal();
        g.setUser(user);
        when(goalRepo.findById(3L)).thenReturn(Optional.of(g));

        Category cat = new Category();
        cat.setCategoryId(123L);
        when(categoryRepo.findByUser_UserIdAndNameIgnoreCase(eq(1L), anyString()))
                .thenReturn(Optional.of(cat));

        when(cashFlowRepo.findByUserCategoryPeriodAndOptionalType(
                eq(1L), eq(123L),
                any(LocalDateTime.class), any(LocalDateTime.class),
                isNull()))
                .thenReturn(java.util.List.of(new CashFlow()));

        var list = goalService.getContributions(
                1L, 3L, LocalDate.now().minusDays(1), LocalDate.now());

        assertEquals(1, list.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testContributeGoalNotFound() {
        User user = new User(); user.setUserId(1L);
        Account cash = new Account(); cash.setAccountId(10L); cash.setUser(user); cash.setBalance(BigDecimal.TEN);
        when(userRepo.findById(1L)).thenReturn(Optional.of(user));
        when(accountRepo.findById(10L)).thenReturn(Optional.of(cash));
        when(goalRepo.findById(99L)).thenReturn(Optional.empty());

        ContributionRequest req = new ContributionRequest();
        req.setFromAccountId(10L);
        req.setGoalId(99L);
        req.setAmount(BigDecimal.ONE);

        goalService.contributeToGoal(1L, req);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testContributeAccountNotOwned() {
        User user = new User(); user.setUserId(1L);
        User other = new User(); other.setUserId(2L);

        Account cash = new Account(); cash.setAccountId(10L); cash.setUser(other); cash.setBalance(BigDecimal.TEN);
        when(userRepo.findById(1L)).thenReturn(Optional.of(user));
        when(accountRepo.findById(10L)).thenReturn(Optional.of(cash));

        ContributionRequest req = new ContributionRequest();
        req.setFromAccountId(10L);
        req.setGoalId(7L);
        req.setAmount(BigDecimal.ONE);

        goalService.contributeToGoal(1L, req);
    }

    @Test
    public void testEnsureGoalTransferCategoryFallback() throws Exception {
        User u = new User(); u.setUserId(1L);

        when(categoryRepo.findByUser_UserIdAndNameIgnoreCase(eq(1L), anyString()))
                .thenReturn(Optional.empty(), Optional.empty());
        when(categoryRepo.save(any(Category.class)))
                .thenThrow(new DataIntegrityViolationException("dup"));

        Method m = GoalServiceImpl.class.getDeclaredMethod("ensureGoalTransferCategory", User.class);
        m.setAccessible(true);

        try {
            m.invoke(goalService, u);
            fail("Expected RuntimeException from ensureGoalTransferCategory");
        } catch (InvocationTargetException ite) {
            Throwable cause = ite.getCause();
            assertTrue(cause instanceof RuntimeException);
            assertEquals("System category creation failed", cause.getMessage());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testContributeAmountNullOrNonPositive() {
        User u = new User(); u.setUserId(1L);
        Account cash = new Account(); cash.setAccountId(10L); cash.setUser(u); cash.setBalance(new BigDecimal("1000"));
        Account goalAcc = new Account(); goalAcc.setAccountId(20L); goalAcc.setUser(u);
        Goal g = new Goal(); g.setId(2L); g.setUser(u); g.setLinkedAccount(goalAcc);

        when(userRepo.findById(1L)).thenReturn(Optional.of(u));
        when(accountRepo.findById(10L)).thenReturn(Optional.of(cash));
        when(goalRepo.findById(2L)).thenReturn(Optional.of(g));


        ContributionRequest req = new ContributionRequest();
        req.setFromAccountId(10L);
        req.setGoalId(2L);
        req.setAmount(null);

        goalService.contributeToGoal(1L, req);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testContributeInsufficientFundsBranch() {
        // 余额不足
        User u = new User(); u.setUserId(1L);
        Account cash = new Account(); cash.setAccountId(10L); cash.setUser(u); cash.setBalance(new BigDecimal("50"));
        Account goalAcc = new Account(); goalAcc.setAccountId(20L); goalAcc.setUser(u);
        Goal g = new Goal(); g.setId(2L); g.setUser(u); g.setLinkedAccount(goalAcc);

        when(userRepo.findById(1L)).thenReturn(Optional.of(u));
        when(accountRepo.findById(10L)).thenReturn(Optional.of(cash));
        when(goalRepo.findById(2L)).thenReturn(Optional.of(g));

        ContributionRequest req = new ContributionRequest();
        req.setFromAccountId(10L);
        req.setGoalId(2L);
        req.setAmount(new BigDecimal("200"));

        goalService.contributeToGoal(1L, req);
    }

// ---------------- getContributions 其他分支 ----------------

    @Test(expected = IllegalArgumentException.class)
    public void testGetContributions_GoalNotFound() {
        when(goalRepo.findById(404L)).thenReturn(Optional.empty());
        goalService.getContributions(1L, 404L, LocalDate.now().minusDays(7), LocalDate.now());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetContributions_GoalNotOwned() {
        User other = new User(); other.setUserId(2L);
        Goal g = new Goal(); g.setUser(other);
        when(goalRepo.findById(3L)).thenReturn(Optional.of(g));
        goalService.getContributions(1L, 3L, LocalDate.now().minusDays(7), LocalDate.now());
    }

    @Test
    public void testGetContributions_NullDates_UseMinMax() {
        Goal g = new Goal(); g.setUser(user);
        when(goalRepo.findById(3L)).thenReturn(Optional.of(g));

        Category cat = new Category(); cat.setCategoryId(111L);
        when(categoryRepo.findByUser_UserIdAndNameIgnoreCase(eq(1L), anyString()))
                .thenReturn(Optional.of(cat));

        when(cashFlowRepo.findByUserCategoryPeriodAndOptionalType(
                anyLong(), anyLong(), any(LocalDateTime.class), any(LocalDateTime.class), isNull()))
                .thenReturn(java.util.List.of(new CashFlow()));

        goalService.getContributions(1L, 3L, null, null);

        ArgumentCaptor<LocalDateTime> fromCap = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> toCap = ArgumentCaptor.forClass(LocalDateTime.class);

        verify(cashFlowRepo).findByUserCategoryPeriodAndOptionalType(
                eq(1L), eq(111L), fromCap.capture(), toCap.capture(), isNull());

        assertEquals(LocalDate.of(1970, 1, 1), fromCap.getValue().toLocalDate());
        assertEquals(LocalTime.MIDNIGHT, fromCap.getValue().toLocalTime());
        assertEquals(LocalDate.of(3000, 12, 31), toCap.getValue().toLocalDate());
        assertEquals(LocalTime.MAX, toCap.getValue().toLocalTime());
    }

// ---------------- deleteGoal: remain==0 时不应有任何资金流水 ----------------

    @Test
    public void testDeleteGoalRemainZero_NoSideEffects() {
        Goal goal = new Goal();
        goal.setUser(user);
        goal.setLinkedAccount(new Account());
        goal.setCurrentAmount(BigDecimal.ZERO);
        when(goalRepo.findById(55L)).thenReturn(Optional.of(goal));

        goalService.deleteGoal(1L, 55L);

        verify(goalRepo, times(1)).delete(goal);
        verify(transferRepo, never()).save(any());
        verify(cashFlowRepo, never()).save(any());
    }

// ---------------- contributeToGoal: amount==0 ----------------

    @Test(expected = IllegalArgumentException.class)
    public void testContributeAmountZero_shouldThrow() {
        User u = new User(); u.setUserId(1L);
        Account cash = new Account(); cash.setAccountId(10L); cash.setUser(u); cash.setBalance(new BigDecimal("100"));
        Account goalAcc = new Account(); goalAcc.setAccountId(20L); goalAcc.setUser(u);
        Goal g = new Goal(); g.setId(2L); g.setUser(u); g.setLinkedAccount(goalAcc);

        when(userRepo.findById(1L)).thenReturn(Optional.of(u));
        when(accountRepo.findById(10L)).thenReturn(Optional.of(cash));
        when(goalRepo.findById(2L)).thenReturn(Optional.of(g));

        ContributionRequest req = new ContributionRequest();
        req.setFromAccountId(10L);
        req.setGoalId(2L);
        req.setAmount(BigDecimal.ZERO);

        goalService.contributeToGoal(1L, req);
    }


}
