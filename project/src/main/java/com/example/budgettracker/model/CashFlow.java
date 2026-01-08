package com.example.budgettracker.model;

import com.example.budgettracker.model.enums.CashFlowType;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Getter
@Setter
@Entity
@Table(name = "cash_flow")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class CashFlow {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cashFlowId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private CashFlowType type; // Expense / Income

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime occurredAt;

    @Column(length = 255)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id",
            foreignKey = @ForeignKey(name = "fk_cashflow_account"))
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id",
            foreignKey = @ForeignKey(name = "fk_cashflow_category"))
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id",
            foreignKey = @ForeignKey(name = "fk_cashflow_subscription"))
    private Subscription subscription;


    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

}
