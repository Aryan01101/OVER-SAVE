package com.example.budgettracker.dto.Budgetcoin;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data @AllArgsConstructor
public class RewardRedeemRequest {
    private Long userId;
    private Long itemId;

    public RewardRedeemRequest() {

    }
}
