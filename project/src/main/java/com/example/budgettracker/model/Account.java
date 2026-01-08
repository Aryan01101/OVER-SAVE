package com.example.budgettracker.model;

import com.example.budgettracker.model.enums.AccountType;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
@Getter
@Setter
@Entity
@Table(
        name = "account",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_account_user_name", columnNames = {"user_id","name"})
        }
)
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Account {

//account for setting goal
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long accountId;

    @Column(nullable = false, length = 64)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountType accountType; // CASH / GOAL

    @Builder.Default
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id",
            foreignKey = @ForeignKey(name = "fk_account_user"))

    private User user;

//    public void setAccountName(String testAccount) {
//    }
}
