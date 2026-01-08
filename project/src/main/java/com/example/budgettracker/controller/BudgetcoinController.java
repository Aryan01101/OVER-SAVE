package com.example.budgettracker.controller;

import com.example.budgettracker.dto.Budgetcoin.RewardBalanceResponse;
import com.example.budgettracker.dto.Budgetcoin.RewardGrantRequest;
import com.example.budgettracker.dto.Budgetcoin.RewardGrantResponse;
import com.example.budgettracker.dto.Budgetcoin.RewardRedeemRequest;
import com.example.budgettracker.dto.Budgetcoin.RewardRedeemResponse;
import com.example.budgettracker.dto.Budgetcoin.RewardTransactionResponse;
import com.example.budgettracker.service.RewardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/budgetcoin")
@RequiredArgsConstructor
public class BudgetcoinController extends BaseController {

    private final RewardService rewardService;

    @GetMapping("/balance")
    public ResponseEntity<RewardBalanceResponse> getBalance(@RequestHeader("Authorization") String authHeader) {
        Long userId = getUserIdFromToken(authHeader);
        BigDecimal balance = rewardService.getBalance(userId);
        return ResponseEntity.ok(new RewardBalanceResponse(userId, balance));
    }

    @PostMapping("/grant")
    public ResponseEntity<RewardGrantResponse> grantBudgetCoin(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody RewardGrantRequest request) {

        Long userId = getUserIdFromToken(authHeader);
        request.setUserId(userId);
        if (request.getRewardEventId() == null || request.getRewardEventId().isBlank()) {
            request.setRewardEventId(UUID.randomUUID().toString());
        }
        RewardGrantResponse response = rewardService.grantBudgetCoin(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/redeem")
    public ResponseEntity<RewardRedeemResponse> redeemBudgetCoin(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody RewardRedeemRequest request) {

        Long userId = getUserIdFromToken(authHeader);
        request.setUserId(userId);
        RewardRedeemResponse response = rewardService.redeemBudgetCoin(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/grants")
    public ResponseEntity<List<RewardGrantResponse>> getGrants(@RequestHeader("Authorization") String authHeader) {
        Long userId = getUserIdFromToken(authHeader);
        return ResponseEntity.ok(rewardService.getGrants(userId));
    }

    @GetMapping("/redeems")
    public ResponseEntity<List<RewardRedeemResponse>> getRedeems(@RequestHeader("Authorization") String authHeader) {
        Long userId = getUserIdFromToken(authHeader);
        return ResponseEntity.ok(rewardService.getRedeems(userId));
    }

    @GetMapping("/history")
    public ResponseEntity<List<RewardTransactionResponse>> getHistory(@RequestHeader("Authorization") String authHeader) {
        Long userId = getUserIdFromToken(authHeader);
        return ResponseEntity.ok(rewardService.getTransactionHistory(userId));
    }
}
