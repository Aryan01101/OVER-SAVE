package com.example.budgettracker.repository;

import com.example.budgettracker.model.LoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {

    @Query("SELECT la FROM LoginAttempt la WHERE la.email = :email AND la.attemptTime > :since AND la.successful = false")
    List<LoginAttempt> findFailedAttemptsSince(@Param("email") String email, @Param("since") LocalDateTime since);

    @Query("SELECT la FROM LoginAttempt la WHERE la.ipAddress = :ipAddress AND la.attemptTime > :since AND la.successful = false")
    List<LoginAttempt> findFailedAttemptsByIpSince(@Param("ipAddress") String ipAddress, @Param("since") LocalDateTime since);
}