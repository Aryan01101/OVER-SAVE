package com.example.budgettracker.dto.Category;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter

public class CategoryMergeRequest {
    @NotNull
    private Long targetId;
    @NotEmpty
    private List<Long> sourceIds;
}
