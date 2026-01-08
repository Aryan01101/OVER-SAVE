package com.example.budgettracker.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "idp_account")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class IdpAccount {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idpAccountId;

    @Column(nullable = false, length = 16)
    private String provider;

    @Column(nullable = false)
    private LocalDateTime linkedAt;

    @Column(nullable = false, length = 255)
    private String subjectId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id",
            foreignKey = @ForeignKey(name = "fk_idp_account_user"))

    private User user;
}