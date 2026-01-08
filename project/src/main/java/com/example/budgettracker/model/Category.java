package com.example.budgettracker.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @Setter
@Table(
        name = "category",
        uniqueConstraints = {
                // Unique category name under 1 user
                @UniqueConstraint(name = "uk_category_user_name", columnNames = {"user_id","name"})
        }
)
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Category {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long categoryId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 255)
    private String name;

    // Check if the category is created by system
    @Column(nullable = false)
    private boolean system;

}
