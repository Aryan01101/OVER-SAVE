package com.example.budgettracker.dto.Budgetcoin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
@Getter
@Setter
@Data @AllArgsConstructor
public class RewardGrantRequest {
    private Long userId;
    private BigDecimal amount;
    private String sourceType;
    private String rewardEventId;


    public RewardGrantRequest() {

    }
}
