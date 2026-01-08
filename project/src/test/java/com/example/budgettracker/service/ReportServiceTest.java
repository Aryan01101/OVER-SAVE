package com.example.budgettracker.service;

import com.example.budgettracker.model.*;
import com.example.budgettracker.model.enums.CashFlowType;
import com.example.budgettracker.repository.CashFlowRepository;
import com.example.budgettracker.repository.CategoryRepository;
import com.example.budgettracker.repository.TransferRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(org.mockito.junit.MockitoJUnitRunner.class)
public class ReportServiceTest {

    @Mock private CashFlowRepository cashFlowRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private TransferRepository transferRepository;

    @InjectMocks
    private ReportService reportService;

    private final Long userId = 1L;
    private final LocalDateTime startDateTime = LocalDateTime.of(2025, 1, 1, 0, 0);
    private final LocalDateTime endDateTime = LocalDateTime.of(2025, 10, 30, 23, 59, 59);

    private Category foodCategory;
    private Category playCategory;

    @Before
    public void setUp() {
        foodCategory = new Category();
        foodCategory.setCategoryId(100L);
        foodCategory.setName("Food");

        playCategory = new Category();
        playCategory.setCategoryId(200L);
        playCategory.setName("Play");

        when(categoryRepository.findById(100L)).thenReturn(Optional.of(foodCategory));
        when(categoryRepository.findById(200L)).thenReturn(Optional.of(playCategory));
    }

    @Test
    public void generateReport_basicAggregation_success() {
        CashFlow income1 = flow(1L, CashFlowType.Income, new BigDecimal("3000"),
                LocalDateTime.of(2025, 1, 10, 10, 0), null, null);
        CashFlow income2 = flow(2L, CashFlowType.Income, new BigDecimal("2001"),
                LocalDateTime.of(2025, 2, 15, 11, 0), null, null);
        CashFlow expense1 = flow(3L, CashFlowType.Expense, new BigDecimal("110.50"),
                LocalDateTime.of(2025, 3, 5, 12, 0), null, 100L);
        CashFlow expense2 = flow(4L, CashFlowType.Expense, new BigDecimal("20.50"),
                LocalDateTime.of(2025, 4, 10, 13, 0), null, 200L);

        List<CashFlow> cashFlows = List.of(income1, income2, expense1, expense2);
        when(cashFlowRepository.findByAccount_User_UserIdAndOccurredAtBetween(userId, startDateTime, endDateTime))
                .thenReturn(cashFlows);

        Transfer t1 = new Transfer();
        t1.setAmount(new BigDecimal("500"));
        t1.setCreatedAt(LocalDateTime.of(2025, 3, 1, 9, 0));

        Transfer t2 = new Transfer();
        t2.setAmount(new BigDecimal("300"));
        t2.setCreatedAt(LocalDateTime.of(2025, 4, 1, 10, 0));

        List<Transfer> transfers = List.of(t1, t2);
        when(transferRepository.findByAccountFrom_User_UserIdAndCreatedAtBetween(
                eq(userId), eq(startDateTime), eq(endDateTime)
        )).thenReturn(transfers);

        Map<String, Object> report = reportService.generateReport(userId, startDateTime, endDateTime);

        assertEquals(0, ((BigDecimal) report.get("totalIncome")).compareTo(new BigDecimal("5001.00")));
        assertEquals(0, ((BigDecimal) report.get("totalExpense")).compareTo(new BigDecimal("131.00")));
        assertEquals(0, ((BigDecimal) report.get("balance")).compareTo(new BigDecimal("4870.00")));
        assertEquals(0, ((BigDecimal) report.get("transfer")).compareTo(new BigDecimal("800.00")));

        Map<String, BigDecimal> expenseByCategory = (Map<String, BigDecimal>) report.get("expenseByCategory");
        assertEquals(0, expenseByCategory.get("Food").compareTo(new BigDecimal("110.50")));
        assertEquals(0, expenseByCategory.get("Play").compareTo(new BigDecimal("20.50")));
    }

    @Test
    public void generateReport_emptyCashFlowAndTransfer_returnsZeroTotals() {
        when(cashFlowRepository.findByAccount_User_UserIdAndOccurredAtBetween(userId, startDateTime, endDateTime))
                .thenReturn(Collections.emptyList());
        when(transferRepository.findByAccountFrom_User_UserIdAndCreatedAtBetween(
                eq(userId), eq(startDateTime), eq(endDateTime)
        )).thenReturn(Collections.emptyList());

        Map<String, Object> report = reportService.generateReport(userId, startDateTime, endDateTime);

        assertEquals(0, ((BigDecimal) report.get("totalIncome")).compareTo(BigDecimal.ZERO));
        assertEquals(0, ((BigDecimal) report.get("totalExpense")).compareTo(BigDecimal.ZERO));
        assertEquals(0, ((BigDecimal) report.get("balance")).compareTo(BigDecimal.ZERO));
        assertEquals(0, ((BigDecimal) report.get("transfer")).compareTo(BigDecimal.ZERO));

        Map<String, BigDecimal> expenseByCategory = (Map<String, BigDecimal>) report.get("expenseByCategory");
        assertTrue(expenseByCategory.isEmpty());
    }

    @Test
    public void generateReport_categoryMissing_returnsUnknown() {
        CashFlow expense = flow(5L, CashFlowType.Expense, new BigDecimal("50.00"),
                LocalDateTime.of(2025, 5, 5, 9, 0), null, 999L);
        when(cashFlowRepository.findByAccount_User_UserIdAndOccurredAtBetween(userId, startDateTime, endDateTime))
                .thenReturn(List.of(expense));
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());
        when(transferRepository.findByAccountFrom_User_UserIdAndCreatedAtBetween(any(), any(), any()))
                .thenReturn(Collections.emptyList());

        Map<String, Object> report = reportService.generateReport(userId, startDateTime, endDateTime);
        Map<String, BigDecimal> expenseByCategory = (Map<String, BigDecimal>) report.get("expenseByCategory");

        assertEquals(0, expenseByCategory.get("Unknown").compareTo(new BigDecimal("50.00")));
    }

    @Test
    public void generateReport_nullCategory_returnsUncategorized() {
        CashFlow expense = flow(6L, CashFlowType.Expense, new BigDecimal("75.00"),
                LocalDateTime.of(2025, 6, 6, 8, 30), null, null);
        when(cashFlowRepository.findByAccount_User_UserIdAndOccurredAtBetween(userId, startDateTime, endDateTime))
                .thenReturn(List.of(expense));
        when(transferRepository.findByAccountFrom_User_UserIdAndCreatedAtBetween(any(), any(), any()))
                .thenReturn(Collections.emptyList());

        Map<String, Object> report = reportService.generateReport(userId, startDateTime, endDateTime);
        Map<String, BigDecimal> expenseByCategory = (Map<String, BigDecimal>) report.get("expenseByCategory");

        assertEquals(0, expenseByCategory.get("Uncategorized").compareTo(new BigDecimal("75.00")));
    }

    private static CashFlow flow(long id, CashFlowType type, BigDecimal amount, LocalDateTime date, Long acctId, Long catId) {
        CashFlow f = new CashFlow();
        f.setCashFlowId(id);
        f.setType(type);
        f.setAmount(amount);
        f.setOccurredAt(date);
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
}
