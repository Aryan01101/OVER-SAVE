package com.example.budgettracker.dto.Budgetcoin;

import lombok.*;

import java.math.BigDecimal;
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RewardBalanceResponse {
    private long userId;
    private BigDecimal balance;

}
