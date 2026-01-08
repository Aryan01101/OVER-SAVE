package com.example.budgettracker.service;

import com.example.budgettracker.dto.ExpenseRequest;
import com.example.budgettracker.dto.ExpenseResponse;
import com.example.budgettracker.model.Account;
import com.example.budgettracker.model.CashFlow;
import com.example.budgettracker.model.Category;
import com.example.budgettracker.model.User;
import com.example.budgettracker.model.enums.CashFlowType;
import com.example.budgettracker.repository.AccountRepository;
import com.example.budgettracker.repository.CashFlowRepository;
import com.example.budgettracker.repository.CategoryRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@RunWith(org.mockito.junit.MockitoJUnitRunner.class)
public class ExpenseServiceImplTest {

    private static final Long USER_ID = 1L;
    private static final Long CATEGORY_ID = 11L;
    private static final Long ACCOUNT_ID = 22L;

    @Mock private CashFlowRepository cashFlowRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private CategoryRepository categoryRepository;

    @InjectMocks
    private ExpenseServiceImpl expenseService;

    private Category category;
    private Account account;
    private User user;
    private ExpenseRequest baseRequest;

    @Before
    public void setUp() {
        user = new User();
        user.setUserId(USER_ID);
        user.setEmail("test@example.com");

        category = new Category();
        category.setCategoryId(CATEGORY_ID);
        category.setName("Groceries");

        account = new Account();
        account.setAccountId(ACCOUNT_ID);
        account.setName("Wallet");
        account.setBalance(new BigDecimal("100.00"));
        account.setUser(user);

        baseRequest = new ExpenseRequest();
        baseRequest.setCategoryId(CATEGORY_ID);
        baseRequest.setAccountId(ACCOUNT_ID);
        baseRequest.setAmount(new BigDecimal("40.00"));
        baseRequest.setOccurredAt(LocalDateTime.of(2024, 3, 15, 14, 30));
        baseRequest.setDescription("Weekly groceries");
    }

    @Test
    public void recordExpense_succeeds_whenInputsValid() {
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
        when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
        when(accountRepository.save(account)).thenReturn(account);

        LocalDateTime createdAt = LocalDateTime.of(2024, 3, 16, 10, 0);
        when(cashFlowRepository.save(any(CashFlow.class))).thenAnswer(invocation -> {
            CashFlow saved = invocation.getArgument(0);
            saved.setCreatedAt(createdAt);
            return saved;
        });

        ExpenseResponse response = expenseService.recordExpense(USER_ID, baseRequest);

        assertThat(response.getMessage(), is("Expense successfully recorded."));
        assertThat(response.getAmount(), is(baseRequest.getAmount()));
        assertThat(response.getOccurredAt(), is(baseRequest.getOccurredAt()));
        assertThat(response.getDescription(), is(baseRequest.getDescription()));
        assertThat(response.getCategoryName(), is(category.getName()));
        assertThat(response.getUpdatedBalance(), is(new BigDecimal("60.00")));
        assertThat(response.getCreatedAt(), is(createdAt));

        assertThat(account.getBalance(), is(new BigDecimal("60.00")));
        verify(accountRepository).save(account);

        ArgumentCaptor<CashFlow> cashFlowCaptor = ArgumentCaptor.forClass(CashFlow.class);
        verify(cashFlowRepository).save(cashFlowCaptor.capture());
        CashFlow persisted = cashFlowCaptor.getValue();
        assertThat(persisted.getType(), is(CashFlowType.Expense));
        assertThat(persisted.getAmount(), is(baseRequest.getAmount()));
        assertThat(persisted.getOccurredAt(), is(baseRequest.getOccurredAt()));
        assertThat(persisted.getDescription(), is(baseRequest.getDescription()));
        assertSame(account, persisted.getAccount());
        assertSame(category, persisted.getCategory());
    }

    @Test
    public void recordExpense_throws_whenCategoryMissing() {
        when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.empty());

        try {
            expenseService.recordExpense(USER_ID, baseRequest);
            fail("Expected RuntimeException");
        } catch (RuntimeException ex) {
            assertThat(ex.getMessage(), is("Category not found"));
        }

        verify(accountRepository).findById(ACCOUNT_ID);
        verify(accountRepository, never()).save(any());
        verify(cashFlowRepository, never()).save(any());
    }

    @Test
    public void recordExpense_throws_whenAccountMissing() {
        when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.empty());

        try {
            expenseService.recordExpense(USER_ID, baseRequest);
            fail("Expected RuntimeException");
        } catch (RuntimeException ex) {
            assertThat(ex.getMessage(), is("Account not found"));
        }

        verify(accountRepository, never()).save(any());
        verify(cashFlowRepository, never()).save(any());
    }

    @Test
    public void recordExpense_allowsNegativeBalance() {
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
        when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
        when(accountRepository.save(account)).thenReturn(account);

        baseRequest.setAmount(new BigDecimal("150.00"));

        LocalDateTime createdAt = LocalDateTime.of(2024, 3, 16, 10, 0);
        when(cashFlowRepository.save(any(CashFlow.class))).thenAnswer(invocation -> {
            CashFlow saved = invocation.getArgument(0);
            saved.setCreatedAt(createdAt);
            return saved;
        });

        ExpenseResponse response = expenseService.recordExpense(USER_ID, baseRequest);

        assertThat(response.getMessage(), is("Expense successfully recorded."));
        assertThat(response.getAmount(), is(new BigDecimal("150.00")));
        assertThat(response.getUpdatedBalance(), is(new BigDecimal("-50.00")));
        assertThat(account.getBalance(), is(new BigDecimal("-50.00")));

        verify(accountRepository).save(account);
        verify(cashFlowRepository).save(any(CashFlow.class));
    }

    @Test
    public void recordExpense_succeeds_withNullCategory() {
        baseRequest.setCategoryId(null);

        when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
        when(accountRepository.save(account)).thenReturn(account);

        LocalDateTime createdAt = LocalDateTime.of(2024, 3, 16, 10, 0);
        when(cashFlowRepository.save(any(CashFlow.class))).thenAnswer(invocation -> {
            CashFlow saved = invocation.getArgument(0);
            saved.setCreatedAt(createdAt);
            return saved;
        });

        ExpenseResponse response = expenseService.recordExpense(USER_ID, baseRequest);

        assertThat(response.getMessage(), is("Expense successfully recorded."));
        assertThat(response.getCategoryName(), is("Uncategorized"));
        assertThat(response.getUpdatedBalance(), is(new BigDecimal("60.00")));

        verify(categoryRepository, never()).findById(any());
        verify(accountRepository).save(account);

        ArgumentCaptor<CashFlow> cashFlowCaptor = ArgumentCaptor.forClass(CashFlow.class);
        verify(cashFlowRepository).save(cashFlowCaptor.capture());
        CashFlow persisted = cashFlowCaptor.getValue();
        assertNull(persisted.getCategory());
    }

    @Test
    public void recordExpense_decreasesBalance_correctly() {
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
        when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
        when(accountRepository.save(account)).thenReturn(account);

        BigDecimal initialBalance = account.getBalance();
        BigDecimal expenseAmount = new BigDecimal("25.75");
        baseRequest.setAmount(expenseAmount);

        when(cashFlowRepository.save(any(CashFlow.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ExpenseResponse response = expenseService.recordExpense(USER_ID, baseRequest);

        BigDecimal expectedBalance = initialBalance.subtract(expenseAmount);
        assertThat(account.getBalance(), is(expectedBalance));
        assertThat(response.getUpdatedBalance(), is(expectedBalance));
    }

    @Test
    public void getAllExpenses_returnsMappedResponses() {
        Account accountOne = new Account();
        accountOne.setBalance(new BigDecimal("42.50"));
        Account accountTwo = new Account();
        accountTwo.setBalance(BigDecimal.ZERO);

        CashFlow first = CashFlow.builder()
                .type(CashFlowType.Expense)
                .amount(new BigDecimal("12.34"))
                .occurredAt(LocalDateTime.of(2024, 3, 1, 12, 0))
                .createdAt(LocalDateTime.of(2024, 3, 1, 9, 0))
                .description("Lunch")
                .account(accountOne)
                .category(category)
                .build();

        CashFlow second = CashFlow.builder()
                .type(CashFlowType.Expense)
                .amount(new BigDecimal("5.55"))
                .occurredAt(LocalDateTime.of(2024, 3, 2, 15, 0))
                .createdAt(LocalDateTime.of(2024, 3, 2, 18, 30))
                .description("Snacks")
                .account(accountTwo)
                .category(null)
                .build();

        when(cashFlowRepository.findRecentByUserId(USER_ID))
                .thenReturn(List.of(first, second));

        List<ExpenseResponse> responses = expenseService.getAllExpenses(USER_ID);

        assertThat(responses.size(), is(2));
        ExpenseResponse firstResponse = responses.get(0);
        assertThat(firstResponse.getAmount(), is(first.getAmount()));
        assertThat(firstResponse.getOccurredAt(), is(first.getOccurredAt()));
        assertThat(firstResponse.getCreatedAt(), is(first.getCreatedAt()));
        assertThat(firstResponse.getDescription(), is(first.getDescription()));
        assertThat(firstResponse.getCategoryName(), is(category.getName()));
        assertThat(firstResponse.getUpdatedBalance(), is(accountOne.getBalance()));

        ExpenseResponse secondResponse = responses.get(1);
        assertThat(secondResponse.getCategoryName(), is("Uncategorized"));
        assertThat(secondResponse.getUpdatedBalance(), is(accountTwo.getBalance()));
    }

    @Test
    public void getAllExpenses_returnsEmptyList_whenNoExpenses() {
        when(cashFlowRepository.findRecentByUserId(USER_ID))
                .thenReturn(List.of());

        List<ExpenseResponse> responses = expenseService.getAllExpenses(USER_ID);

        assertThat(responses.size(), is(0));
        assertTrue(responses.isEmpty());
    }

    @Test
    public void recordExpense_preservesDescription_whenProvided() {
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
        when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
        when(accountRepository.save(account)).thenReturn(account);

        String testDescription = "Test description with special chars: @#$%";
        baseRequest.setDescription(testDescription);

        when(cashFlowRepository.save(any(CashFlow.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ExpenseResponse response = expenseService.recordExpense(USER_ID, baseRequest);

        assertThat(response.getDescription(), is(testDescription));

        ArgumentCaptor<CashFlow> cashFlowCaptor = ArgumentCaptor.forClass(CashFlow.class);
        verify(cashFlowRepository).save(cashFlowCaptor.capture());
        assertThat(cashFlowCaptor.getValue().getDescription(), is(testDescription));
    }

    @Test
    public void recordExpense_preservesOccurredAt_whenProvided() {
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
        when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
        when(accountRepository.save(account)).thenReturn(account);

        LocalDateTime testDate = LocalDateTime.of(2023, 12, 25, 18, 45);
        baseRequest.setOccurredAt(testDate);

        when(cashFlowRepository.save(any(CashFlow.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ExpenseResponse response = expenseService.recordExpense(USER_ID, baseRequest);

        assertThat(response.getOccurredAt(), is(testDate));

        ArgumentCaptor<CashFlow> cashFlowCaptor = ArgumentCaptor.forClass(CashFlow.class);
        verify(cashFlowRepository).save(cashFlowCaptor.capture());
        assertThat(cashFlowCaptor.getValue().getOccurredAt(), is(testDate));
    }
}
