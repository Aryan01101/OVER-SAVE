package com.example.budgettracker.controller;

import com.example.budgettracker.dto.IncomeRequest;
import com.example.budgettracker.dto.IncomeResponse;
import com.example.budgettracker.model.Account;
import com.example.budgettracker.model.User;
import com.example.budgettracker.model.enums.AccountType;
import com.example.budgettracker.repository.AccountRepository;
import com.example.budgettracker.repository.UserRepository;
import com.example.budgettracker.service.IncomeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/income")
public class IncomeController extends BaseController {

    private final IncomeService incomeService;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    public IncomeController(IncomeService incomeService,
                            AccountRepository accountRepository,
                            UserRepository userRepository) {
        this.incomeService = incomeService;
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<IncomeResponse> addIncome(@RequestHeader("Authorization") String authHeader,
                                                    @Valid @RequestBody IncomeRequest request) {
        Long userId = getUserIdFromToken(authHeader);
        IncomeResponse response = incomeService.recordIncome(userId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<IncomeResponse>> getAllIncome(@RequestHeader("Authorization") String authHeader) {
        Long userId = getUserIdFromToken(authHeader);
        List<IncomeResponse> incomeList = incomeService.getAllIncome(userId);
        return ResponseEntity.ok(incomeList);
    }

    /**
     * Resolve the default account for the authenticated user.
     * If the user has no accounts yet, provision a zero-balance cash account.
     */
    @GetMapping("/default-account")
    public ResponseEntity<Map<String, Object>> getDefaultAccount(@RequestHeader("Authorization") String authHeader) {
        Long userId = getUserIdFromToken(authHeader);
        List<Account> accounts = accountRepository.findByUser_UserId(userId);

        Account defaultAccount;
        if (accounts.isEmpty()) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalStateException("User not found: " + userId));

            if (accountRepository.existsByUser_UserIdAndNameIgnoreCase(userId, "Cash Account")) {
                defaultAccount = accountRepository.findFirstByUser_UserIdAndAccountType(userId, AccountType.CASH)
                        .orElseThrow(() -> new IllegalStateException("Default account already exists but cannot be loaded."));
            } else {
                defaultAccount = new Account();
                defaultAccount.setUser(user);
                defaultAccount.setName("Cash Account");
                defaultAccount.setAccountType(AccountType.CASH);
                defaultAccount.setBalance(BigDecimal.ZERO);
                defaultAccount = accountRepository.save(defaultAccount);
            }
        } else {
            defaultAccount = accounts.get(0);
        }

        return ResponseEntity.ok(Map.of(
                "accountId", defaultAccount.getAccountId(),
                "accountName", defaultAccount.getName(),
                "balance", defaultAccount.getBalance()
        ));
    }

}
