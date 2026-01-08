package com.example.budgettracker.repository;

import com.example.budgettracker.model.RewardGrant;
import com.example.budgettracker.model.RewardRedeem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

public interface RewardRedeemRepository extends JpaRepository<RewardRedeem, Long> {
    List<RewardRedeem> findByUserUserId(Long userId);
}
