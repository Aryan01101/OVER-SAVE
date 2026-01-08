package com.example.budgettracker.dto.Budgetcoin;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ItemResponse {
    private Long itemId;
    private String itemName;
    private Long price;
    private Long stockQty;
    private String description;
    private String emoji;
}
