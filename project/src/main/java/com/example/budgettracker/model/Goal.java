package com.example.budgettracker.model;

import com.example.budgettracker.model.enums.GoalStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(
        name = "goal",
        uniqueConstraints = @UniqueConstraint(name = "uk_goal_user_name", columnNames = {"user_id","name"})
)
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Goal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(name = "target_amount", nullable = false)
    private BigDecimal targetAmount;


    @Builder.Default
    @Column(name = "saved_amount", nullable = false)
    private BigDecimal currentAmount = BigDecimal.ZERO;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private GoalStatus status = GoalStatus.IN_PROGRESS;
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToOne
    @JoinColumn(name = "account_id")
    private Account linkedAccount;

    @PrePersist
    public void prePersist() {
        if (currentAmount == null) currentAmount = BigDecimal.ZERO;
        if (status == null) status = GoalStatus.IN_PROGRESS;
    }

    // Alias methods for compatibility with DashboardServiceImpl
    public Long getGoalId() {
        return this.id;
    }

    public void setGoalId(Long goalId) {
        this.id = goalId;
    }

    public LocalDate getTargetDate() {
        return this.dueDate;
    }

    public void setTargetDate(LocalDate targetDate) {
        this.dueDate = targetDate;
    }

    public BigDecimal getSavedAmount() {
        return this.currentAmount;
    }

    public void setSavedAmount(BigDecimal savedAmount) {
        this.currentAmount = savedAmount;
    }

    public Account getAccount() {
        return this.linkedAccount;
    }

    public void setAccount(Account account) {
        this.linkedAccount = account;
    }

    public int getProgressPercent() {
        if (targetAmount == null || targetAmount.compareTo(BigDecimal.ZERO) == 0) {
            return 0;
        }
        if (currentAmount == null) {
            return 0;
        }
        return currentAmount.multiply(new BigDecimal(100))
                .divide(targetAmount, 0, java.math.RoundingMode.HALF_UP)
                .intValue();
    }

    // Note: getCategory() is not implemented as Goal doesn't have a category field
    // If needed, this would require adding a category relationship to the model
}
