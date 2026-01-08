package com.example.budgettracker.dto.Category;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class CategoryResponse {
    private Long id;

    // Add categoryId as an alias for id for API compatibility
    @JsonProperty("categoryId")
    public Long getCategoryId() {
        return id;
    }

    private String name;
    private boolean system;

    public CategoryResponse(Long id, String name, boolean system) {
        this.id = id; this.name = name; this.system = system;
    }
}
