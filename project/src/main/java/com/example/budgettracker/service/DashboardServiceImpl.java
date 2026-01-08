package com.example.budgettracker.service;

import com.example.budgettracker.dto.BudgetSummaryResponse;
import com.example.budgettracker.dto.Dashboard.*;
import com.example.budgettracker.dto.Goal.GoalResponse;
import com.example.budgettracker.model.CashFlow;
import com.example.budgettracker.model.Category;
import com.example.budgettracker.model.Goal;
import com.example.budgettracker.model.Subscription;
import com.example.budgettracker.model.enums.CashFlowType;
import com.example.budgettracker.model.enums.GoalStatus;
import com.example.budgettracker.repository.CashFlowRepository;
import com.example.budgettracker.repository.CategoryRepository;
import com.example.budgettracker.repository.GoalRepository;
import com.example.budgettracker.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

/**
 * FR-14: Dashboard Service Implementation
 * Aggregates data from existing tables (no ERD changes needed)
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final CashFlowRepository cashFlowRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final GoalRepository goalRepository;
    private final CategoryRepository categoryRepository;
    private final BudgetService budgetService;

    @Override
    public DashboardResponse getDashboardData(Long userId) {
        log.info("FR-14: Fetching complete dashboard data for user {}", userId);

        try {
            // Fetch all components
            FinancialAggregatesResponse financialAggregates = getFinancialAggregates(userId);
            List<BudgetSummaryResponse> budgets = getBudgetSummaries(userId);
            List<SubscriptionSummaryResponse> subscriptions = getActiveSubscriptions(userId);
            List<TransactionResponse> recentTransactions = getRecentTransactions(userId, 10);
            SpendingTrendData spendingTrend = getSpendingTrend(userId, "WEEK");
            List<GoalResponse> savingsGoals = getActiveSavingsGoals(userId);

            return DashboardResponse.builder()
                    .financialAggregates(financialAggregates)
                    .budgets(budgets)
                    .subscriptions(subscriptions)
                    .recentTransactions(recentTransactions)
                    .spendingTrend(spendingTrend)
                    .savingsGoals(savingsGoals)
                    .dataAvailable(true)
                    .build();

        } catch (Exception e) {
            log.error("FR-14: Error fetching dashboard data for user {}", userId, e);
            // Return fallback response (FR-14 requirement)
            return DashboardResponse.builder()
                    .dataAvailable(false)
                    .message("Unable to load dashboard data. Please try again later.")
                    .build();
        }
    }

    @Override
    public FinancialAggregatesResponse getFinancialAggregates(Long userId) {
        log.info("FR-14: Calculating financial aggregates for user {} (all-time)", userId);

        // Query all-time aggregates (no date filtering)
        BigDecimal totalIncome = cashFlowRepository.sumAllIncomeByUser(userId);
        BigDecimal totalExpenses = cashFlowRepository.sumAllExpenseByUser(userId);
        BigDecimal currentBalance = cashFlowRepository.computeTotalBalanceByUserId(userId);

        log.info("FR-14: User {} - Total Income: {}, Total Expenses: {}, Balance: {}",
                userId, totalIncome, totalExpenses, currentBalance);

        // Calculate savings rate (all-time)
        Double savingsRate = 0.0;
        if (totalIncome.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal saved = totalIncome.subtract(totalExpenses);
            savingsRate = saved.divide(totalIncome, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();
        }

        // Calculate total savings from goals
        List<Goal> goals = goalRepository.findAllByUser_UserIdOrderByIdDesc(userId);
        BigDecimal totalSavings = goals.stream()
                .map(Goal::getCurrentAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate average goals progress
        Double goalsProgressPercent = goals.isEmpty() ? 0.0 :
                goals.stream()
                        .mapToDouble(goal -> {
                            if (goal.getTargetAmount() == null || goal.getTargetAmount().compareTo(BigDecimal.ZERO) == 0) {
                                return 0.0;
                            }
                            return goal.getCurrentAmount()
                                    .divide(goal.getTargetAmount(), 4, java.math.RoundingMode.HALF_UP)
                                    .multiply(BigDecimal.valueOf(100))
                                    .doubleValue();
                        })
                        .average()
                        .orElse(0.0);

        log.info("FR-14: User {} - Savings Rate: {}%, Total Savings from Goals: {}, Goals Progress: {}%",
                userId, String.format("%.1f", savingsRate), totalSavings, String.format("%.1f", goalsProgressPercent));

        // Using same field names for backward compatibility with frontend
        FinancialAggregatesResponse response = FinancialAggregatesResponse.builder()
                .monthlyIncome(totalIncome)
                .monthlyExpenses(totalExpenses)
                .currentBalance(currentBalance)
                .totalSavings(totalSavings)
                .incomeChangePercent(null)  // No comparison for all-time data
                .expenseChangePercent(null)
                .savingsRate(savingsRate)
                .goalsProgressPercent(goalsProgressPercent)
                .build();

        // FR-14: Validate numeric values
        response.validateNumericValues();
        return response;
    }

    @Override
    public SpendingTrendData getSpendingTrend(Long userId, String period) {
        log.info("FR-14: Fetching spending trend for user {} with period {}", userId, period);

        LocalDate now = LocalDate.now();
        LocalDate startDate;
        List<SpendingTrendData.DataPoint> dataPoints = new ArrayList<>();

        switch (period.toUpperCase()) {
            case "WEEK":
                startDate = now.minusDays(6); // Last 7 days including today
                dataPoints = generateWeeklyTrend(userId, startDate, now);
                break;
            case "MONTH":
                startDate = now.withDayOfMonth(1);
                dataPoints = generateMonthlyTrend(userId, startDate, now);
                break;
            case "YEAR":
                startDate = now.withDayOfYear(1);
                dataPoints = generateYearlyTrend(userId, startDate, now);
                break;
            default:
                startDate = now.minusDays(6);
                dataPoints = generateWeeklyTrend(userId, startDate, now);
        }

        return SpendingTrendData.builder()
                .period(period.toUpperCase())
                .dataPoints(dataPoints)
                .build();
    }

    // ===== Private Helper Methods =====

    private List<BudgetSummaryResponse> getBudgetSummaries(Long userId) {
        YearMonth currentMonth = YearMonth.now();
        List<Category> categories = categoryRepository.findByUser_UserIdOrderByNameAsc(userId);

        return categories.stream()
                .map(category -> {
                    try {
                        return budgetService.getUserCategoryBudgetSummary(userId, category.getCategoryId(), currentMonth);
                    } catch (Exception e) {
                        log.warn("Could not fetch budget for category {}", category.getName());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .filter(budget -> budget.getBudget() != null && budget.getBudget().compareTo(BigDecimal.ZERO) > 0)
                .limit(5) // Top 5 budgets for dashboard
                .collect(Collectors.toList());
    }

    private List<SubscriptionSummaryResponse> getActiveSubscriptions(Long userId) {
        List<Subscription> subscriptions = subscriptionRepository.findByUser_UserIdAndIsActiveTrue(userId);

        return subscriptions.stream()
                .map(sub -> {
                    BigDecimal monthlyEquiv = calculateMonthlyEquivalent(sub.getAmount(), sub.getFrequency());
                    return SubscriptionSummaryResponse.builder()
                            .subscriptionId(sub.getSubscriptionId())
                            .merchant(sub.getMerchant())
                            .amount(sub.getAmount())
                            .frequency(sub.getFrequency())
                            .nextPostAt(sub.getNextPostAt())
                            .isActive(sub.getIsActive())
                            .monthlyEquivalent(monthlyEquiv)
                            .build();
                })
                .limit(5) // Top 5 subscriptions for dashboard
                .collect(Collectors.toList());
    }

    private List<TransactionResponse> getRecentTransactions(Long userId, int limit) {
        List<CashFlow> cashFlows = cashFlowRepository.findRecentByUserId(userId);

        return cashFlows.stream()
                .limit(limit)
                .map(cf -> TransactionResponse.builder()
                        .cashFlowId(cf.getCashFlowId())
                        .type(cf.getType())
                        .amount(cf.getAmount())
                        .description(cf.getDescription())
                        .occurredAt(cf.getOccurredAt())
                        .createdAt(cf.getCreatedAt())
                        .categoryName(cf.getCategory() != null ? cf.getCategory().getName() : "Uncategorized")
                        .categoryIcon(getCategoryIcon(cf.getCategory()))
                        .isSubscription(cf.getSubscription() != null)
                        .subscriptionMerchant(cf.getSubscription() != null ? cf.getSubscription().getMerchant() : null)
                        .build())
                .collect(Collectors.toList());
    }

    private List<GoalResponse> getActiveSavingsGoals(Long userId) {
        List<Goal> goals = goalRepository.findAllByUser_UserIdOrderByIdDesc(userId);

        return goals.stream()
                .filter(goal -> goal.getStatus() == GoalStatus.IN_PROGRESS || goal.getStatus().name().equals("ACTIVE"))
                .limit(4) // Top 4 goals for dashboard
                .map(goal -> {
                    GoalResponse response = new GoalResponse();
                    response.setId(goal.getId());
                    response.setName(goal.getName());
                    response.setTargetAmount(goal.getTargetAmount());
                    response.setDueDate(goal.getDueDate());
                    response.setCurrentAmount(goal.getCurrentAmount());

                    // Calculate progress percentage
                    double progress = 0.0;
                    if (goal.getTargetAmount() != null && goal.getTargetAmount().compareTo(BigDecimal.ZERO) > 0) {
                        progress = goal.getCurrentAmount()
                                .divide(goal.getTargetAmount(), 4, java.math.RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(100))
                                .doubleValue();
                    }
                    response.setProgress(progress);
                    response.setStatus(goal.getStatus().name());

                    response.setAccountId(goal.getAccount() != null ? goal.getAccount().getAccountId() : null);
                    response.setCategoryId(null); // Goal doesn't have a category field

                    return response;
                })
                .collect(Collectors.toList());
    }

    private List<SpendingTrendData.DataPoint> generateWeeklyTrend(Long userId, LocalDate start, LocalDate end) {
        List<SpendingTrendData.DataPoint> dataPoints = new ArrayList<>();
        List<CashFlow> cashFlows = cashFlowRepository.findByUserIdAndDateRange(
                userId,
                start.atStartOfDay(),
                end.atTime(LocalTime.MAX)
        );

        // Group by date
        Map<LocalDate, BigDecimal> dailyExpenses = cashFlows.stream()
                .filter(cf -> cf.getType() == CashFlowType.Expense)
                .collect(Collectors.groupingBy(
                        cf -> cf.getOccurredAt().toLocalDate(),
                        Collectors.reducing(BigDecimal.ZERO, CashFlow::getAmount, BigDecimal::add)
                ));

        // Generate data points for each day
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            BigDecimal amount = dailyExpenses.getOrDefault(date, BigDecimal.ZERO);
            dataPoints.add(SpendingTrendData.DataPoint.builder()
                    .label(date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH))
                    .date(date)
                    .amount(amount)
                    .isToday(date.equals(LocalDate.now()))
                    .build());
        }

        return dataPoints;
    }

    private List<SpendingTrendData.DataPoint> generateMonthlyTrend(Long userId, LocalDate start, LocalDate end) {
        // Similar to weekly but grouped by week or day-of-month
        List<SpendingTrendData.DataPoint> dataPoints = new ArrayList<>();
        List<CashFlow> cashFlows = cashFlowRepository.findByUserIdAndDateRange(
                userId,
                start.atStartOfDay(),
                end.atTime(LocalTime.MAX)
        );

        Map<LocalDate, BigDecimal> dailyExpenses = cashFlows.stream()
                .filter(cf -> cf.getType() == CashFlowType.Expense)
                .collect(Collectors.groupingBy(
                        cf -> cf.getOccurredAt().toLocalDate(),
                        Collectors.reducing(BigDecimal.ZERO, CashFlow::getAmount, BigDecimal::add)
                ));

        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            BigDecimal amount = dailyExpenses.getOrDefault(date, BigDecimal.ZERO);
            dataPoints.add(SpendingTrendData.DataPoint.builder()
                    .label(String.valueOf(date.getDayOfMonth()))
                    .date(date)
                    .amount(amount)
                    .isToday(date.equals(LocalDate.now()))
                    .build());
        }

        return dataPoints;
    }

    private List<SpendingTrendData.DataPoint> generateYearlyTrend(Long userId, LocalDate start, LocalDate end) {
        List<SpendingTrendData.DataPoint> dataPoints = new ArrayList<>();
        List<CashFlow> cashFlows = cashFlowRepository.findByUserIdAndDateRange(
                userId,
                start.atStartOfDay(),
                end.atTime(LocalTime.MAX)
        );

        // Group by month
        Map<YearMonth, BigDecimal> monthlyExpenses = cashFlows.stream()
                .filter(cf -> cf.getType() == CashFlowType.Expense)
                .collect(Collectors.groupingBy(
                        cf -> YearMonth.from(cf.getOccurredAt()),
                        Collectors.reducing(BigDecimal.ZERO, CashFlow::getAmount, BigDecimal::add)
                ));

        YearMonth currentMonth = YearMonth.from(end);
        for (int i = 0; i < 12; i++) {
            YearMonth month = YearMonth.from(start).plusMonths(i);
            if (month.isAfter(currentMonth)) break;

            BigDecimal amount = monthlyExpenses.getOrDefault(month, BigDecimal.ZERO);
            dataPoints.add(SpendingTrendData.DataPoint.builder()
                    .label(month.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH))
                    .date(month.atDay(1))
                    .amount(amount)
                    .isToday(month.equals(YearMonth.now()))
                    .build());
        }

        return dataPoints;
    }

    private Double calculatePercentageChange(BigDecimal oldValue, BigDecimal newValue) {
        if (oldValue.compareTo(BigDecimal.ZERO) == 0) {
            return newValue.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0;
        }

        return newValue.subtract(oldValue)
                .divide(oldValue, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }

    private BigDecimal calculateMonthlyEquivalent(BigDecimal amount, String frequency) {
        return switch (frequency.toUpperCase()) {
            case "WEEKLY" -> amount.multiply(BigDecimal.valueOf(4.33)); // Avg weeks per month
            case "MONTHLY" -> amount;
            case "YEARLY" -> amount.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
            default -> amount;
        };
    }

    private String getCategoryIcon(Category category) {
        if (category == null) return "💰";

        // Map category names to icons
        return switch (category.getName().toLowerCase()) {
            case "food", "dining" -> "🍔";
            case "transport", "transportation" -> "🚗";
            case "entertainment" -> "🎮";
            case "shopping" -> "🛍️";
            case "groceries" -> "🛒";
            case "education" -> "📚";
            case "health", "healthcare" -> "🏥";
            case "utilities" -> "💡";
            case "housing", "rent" -> "🏠";
            case "goal transfer" -> "🎯";
            default -> "💰";
        };
    }
}
