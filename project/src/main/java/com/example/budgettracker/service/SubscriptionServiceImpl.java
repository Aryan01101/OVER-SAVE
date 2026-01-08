package com.example.budgettracker.service;

import com.example.budgettracker.dto.SubscriptionRequest;
import com.example.budgettracker.dto.SubscriptionResponse;
import com.example.budgettracker.model.Subscription;
import com.example.budgettracker.model.User;
import com.example.budgettracker.repository.SubscriptionRepository;
import com.example.budgettracker.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;

    public SubscriptionServiceImpl(SubscriptionRepository subscriptionRepository, UserRepository userRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.userRepository = userRepository;
    }

    @Override
    public SubscriptionResponse create(Long userId, SubscriptionRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        // Determine the first posting date/time — if not explicitly given, use startDate
        LocalDateTime firstPost = req.getFirstPostAt() != null ? req.getFirstPostAt() : req.getStartDate();

        Subscription s = Subscription.builder()
                .merchant(req.getMerchant())
                .amount(req.getAmount())
                .frequency(req.getFrequency().toUpperCase())
                .startDate(req.getStartDate())
                .isActive(req.getIsActive() == null ? Boolean.TRUE : req.getIsActive())
                .nextPostAt(firstPost)
                .user(user)
                .build();

        s = subscriptionRepository.save(s);
        return toResponse(s);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionResponse> list(Long userId, boolean activeOnly) {
        List<Subscription> subs = activeOnly
                ? subscriptionRepository.findByUser_UserIdAndIsActiveTrue(userId)
                : subscriptionRepository.findByUser_UserIdOrderByNextPostAtAsc(userId);
        return subs.stream().map(this::toResponse).toList();
    }

    @Override
    public SubscriptionResponse update(Long userId, Long id, SubscriptionRequest req) {
        Subscription s = getOwned(userId, id);

        s.setMerchant(req.getMerchant());
        s.setAmount(req.getAmount());
        s.setFrequency(req.getFrequency().toUpperCase());
        s.setStartDate(req.getStartDate());

        if (req.getFirstPostAt() != null)
            s.setNextPostAt(req.getFirstPostAt());

        if (req.getIsActive() != null)
            s.setIsActive(req.getIsActive());

        return toResponse(s);
    }

    @Override
    public void pause(Long userId, Long id) {
        Subscription s = getOwned(userId, id);
        s.setIsActive(false);
    }

    @Override
    public void resume(Long userId, Long id) {
        Subscription s = getOwned(userId, id);
        s.setIsActive(true);

        // If somehow nextPostAt was null, set it to now
        if (s.getNextPostAt() == null)
            s.setNextPostAt(LocalDateTime.now());
    }

    @Override
    public void delete(Long userId, Long id) {
        Subscription s = getOwned(userId, id);
        subscriptionRepository.delete(s);
    }

    private Subscription getOwned(Long userId, Long id) {
        Subscription s = subscriptionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Subscription not found"));
        if (!s.getUser().getUserId().equals(userId)) {
            throw new EntityNotFoundException("Subscription not found"); // hide ownership
        }
        return s;
    }

    private SubscriptionResponse toResponse(Subscription s) {
        return new SubscriptionResponse(
                s.getSubscriptionId(),
                s.getMerchant(),
                s.getAmount(),
                s.getFrequency(),
                s.getStartDate(),
                s.getIsActive(),
                s.getNextPostAt(),
                monthlyEquivalent(s.getAmount(), s.getFrequency())
        );
    }

    private BigDecimal monthlyEquivalent(BigDecimal amount, String freqRaw) {
        String f = freqRaw == null ? "" : freqRaw.trim().toUpperCase();
        BigDecimal monthlyAmount = switch (f) {
            case "WEEKLY" -> amount.multiply(BigDecimal.valueOf(52.0 / 12.0));
            case "FORTNIGHTLY" -> amount.multiply(BigDecimal.valueOf(26.0 / 12.0));
            case "MONTHLY" -> amount;
            case "QUARTERLY" -> amount.divide(BigDecimal.valueOf(3), 6, RoundingMode.HALF_UP);
            case "YEARLY", "ANNUAL", "ANNUALLY" ->
                    amount.divide(BigDecimal.valueOf(12), 6, RoundingMode.HALF_UP);
            default -> amount; // fallback treat as monthly
        };
        return monthlyAmount.setScale(2, RoundingMode.HALF_UP);
    }
}
