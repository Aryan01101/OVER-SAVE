package com.example.budgettracker.model;


import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "item")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Item {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long itemId;

    @Column(nullable = false)
    private Long price;

    @Column(nullable = false)
    private Long stockQty;

    @Column(nullable = false, length = 50)
    private String itemName;

    @Column(length = 500)
    private String description;

    @Column(length = 10)
    private String emoji;

    @OneToMany(mappedBy = "item")
    private List<RewardRedeem> rewardRedeems;
}
