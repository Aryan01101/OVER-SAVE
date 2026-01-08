package com.example.budgettracker.repository;

import com.example.budgettracker.model.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface TransferRepository extends JpaRepository<Transfer, Long> {
    List<Transfer> findByAccountFrom_User_UserIdAndCreatedAtBetween(Long userId, LocalDateTime start, LocalDateTime end);
}
