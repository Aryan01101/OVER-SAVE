package com.example.budgettracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetResponse {

    private boolean success;
    private String message;
    private List<String> errors;
    private String token;

    // Static factory methods
    public static PasswordResetResponse success(String message) {
        return PasswordResetResponse.builder()
                .success(true)
                .message(message)
                .build();
    }

    public static PasswordResetResponse success(String message, String token) {
        return PasswordResetResponse.builder()
                .success(true)
                .message(message)
                .token(token)
                .build();
    }

    public static PasswordResetResponse error(String message) {
        return PasswordResetResponse.builder()
                .success(false)
                .message(message)
                .build();
    }

    public static PasswordResetResponse error(String message, List<String> errors) {
        return PasswordResetResponse.builder()
                .success(false)
                .message(message)
                .errors(errors)
                .build();
    }
}