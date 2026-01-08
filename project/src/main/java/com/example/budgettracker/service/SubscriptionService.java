package com.example.budgettracker.service;

import com.example.budgettracker.dto.SubscriptionRequest;
import com.example.budgettracker.dto.SubscriptionResponse;

import java.util.List;

/**
 * FR-10: Recurring Subscriptions
 * The system shall allow users to manage recurring subscription expenses.
 */
public interface SubscriptionService {
    SubscriptionResponse create(Long userId, SubscriptionRequest req);
    List<SubscriptionResponse> list(Long userId, boolean activeOnly);
    SubscriptionResponse update(Long userId, Long id, SubscriptionRequest req);
    void pause(Long userId, Long id);
    void resume(Long userId, Long id);
    void delete(Long userId, Long id);
}
