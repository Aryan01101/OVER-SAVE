package com.example.budgettracker.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class IdpLoginRequest {

    @NotBlank(message = "Provider is required")
    private String provider;

    @NotBlank(message = "ID token is required")
    private String idToken;

    @NotBlank(message = "Subject ID is required")
    private String subjectId;
}