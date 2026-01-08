package com.example.budgettracker.mockdata;

import com.example.budgettracker.model.*;
import com.example.budgettracker.model.enums.AccountType;
import com.example.budgettracker.model.enums.CashFlowType;
import com.example.budgettracker.repository.*;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;

@Component
public class TestDataInitializer {
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final CashFlowRepository cashFlowRepository;
    private final CategoryBudgetRepository categoryBudgetRepository;
    private final ItemRepository itemRepository;

    public TestDataInitializer(UserRepository userRepository, AccountRepository accountRepository, CategoryRepository categoryRepository, CashFlowRepository cashFlowRepository, CategoryBudgetRepository categoryBudgetRepository, ItemRepository itemRepository) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.categoryRepository = categoryRepository;
        this.cashFlowRepository = cashFlowRepository;
        this.categoryBudgetRepository = categoryBudgetRepository;
        this.itemRepository = itemRepository;
    }

    private void ensureItem(String name, long price, long stockQty) {
        itemRepository.findByItemNameIgnoreCase(name)
                .orElseGet(() -> itemRepository.save(Item.builder()
                        .itemName(name)
                        .price(price)
                        .stockQty(stockQty)
                        .build()));
    }


    private Category getOrCreateCategory(User user, String name, boolean system) {
        return categoryRepository.findByUser_UserIdAndNameIgnoreCase(user.getUserId(), name)
                .map(c -> {
                    if (system && !Boolean.TRUE.equals(c.isSystem())) {
                        c.setSystem(true);
                        return categoryRepository.save(c);
                    }
                    return c;
                })
                .orElseGet(() -> {
                    Category c = new Category();
                    c.setUser(user);
                    c.setName(name);
                    c.setSystem(system);
                    return categoryRepository.save(c);
                });
    }

    private void ensureSystemCategoriesFor(User user) {
        List<String> defaults = List.of("Food", "Transport", "Shopping", "Utilities", "Entertainment");
        for (String n : defaults) {
            getOrCreateCategory(user, n, true);
        }
    }

    @PostConstruct
    public void init() {
        User savedUser = userRepository.findByEmail("test@example.com")
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setEmail("test@example.com");
                    newUser.setHashedPassword("hashed123");
                    newUser.setFirstName("Test");
                    newUser.setMiddleName(null);
                    newUser.setLastName("User");
                    newUser.setAllowNotificationEmail(false);
                    newUser.setBudgetCoin(0L);
                    return userRepository.save(newUser);
                });

        Account cashAccount = accountRepository.findFirstByUser_UserIdAndAccountType(
                savedUser.getUserId(), AccountType.CASH
        ).orElseGet(() -> {
            Account mockAccount = new Account();
            mockAccount.setName("Test Account");
            mockAccount.setAccountType(AccountType.CASH);
            mockAccount.setUser(savedUser);
            mockAccount.setBalance(BigDecimal.ZERO);
            accountRepository.save(mockAccount);
            if (mockAccount.getAccountId() == null) {
                throw new IllegalStateException("Account id not generated after save()");
            }

            CashFlow opening = new CashFlow();
            opening.setType(CashFlowType.Income);
            opening.setAmount(new BigDecimal("1000.00"));
            opening.setOccurredAt(LocalDateTime.now());
            opening.setDescription("Opening Balance");
            opening.setAccount(mockAccount);
            cashFlowRepository.save(opening);

            return mockAccount;
        });


        Account goalAccount = accountRepository.findFirstByUser_UserIdAndAccountType(
                savedUser.getUserId(), AccountType.GOAL
        ).orElseGet(() -> {
            Account gAcc = new Account();
            gAcc.setUser(savedUser);

            gAcc.setName("Main Goal");

            gAcc.setAccountType(AccountType.GOAL);
            gAcc.setBalance(BigDecimal.ZERO);
            accountRepository.save(gAcc);
            if (gAcc.getAccountId() == null) {
                throw new IllegalStateException("Goal account id not generated after save()");
            }
            return gAcc;
        });


        ensureSystemCategoriesFor(savedUser);


        // Categorized
        Category play = getOrCreateCategory(savedUser, "Play", false);

        Category food = categoryRepository
                .findByUser_UserIdAndNameIgnoreCase(savedUser.getUserId(), "Food")
                .orElseThrow();


        YearMonth ym = YearMonth.now();
        categoryBudgetRepository.findByUser_UserIdAndCategory_CategoryIdAndYearMonth(
                        savedUser.getUserId(), play.getCategoryId(), ym.toString())
                .orElseGet(() -> {
                    CategoryBudget cb = new CategoryBudget();
                    cb.setUser(savedUser);
                    cb.setCategory(play);
                    cb.setYearMonth(ym.toString());
                    cb.setAmount(new BigDecimal("200.00"));
                    return categoryBudgetRepository.save(cb);
                });

        // Current Month Budget for food
        categoryBudgetRepository.findByUser_UserIdAndCategory_CategoryIdAndYearMonth(
                        savedUser.getUserId(), food.getCategoryId(), ym.toString())
                .orElseGet(() -> {
                    CategoryBudget cb = new CategoryBudget();
                    cb.setUser(savedUser);
                    cb.setCategory(food);
                    cb.setYearMonth(ym.toString());
                    cb.setAmount(new BigDecimal("300.00"));
                    return categoryBudgetRepository.save(cb);
                });

        // Two expend records for the month
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        LocalDateTime rangeStart = start.atStartOfDay();
        LocalDateTime rangeEnd = end.atTime(LocalTime.MAX);
        boolean hasThisMonthExpenses = cashFlowRepository
                .existsByAccount_User_UserIdAndOccurredAtBetween(
                        savedUser.getUserId(), rangeStart, rangeEnd);

        if (!hasThisMonthExpenses) {
            CashFlow e1 = new CashFlow();
            e1.setType(CashFlowType.Expense);
            e1.setAmount(new BigDecimal("20.50"));
            e1.setOccurredAt(ym.atDay(5).atStartOfDay());
            e1.setDescription("Coffee & snacks");
            e1.setAccount(cashAccount);
            e1.setCategory(play);

            CashFlow e2 = new CashFlow();
            e2.setType(CashFlowType.Expense);
            e2.setAmount(new BigDecimal("45.00"));
            e2.setOccurredAt(ym.atDay(12).atStartOfDay());
            e2.setDescription("Lunch");
            e2.setAccount(cashAccount);
            e2.setCategory(food);

            cashFlowRepository.saveAll(List.of(e1, e2));
        }

        List.of(
                new Object[]{"Spotify Premium", 450L, 250L},
                new Object[]{"Adobe Creative Suite", 800L, 100L},
                new Object[]{"Steam Wallet", 650L, 200L},
                new Object[]{"Kindle Unlimited", 300L, 300L},
                new Object[]{"Apple Music", 400L, 300L},
                new Object[]{"NordVPN (1 Year)", 1200L, 150L},
                new Object[]{"Starbucks Gift Card $10", 280L, 400L},
                new Object[]{"McDonald's Voucher $15", 350L, 250L},
                new Object[]{"Wireless Earbuds", 1800L, 80L},
                new Object[]{"Phone Case Premium", 600L, 180L},
                new Object[]{"Movie Tickets (2x)", 750L, 120L},
                new Object[]{"Gym Day Pass", 200L, 220L},
                new Object[]{"Bowling Night", 900L, 140L},
                new Object[]{"OVER-SAVE Premium", 500L, 500L},
                new Object[]{"Analytics Pack", 300L, 500L},
                new Object[]{"Theme Pack", 250L, 500L},
                new Object[]{"Gift Card", 100L, 400L},
                new Object[]{"Coffee Mug", 50L, 600L},
                new Object[]{"T-Shirt", 200L, 200L}
        ).forEach(entry -> ensureItem((String) entry[0], (Long) entry[1], (Long) entry[2]));

        System.out.println("✅ Mock User ID: " + savedUser.getUserId());
        System.out.println("✅ Mock Cash Account ID: " + cashAccount.getAccountId());
        System.out.println("✅ Category(Play) ID: " + play.getCategoryId());
        System.out.println("✅ Mock Goal Account ID: " + goalAccount.getAccountId());
    }
}
