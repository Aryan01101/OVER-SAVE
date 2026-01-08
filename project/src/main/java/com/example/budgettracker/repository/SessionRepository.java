package com.example.budgettracker.repository;

import com.example.budgettracker.model.Session;
import com.example.budgettracker.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SessionRepository extends JpaRepository<Session, Long> {

    Optional<Session> findBySessionToken(String sessionToken);

    @Query("SELECT s FROM Session s WHERE s.sessionToken = :token AND s.expiresAt > :now AND s.isActive = true")
    Optional<Session> findValidSessionByToken(@Param("token") String token, @Param("now") LocalDateTime now);

    @Query("SELECT s FROM Session s WHERE s.user.userId = :userId AND s.isActive = true")
    List<Session> findActiveSessionsByUserId(@Param("userId") Long userId);

    @Query("SELECT s FROM Session s WHERE (s.expiresAt < :now OR s.lastActivityAt < :idleCutoff) AND s.isActive = true")
    List<Session> findExpiredSessions(@Param("now") LocalDateTime now, @Param("idleCutoff") LocalDateTime idleCutoff);

    @Query("SELECT s FROM Session s WHERE s.expiresAt < :now OR s.lastActivityAt < :idleCutoff")
    List<Session> findExpiredSessions(@Param("now") LocalDateTime now);

    @Query("SELECT s FROM Session s WHERE s.sessionToken = :token AND s.expiresAt > :now AND s.lastActivityAt > :idleCutoff AND s.isActive = true")
    Optional<Session> findValidActiveSession(@Param("token") String token, @Param("now") LocalDateTime now, @Param("idleCutoff") LocalDateTime idleCutoff);

    @Modifying
    @Query("DELETE FROM Session s WHERE s.user = :user")
    void deleteAllByUser(@Param("user") User user);

    @Modifying
    @Query("DELETE FROM Session s WHERE s.expiresAt < :now OR s.lastActivityAt < :idleCutoff")
    void deleteExpiredSessions(@Param("now") LocalDateTime now, @Param("idleCutoff") LocalDateTime idleCutoff);

    @Modifying
    @Query("UPDATE Session s SET s.isActive = false WHERE s.expiresAt < :now OR s.lastActivityAt < :idleCutoff")
    void deactivateExpiredSessions(@Param("now") LocalDateTime now, @Param("idleCutoff") LocalDateTime idleCutoff);
}