package com.example.budgettracker.dto.Account;

import com.example.budgettracker.model.enums.AccountType;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AccountResponse {
    private Long id;
    private String name;
    private BigDecimal balance;
    private AccountType accountType;
}
