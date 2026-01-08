package com.example.budgettracker.repository;

import com.example.budgettracker.model.PasswordResetToken;
import com.example.budgettracker.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    /**
     * Find a valid (unused and not expired) token
     */
    @Query("SELECT prt FROM PasswordResetToken prt WHERE prt.token = :token AND prt.used = false AND prt.expiresAt > :now")
    Optional<PasswordResetToken> findValidToken(@Param("token") String token, @Param("now") LocalDateTime now);

    /**
     * Find token by token string (regardless of validity)
     */
    Optional<PasswordResetToken> findByToken(String token);

    /**
     * Find all tokens for a user
     */
    List<PasswordResetToken> findByUserOrderByCreatedAtDesc(User user);

    /**
     * Delete expired tokens
     */
    @Modifying
    @Query("DELETE FROM PasswordResetToken prt WHERE prt.expiresAt < :now")
    int deleteExpiredTokens(@Param("now") LocalDateTime now);

    /**
     * Mark all existing tokens for a user as used (for security when password is reset)
     */
    @Modifying
    @Query("UPDATE PasswordResetToken prt SET prt.used = true WHERE prt.user = :user AND prt.used = false")
    int invalidateAllTokensForUser(@Param("user") User user);

    /**
     * Count valid tokens for a user (to prevent spam)
     */
    @Query("SELECT COUNT(prt) FROM PasswordResetToken prt WHERE prt.user = :user AND prt.used = false AND prt.expiresAt > :now")
    int countValidTokensForUser(@Param("user") User user, @Param("now") LocalDateTime now);

    /**
     * Find expired tokens
     */
    @Query("SELECT prt FROM PasswordResetToken prt WHERE prt.expiresAt < :now")
    List<PasswordResetToken> findExpiredTokens(@Param("now") LocalDateTime now);
}