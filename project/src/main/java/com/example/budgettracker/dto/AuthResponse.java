package com.example.budgettracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String sessionToken;
    private Long userId;
    private String email;
    private String firstName;
    private String lastName;
    private String redirectUrl;
    private String message;

    public static AuthResponse success(String sessionToken, Long userId, String email,
                                     String firstName, String lastName, String redirectUrl) {
        return AuthResponse.builder()
                .sessionToken(sessionToken)
                .userId(userId)
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .redirectUrl(redirectUrl)
                .build();
    }

    public static AuthResponse error(String message) {
        return AuthResponse.builder()
                .message(message)
                .build();
    }
}