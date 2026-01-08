package com.example.budgettracker.model;



import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transfer")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Transfer {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long transferId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_to",
            foreignKey = @ForeignKey(name = "fk_transfer_to"))
    private Account accountTo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_from",
            foreignKey = @ForeignKey(name = "fk_transfer_from"))
    private Account accountFrom;
}
