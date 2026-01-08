package com.example.budgettracker.repository;

import com.example.budgettracker.model.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    /**
     * FR-14: Find all active subscriptions for a user
     */
    List<Subscription> findByUser_UserIdAndIsActiveTrue(Long userId);

    /**
     * FR-14: Find all subscriptions (active and inactive) for a user
     */
    List<Subscription> findByUser_UserIdOrderByNextPostAtAsc(Long userId);

    /**
     * FR-14: Count active subscriptions for a user
     */
    long countByUser_UserIdAndIsActiveTrue(Long userId);

    List<Subscription> findByIsActiveTrueAndNextPostAtLessThanEqual(LocalDateTime date);

}
