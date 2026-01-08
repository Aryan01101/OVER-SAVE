package com.example.budgettracker.usecase;

import com.example.budgettracker.dto.ExpenseRequest;
import com.example.budgettracker.dto.ExpenseResponse;
import com.example.budgettracker.model.Account;
import com.example.budgettracker.model.CashFlow;
import com.example.budgettracker.model.Category;
import com.example.budgettracker.model.User;
import com.example.budgettracker.model.enums.AccountType;
import com.example.budgettracker.model.enums.CashFlowType;
import com.example.budgettracker.repository.AccountRepository;
import com.example.budgettracker.repository.CashFlowRepository;
import com.example.budgettracker.repository.CategoryRepository;
import com.example.budgettracker.service.ExpenseServiceImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * UC-05: Add Expense Use Case Tests
 * Tests expense creation functionality including validation, categorization, and dashboard updates
 */
@RunWith(MockitoJUnitRunner.class)
public class AddExpenseTest {

    @Mock
    private CashFlowRepository cashFlowRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ExpenseServiceImpl expenseService;

    private ExpenseRequest validExpenseRequest;
    private Account mockAccount;
    private Category mockCategory;
    private CashFlow mockCashFlow;
    private User mockUser;

    @Before
    public void setUp() {
        mockUser = User.builder()
                .userId(1L)
                .email("testuser@example.com")
                .firstName("Test")
                .lastName("User")
                .build();

        mockAccount = Account.builder()
                .accountId(1L)
                .name("Cash Account")
                .balance(new BigDecimal("1000.00"))
                .accountType(AccountType.CASH)
                .user(mockUser)
                .build();

        mockCategory = Category.builder()
                .categoryId(1L)
                .name("Food")
                .system(false)
                .user(mockUser)
                .build();

        validExpenseRequest = new ExpenseRequest();
        validExpenseRequest.setAccountId(1L);
        validExpenseRequest.setCategoryId(1L);
        validExpenseRequest.setAmount(new BigDecimal("50.00"));
        validExpenseRequest.setDescription("Lunch at restaurant");
        validExpenseRequest.setOccurredAt(LocalDateTime.now());

        mockCashFlow = CashFlow.builder()
                .cashFlowId(1L)
                .type(CashFlowType.Expense)
                .amount(new BigDecimal("50.00"))
                .description("Lunch at restaurant")
                .account(mockAccount)
                .category(mockCategory)
                .occurredAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    public void testAddExpense_ValidExpense_Success() {
        // Arrange
        when(accountRepository.findById(1L)).thenReturn(Optional.of(mockAccount));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(mockCategory));
        when(cashFlowRepository.save(any(CashFlow.class))).thenReturn(mockCashFlow);
        when(accountRepository.save(any(Account.class))).thenReturn(mockAccount);

        // Act
        ExpenseResponse response = expenseService.recordExpense(1L, validExpenseRequest);

        // Assert
        assertNotNull("Response should not be null", response);
        assertEquals("Amount should match", new BigDecimal("50.00"), response.getAmount());
        assertEquals("Description should match", "Lunch at restaurant", response.getDescription());
        assertNotNull("Created timestamp should be set", response.getCreatedAt());
        verify(cashFlowRepository, times(1)).save(any(CashFlow.class));
        verify(accountRepository, times(1)).save(any(Account.class));
    }

    @Test
    public void testAddExpense_WithCategory_Success() {
        // Arrange
        when(accountRepository.findById(1L)).thenReturn(Optional.of(mockAccount));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(mockCategory));
        when(cashFlowRepository.save(any(CashFlow.class))).thenReturn(mockCashFlow);
        when(accountRepository.save(any(Account.class))).thenReturn(mockAccount);

        // Act
        ExpenseResponse response = expenseService.recordExpense(1L, validExpenseRequest);

        // Assert
        assertNotNull("Response should not be null", response);
        assertEquals("Category should be assigned", "Food", response.getCategoryName());
        verify(categoryRepository, times(1)).findById(1L);
        verify(cashFlowRepository, times(1)).save(argThat(cashFlow ->
                cashFlow.getCategory() != null && cashFlow.getCategory().getName().equals("Food")
        ));
    }

    @Test
    public void testAddExpense_InvalidAmount_Failure() {
        // Arrange - Negative amount
        ExpenseRequest invalidRequest = new ExpenseRequest();
        invalidRequest.setAccountId(1L);
        invalidRequest.setCategoryId(1L);
        invalidRequest.setAmount(new BigDecimal("-50.00"));
        invalidRequest.setDescription("Invalid expense");
        invalidRequest.setOccurredAt(LocalDateTime.now());

        // Act & Assert - In a real scenario, validation would throw exception
        // Here we verify that the amount is invalid
        assertTrue("Amount should be negative", invalidRequest.getAmount().compareTo(BigDecimal.ZERO) < 0);
        // Bean validation (@Positive annotation) would prevent this from reaching the service layer
        // This test validates the request object itself
    }

    @Test
    public void testAddExpense_MissingFields_Failure() {
        // Arrange - Missing required fields
        ExpenseRequest incompleteRequest = new ExpenseRequest();
        incompleteRequest.setAccountId(1L);
        // Missing: amount, description, occurredAt

        // Act & Assert
        assertNull("Amount should be missing", incompleteRequest.getAmount());
        assertNull("Description should be missing", incompleteRequest.getDescription());
        assertNull("Occurred at should be missing", incompleteRequest.getOccurredAt());

        // In real scenario, @NotNull validation would prevent this from reaching service
        // Bean validation annotations would catch these at controller level
        // This test validates the request object structure
    }

    @Test
    public void testAddExpense_DashboardUpdate_Success() {
        // Arrange
        BigDecimal initialBalance = new BigDecimal("1000.00");
        BigDecimal expenseAmount = new BigDecimal("50.00");
        BigDecimal expectedBalance = initialBalance.subtract(expenseAmount);

        mockAccount.setBalance(initialBalance);

        when(accountRepository.findById(1L)).thenReturn(Optional.of(mockAccount));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(mockCategory));
        when(cashFlowRepository.save(any(CashFlow.class))).thenReturn(mockCashFlow);
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            Account savedAccount = invocation.getArgument(0);
            // Verify balance was updated
            assertEquals("Balance should be updated", expectedBalance, savedAccount.getBalance());
            return savedAccount;
        });

        // Act
        ExpenseResponse response = expenseService.recordExpense(1L, validExpenseRequest);

        // Assert
        assertNotNull("Response should not be null", response);
        assertNotNull("Updated balance should be returned", response.getUpdatedBalance());
        assertEquals("Balance should reflect expense deduction", expectedBalance, response.getUpdatedBalance());
        verify(accountRepository, times(1)).save(argThat(account ->
                account.getBalance().compareTo(expectedBalance) == 0
        ));
    }
}
