package com.example.budgettracker.controller;

import com.example.budgettracker.dto.Account.AccountResponse;
import com.example.budgettracker.model.Account;
import com.example.budgettracker.model.enums.AccountType;
import com.example.budgettracker.repository.AccountRepository;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController extends BaseController {

    private final AccountRepository accountRepository;

    @GetMapping
    public List<AccountResponse> list(@RequestHeader("Authorization") String authHeader) {
        Long userId = getUserIdFromToken(authHeader);

        return accountRepository.findByUser_UserId(userId).stream()
                .map(this::toResponse)
                .sorted(accountComparator())
                .collect(Collectors.toList());
    }

    private AccountResponse toResponse(Account account) {
        BigDecimal balance = account.getBalance() != null ? account.getBalance() : BigDecimal.ZERO;
        return new AccountResponse(
                account.getAccountId(),
                account.getName(),
                balance,
                account.getAccountType());
    }

    private Comparator<AccountResponse> accountComparator() {
        return Comparator
            .comparing((AccountResponse acc) -> acc.getAccountType() == AccountType.CASH ? 0 : 1)
            .thenComparing(AccountResponse::getName, String.CASE_INSENSITIVE_ORDER);
    }
}
