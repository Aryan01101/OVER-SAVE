package com.example.budgettracker.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "login_attempt")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class LoginAttempt {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String ipAddress;

    @Column(nullable = false)
    private LocalDateTime attemptTime;

    @Column(nullable = false)
    @Builder.Default
    private Boolean successful = false;
}