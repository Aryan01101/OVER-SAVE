// com/example/budgettracker/config/SubscriptionPostingScheduler.java
package com.example.budgettracker.config;

import com.example.budgettracker.model.*;
import com.example.budgettracker.model.enums.AccountType;
import com.example.budgettracker.model.enums.CashFlowType;
import com.example.budgettracker.repository.AccountRepository;
import com.example.budgettracker.repository.CashFlowRepository;
import com.example.budgettracker.repository.CategoryRepository;
import com.example.budgettracker.repository.SubscriptionRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Component
public class SubscriptionPostingScheduler {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionPostingScheduler.class);
    private static final ZoneId SYD = ZoneId.of("Australia/Sydney");

    private final SubscriptionRepository subscriptionRepository;
    private final CashFlowRepository cashFlowRepository;
    private final CategoryRepository categoryRepository;
    private final AccountRepository accountRepository;

    public SubscriptionPostingScheduler(SubscriptionRepository subscriptionRepository,
                                        CashFlowRepository cashFlowRepository,
                                        CategoryRepository categoryRepository,
                                        AccountRepository accountRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.cashFlowRepository = cashFlowRepository;
        this.categoryRepository = categoryRepository;
        this.accountRepository = accountRepository;
    }

    /**
     * Runs daily at 02:15 local time. Picks all active subscriptions with nextPostAt <= today,
     * posts an Expense CashFlow for each due posting (catching up if the app was down),
     * and advances nextPostAt to the next future date.
     */
    @Transactional
    @Scheduled(cron = "0 15 2 * * *", zone = "Australia/Sydney")
    public void postDueSubscriptions() {
        LocalDateTime today = LocalDateTime.now(SYD);
        List<Subscription> due = subscriptionRepository.findByIsActiveTrueAndNextPostAtLessThanEqual(today);
        if (due.isEmpty()) {
            return;
        }

        log.info("Posting due subscriptions: count={}", due.size());

        for (Subscription s : due) {
            User user = s.getUser();

            // Ensure/resolve "Subscriptions" category for this user
            Category subsCategory = categoryRepository
                    .findByUser_UserIdAndNameIgnoreCase(user.getUserId(), "Subscriptions")
                    .orElseGet(() -> {
                        Category c = new Category();
                        c.setUser(user);
                        c.setName("Subscriptions");
                        c.setSystem(true);
                        return categoryRepository.save(c);
                    });

            // Resolve default CASH account (required by your domain)
            Account cashAccount = accountRepository
                    .findFirstByUser_UserIdAndAccountType(user.getUserId(), AccountType.CASH)
                    .orElseThrow(() ->
                            new IllegalStateException("No CASH account found for userId=" + user.getUserId()));

            // Post one or more times if nextPostAt lags behind today
            LocalDateTime postDate = s.getNextPostAt();
            int posted = 0;
            while (!postDate.isAfter(today)) {
                CashFlow cf = new CashFlow();
                cf.setType(CashFlowType.Expense);
                cf.setAmount(s.getAmount() != null ? s.getAmount() : BigDecimal.ZERO);
                cf.setOccurredAt(postDate);
                cf.setDescription("Subscription: " + s.getMerchant());
                cf.setAccount(cashAccount);
                cf.setCategory(subsCategory);
                cashFlowRepository.save(cf);

                postDate = increment(postDate, s.getFrequency());
                posted++;
            }

            s.setNextPostAt(postDate); // push to first future date
            // JPA will auto-flush on tx end since the entity is managed
            log.info("Posted {} time(s) for subId={} (merchant={}), nextPostAt -> {}",
                    posted, s.getSubscriptionId(), s.getMerchant(), postDate);
        }
    }

    private LocalDateTime increment(LocalDateTime date, String freqRaw) {
        String f = (freqRaw == null ? "" : freqRaw.trim().toUpperCase());
        return switch (f) {
            case "WEEKLY" -> date.plusWeeks(1);
            case "FORTNIGHTLY" -> date.plusWeeks(2);
            case "QUARTERLY" -> date.plusMonths(3);
            case "YEARLY", "ANNUAL", "ANNUALLY" -> date.plusYears(1);
            case "MONTHLY" -> date.plusMonths(1);
            default -> date.plusMonths(1); // safe fallback
        };
    }
}
