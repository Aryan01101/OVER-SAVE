package com.example.budgettracker.repository;

import com.example.budgettracker.model.Goal;
import com.example.budgettracker.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface GoalRepository extends JpaRepository<Goal, Long> {
    List<Goal> findByUser(User user);

    boolean existsByUser_UserIdAndNameIgnoreCase(Long userId, String name);


    List<Goal> findAllByUser_UserIdOrderByIdDesc(Long userId);
}
