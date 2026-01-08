package com.example.budgettracker.service;



import com.example.budgettracker.model.Category;
import com.example.budgettracker.model.Transfer;
import com.example.budgettracker.repository.CashFlowRepository;
import com.example.budgettracker.repository.CategoryRepository;
import com.example.budgettracker.model.CashFlow;
import com.example.budgettracker.repository.TransferRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private final CashFlowRepository cashFlowRepository;
    private final CategoryRepository categoryRepository;
    private final TransferRepository transferRepository;

    public ReportService(CashFlowRepository cashFlowRepository,
                         CategoryRepository categoryRepository,
                         TransferRepository transferRepository) {
        this.cashFlowRepository = cashFlowRepository;
        this.categoryRepository = categoryRepository;
        this.transferRepository = transferRepository;
    }

    public Map<String, Object> generateReport(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        LocalDateTime startDateTime = startDate;
        LocalDateTime endDateTime = endDate;
        List<CashFlow> cashFlows = cashFlowRepository
                .findByAccount_User_UserIdAndOccurredAtBetween(userId, startDateTime, endDateTime);


        List<CashFlow> incomes = cashFlows.stream()
                .filter(cf -> "Income".equalsIgnoreCase(String.valueOf(cf.getType())))
                .toList();

        List<CashFlow> expenses = cashFlows.stream()
                .filter(cf -> "Expense".equalsIgnoreCase(String.valueOf(cf.getType())))
                .toList();

        List<Transfer> transfers = transferRepository
                .findByAccountFrom_User_UserIdAndCreatedAtBetween(userId, startDate, endDate);


        BigDecimal totalTransfer = transfers.stream()
                .map(Transfer::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);        // 计算总收入
        BigDecimal totalIncome = incomes.stream()
                .map(CashFlow::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);


        BigDecimal totalExpense = expenses.stream()
                .map(CashFlow::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);


        BigDecimal balance = totalIncome.subtract(totalExpense);


        Map<Long, BigDecimal> totalsByCategoryId = expenses.stream()
                .collect(Collectors.groupingBy(
                        cf -> cf.getCategory() != null ? cf.getCategory().getCategoryId() : -1L,
                        Collectors.mapping(CashFlow::getAmount,
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
                ));


        Map<String, BigDecimal> expenseByCategory = new LinkedHashMap<>();
        totalsByCategoryId.forEach((categoryId, total) -> {
            String name;
            if (categoryId == -1L) {
                name = "Uncategorized";
            } else {
                name = categoryRepository.findById(categoryId)
                        .map(Category::getName)
                        .orElse("Unknown");
            }
            expenseByCategory.put(name, total);
        });

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("totalIncome", totalIncome);
        report.put("totalExpense", totalExpense);
        report.put("balance", balance);
        report.put("expenseByCategory", expenseByCategory);
        report.put("transfer", totalTransfer);

        System.out.println("===== Financial Report =====");
        System.out.println("Total Income: " + totalIncome);
        System.out.println("Total Expense: " + totalExpense);
        System.out.println("Balance: " + balance);
        expenseByCategory.forEach((name, total) -> {
            System.out.println("Category: " + name + " | Expense: " + total);
        });
        System.out.println("============================");

        return report;
    }

}
