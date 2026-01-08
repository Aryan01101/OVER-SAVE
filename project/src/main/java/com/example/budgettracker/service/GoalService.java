package com.example.budgettracker.service;

import com.example.budgettracker.dto.Goal.*;
import com.example.budgettracker.model.CashFlow;

import java.time.LocalDate;
import java.util.List;

public interface GoalService {
    GoalResponse create(Long userId, CreateGoalRequest request);
    GoalResponse createGoal(Long userId, GoalRequest request);
    List<GoalResponse> list(Long userId);
    List<GoalResponse> getAllGoals(Long userId);
    GoalResponse get(Long userId, Long goalId);
    GoalResponse getGoalById(Long userId, Long goalId);
    GoalResponse update(Long userId, Long goalId, UpdateGoalRequest request);
    GoalResponse updateGoal(Long userId, Long goalId, GoalRequest request);
    void delete(Long userId, Long goalId);
    void deleteGoal(Long userId, Long goalId);
    GoalResponse contribute(Long userId, Long goalId, ContributionRequest request);
    ContributionResponse contributeToGoal(Long userId, ContributionRequest request);
    List<CashFlow> getContributions(Long userId, Long goalId, LocalDate from, LocalDate to);
}

