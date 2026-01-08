package com.example.budgettracker.repository;

import com.example.budgettracker.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String mail);

    @Modifying(clearAutomatically = true)
    @Query("update User u set u.allowNotificationEmail = :enabled where u.userId = :userId")
    int updateNotificationEmail(@Param("userId") Long userId, @Param("enabled") boolean enabled);

    @Query("select u.allowNotificationEmail from User u where u.userId = :userId")
    Boolean getNotificationEmailStatus(@Param("userId") Long userId);
}