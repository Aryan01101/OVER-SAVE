package com.example.budgettracker.service;

import com.example.budgettracker.dto.IncomeRequest;
import com.example.budgettracker.dto.IncomeResponse;
import com.example.budgettracker.model.Account;
import com.example.budgettracker.model.CashFlow;
import com.example.budgettracker.model.Category;
import com.example.budgettracker.model.enums.CashFlowType;
import com.example.budgettracker.model.User;
import com.example.budgettracker.repository.AccountRepository;
import com.example.budgettracker.repository.CategoryRepository;
import com.example.budgettracker.repository.CashFlowRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class IncomeServiceTest {

    @Mock
    private CashFlowRepository cashFlowRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private IncomeServiceImpl incomeService;

    private Account account;
    private User user;
    private final Long userId = 39L;
    private IncomeRequest request;
    private CashFlow savedCashFlow;

    @Before
    public void setUp() {
        user = new User();
        user.setUserId(userId);

        account = new Account();
        account.setAccountId(1L);
        account.setBalance(new BigDecimal("2000.00"));
        account.setUser(user);

        request = new IncomeRequest();
        request.setAccountId(1L);
        request.setAmount(new BigDecimal("1000.00"));
        request.setOccurredAt(LocalDateTime.of(2025, 10, 4, 0, 0));
        request.setDescription("Salary");

        savedCashFlow = new CashFlow();
        savedCashFlow.setCashFlowId(10L);
        savedCashFlow.setType(CashFlowType.Income);
        savedCashFlow.setAmount(request.getAmount());
        savedCashFlow.setOccurredAt(request.getOccurredAt());
        savedCashFlow.setDescription(request.getDescription());
        savedCashFlow.setAccount(account);
        savedCashFlow.setCreatedAt(LocalDateTime.now());
    }

    @Test
    public void recordIncome_successfullySavesAndUpdatesAccount() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(cashFlowRepository.save(any(CashFlow.class))).thenReturn(savedCashFlow);
        when(accountRepository.save(any(Account.class))).thenReturn(account);

        IncomeResponse response = incomeService.recordIncome(userId, request);

        assertNotNull(response);
        assertEquals("Income successfully recorded.", response.getMessage());
        assertEquals(new BigDecimal("1000.00"), response.getAmount());
        assertEquals(request.getDescription(), response.getDescription());
        assertEquals(new BigDecimal("3000.00"), response.getUpdatedBalance());

        verify(accountRepository).findById(1L);
        verify(accountRepository).save(account);
        verify(cashFlowRepository).save(any(CashFlow.class));
    }

    @Test
    public void recordIncome_accountNotFound_throwsException() {
        when(accountRepository.findById(1L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> incomeService.recordIncome(userId, request));

        assertEquals("Account not found", ex.getMessage());
        verify(accountRepository).findById(1L);
        verifyNoInteractions(cashFlowRepository);
    }

    @Test
    public void getAllIncome_returnsMappedResponses() {
        Account acc = new Account();
        acc.setAccountId(2L);
        acc.setBalance(new BigDecimal("5000.00"));
        acc.setUser(user);

        CashFlow cf1 = new CashFlow();
        cf1.setCashFlowId(100L);
        cf1.setType(CashFlowType.Income);
        cf1.setAmount(new BigDecimal("200.00"));
        cf1.setOccurredAt(LocalDateTime.of(2025, 1, 10, 0, 0));
        cf1.setCreatedAt(LocalDateTime.now());
        cf1.setDescription("Gift");
        cf1.setAccount(acc);

        CashFlow cf2 = new CashFlow();
        cf2.setCashFlowId(101L);
        cf2.setType(CashFlowType.Income);
        cf2.setAmount(new BigDecimal("800.00"));
        cf2.setOccurredAt(LocalDateTime.of(2025, 2, 15, 0, 0));
        cf2.setCreatedAt(LocalDateTime.now());
        cf2.setDescription("Bonus");
        cf2.setAccount(acc);

        List<CashFlow> cashFlows = new ArrayList<>();
        cashFlows.add(cf1);
        cashFlows.add(cf2);

        when(accountRepository.findByUser_UserId(userId)).thenReturn(List.of(acc));

        when(cashFlowRepository.findByTypeOrderByCreatedAtDesc(CashFlowType.Income))
                .thenReturn(cashFlows);

        List<IncomeResponse> result = incomeService.getAllIncome(userId);

        assertEquals(2, result.size());
        assertEquals(new BigDecimal("200.00"), result.get(0).getAmount());
        assertEquals(new BigDecimal("800.00"), result.get(1).getAmount());
        assertEquals("Gift", result.get(0).getDescription());
        assertEquals("Bonus", result.get(1).getDescription());
        assertEquals(new BigDecimal("5000.00"), result.get(0).getUpdatedBalance());

        verify(cashFlowRepository).findByTypeOrderByCreatedAtDesc(CashFlowType.Income);
    }

    @Test
    public void getAllIncome_returnsEmptyListWhenNoRecords() {
        when(accountRepository.findByUser_UserId(userId)).thenReturn(Collections.emptyList());
        when(cashFlowRepository.findByTypeOrderByCreatedAtDesc(CashFlowType.Income))
                .thenReturn(Collections.emptyList());

        List<IncomeResponse> result = incomeService.getAllIncome(userId);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(cashFlowRepository).findByTypeOrderByCreatedAtDesc(CashFlowType.Income);
    }

    @Test
    public void recordIncome_updatesAccountBalanceAccurately() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(cashFlowRepository.save(any(CashFlow.class))).thenReturn(savedCashFlow);

        BigDecimal before = account.getBalance();
        incomeService.recordIncome(userId, request);
        BigDecimal after = account.getBalance();

        assertEquals(before.add(request.getAmount()), after);
        verify(accountRepository).save(account);
    }

    @Test
    public void recordIncome_withCategory_assignsCategory() {
        Category category = new Category();
        category.setCategoryId(5L);
        category.setName("Bonus");
        category.setUser(user);

        IncomeRequest reqWithCategory = new IncomeRequest();
        reqWithCategory.setAccountId(1L);
        reqWithCategory.setAmount(new BigDecimal("150.00"));
        reqWithCategory.setOccurredAt(LocalDateTime.of(2025, 3, 1, 0, 0));
        reqWithCategory.setDescription("Bonus");
        reqWithCategory.setCategoryId(5L);

        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(categoryRepository.findById(5L)).thenReturn(Optional.of(category));

        ArgumentCaptor<CashFlow> flowCaptor = ArgumentCaptor.forClass(CashFlow.class);
        when(cashFlowRepository.save(any(CashFlow.class))).thenAnswer(invocation -> {
            CashFlow flow = invocation.getArgument(0);
            flow.setCashFlowId(99L);
            return flow;
        });

        incomeService.recordIncome(userId, reqWithCategory);

        verify(cashFlowRepository).save(flowCaptor.capture());
        assertNotNull(flowCaptor.getValue().getCategory());
        assertEquals(Long.valueOf(5L), flowCaptor.getValue().getCategory().getCategoryId());
    }
}
