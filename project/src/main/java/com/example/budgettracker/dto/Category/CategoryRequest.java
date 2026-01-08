package com.example.budgettracker.dto.Category;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoryRequest {
    @NotBlank
    @Size(max = 100)
    @Pattern(regexp = "^[\\p{L}\\p{N} _/&\\-]{1,100}$",
            message = "Name contains invalid characters")
    private String name;

}
