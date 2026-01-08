package com.example.budgettracker.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "session", indexes = {
    @Index(name = "idx_session_token", columnList = "sessionToken"),
    @Index(name = "idx_session_expires", columnList = "expiresAt"),
    @Index(name = "idx_session_last_activity", columnList = "lastActivityAt")
})
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Session {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long sessionId;

    @Column(nullable = false)
    private LocalDateTime issuedAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = true) // Allow null for migration
    private LocalDateTime lastActivityAt;

    @Column(nullable = false, length = 500)
    private String sessionToken;

    // For tampering detection
    @Column(nullable = true, length = 255) // Allow null for migration
    private String tokenSignature;

    @Column(length = 45) // IPv4/IPv6 address
    private String ipAddress;

    @Column(length = 500)
    private String userAgent;

    @Column(nullable = true) // Allow null for migration
    @Builder.Default
    private Boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id",
            foreignKey = @ForeignKey(name = "fk_session_user"))
    private User user;

    // Helper method to check if session is expired
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    // Helper method to check if session is idle (5 minutes)
    public boolean isIdle() {
        if (lastActivityAt == null) {
            return true; // Treat null as expired
        }
        return LocalDateTime.now().isAfter(lastActivityAt.plusMinutes(5));
    }

    // Update activity timestamp
    public void updateActivity() {
        this.lastActivityAt = LocalDateTime.now();
    }

    // Helper method to check if session is active
    public Boolean getIsActive() {
        return isActive != null ? isActive : false;
    }
}