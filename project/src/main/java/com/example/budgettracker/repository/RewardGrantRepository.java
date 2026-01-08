package com.example.budgettracker.repository;

import com.example.budgettracker.model.RewardGrant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RewardGrantRepository extends JpaRepository<RewardGrant, Long> {
    List<RewardGrant> findByUserUserId(Long userId);
}
