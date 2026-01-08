package com.example.budgettracker.service;

import com.example.budgettracker.dto.Budgetcoin.RewardGrantRequest;
import com.example.budgettracker.dto.Budgetcoin.RewardGrantResponse;
import com.example.budgettracker.dto.Budgetcoin.RewardRedeemRequest;
import com.example.budgettracker.dto.Budgetcoin.RewardRedeemResponse;
import com.example.budgettracker.dto.Budgetcoin.RewardTransactionResponse;
import com.example.budgettracker.model.Item;
import com.example.budgettracker.model.RewardGrant;
import com.example.budgettracker.model.RewardRedeem;
import com.example.budgettracker.model.User;
import com.example.budgettracker.repository.ItemRepository;
import com.example.budgettracker.repository.RewardGrantRepository;
import com.example.budgettracker.repository.RewardRedeemRepository;
import com.example.budgettracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class RewardService {

    private final UserRepository userRepository;
    private final RewardGrantRepository rewardGrantRepository;
    private final RewardRedeemRepository rewardRedeemRepository;
    private final ItemRepository itemRepository;

    public BigDecimal getBalance(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return user.getBudgetCoin() != null ? BigDecimal.valueOf(user.getBudgetCoin()) : BigDecimal.ZERO;
    }

    @Transactional
    public RewardGrantResponse grantBudgetCoin(RewardGrantRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Grant amount must be positive");
        }

        String sourceType = request.getSourceType();
        if (sourceType == null || sourceType.isBlank()) {
            sourceType = "General";
        }

        String rewardEventId = request.getRewardEventId();
        if (rewardEventId == null || rewardEventId.isBlank()) {
            rewardEventId = UUID.randomUUID().toString();
        }

        RewardGrant grant = RewardGrant.builder()
                .user(user)
                .amount(request.getAmount())
                .sourceType(sourceType)
                .rewardEventId(rewardEventId)
                .build();

        long currentBalance = user.getBudgetCoin() != null ? user.getBudgetCoin() : 0L;
        long newBalance = currentBalance + request.getAmount().longValue();
        user.setBudgetCoin(newBalance);
        userRepository.save(user);

        RewardGrant saved = rewardGrantRepository.save(grant);

        return new RewardGrantResponse(
                saved.getGrantId(),
                user.getUserId(),
                saved.getAmount(),
                saved.getSourceType(),
                saved.getRewardEventId(),
                saved.getCreatedAt(),
                newBalance
        );
    }

    @Transactional
    public RewardRedeemResponse redeemBudgetCoin(RewardRedeemRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Item item = itemRepository.findById(request.getItemId())
                .orElseThrow(() -> new RuntimeException("Item not found"));

        long currentBalance = user.getBudgetCoin() != null ? user.getBudgetCoin() : 0L;
        if (currentBalance < item.getPrice()) {
            throw new RuntimeException("Insufficient BudgetCoin balance");
        }

        if (item.getStockQty() <= 0) {
            throw new RuntimeException("Item out of stock");
        }

        long newBalance = currentBalance - item.getPrice();
        user.setBudgetCoin(newBalance);
        userRepository.save(user);

        item.setStockQty(item.getStockQty() - 1);
        itemRepository.save(item);

        RewardRedeem redeem = RewardRedeem.builder()
                .user(user)
                .item(item)
                .amount(BigDecimal.valueOf(item.getPrice()))
                .itemName(item.getItemName())
                .build();

        RewardRedeem saved = rewardRedeemRepository.save(redeem);

        return new RewardRedeemResponse(
                saved.getOrderId(),
                user.getUserId(),
                item.getItemId(),
                item.getItemName(),
                saved.getAmount(),
                saved.getRedeemedAt(),
                newBalance
        );
    }

    public List<RewardGrantResponse> getGrants(Long userId) {
        return rewardGrantRepository.findByUserUserId(userId)
                .stream()
                .map(g -> new RewardGrantResponse(
                        g.getGrantId(),
                        g.getUser().getUserId(),
                        g.getAmount(),
                        g.getSourceType(),
                        g.getRewardEventId(),
                        g.getCreatedAt(),
                        null
                ))
                .collect(Collectors.toList());
    }

    public List<RewardRedeemResponse> getRedeems(Long userId) {
        return rewardRedeemRepository.findByUserUserId(userId)
                .stream()
                .map(r -> new RewardRedeemResponse(
                        r.getOrderId(),
                        r.getUser().getUserId(),
                        r.getItem().getItemId(),
                        r.getItemName(),
                        r.getAmount(),
                        r.getRedeemedAt(),
                        null
                ))
                .collect(Collectors.toList());
    }

    public List<RewardTransactionResponse> getTransactionHistory(Long userId) {
        List<RewardGrant> grants = rewardGrantRepository.findByUserUserId(userId);
        List<RewardRedeem> redeems = rewardRedeemRepository.findByUserUserId(userId);

        return Stream.concat(
                        grants.stream()
                                .map(g -> new RewardTransactionResponse(
                                        "EARN",
                                        g.getSourceType(),
                                        g.getRewardEventId(),
                                        g.getAmount(),
                                        g.getCreatedAt()
                                )),
                        redeems.stream()
                                .map(r -> new RewardTransactionResponse(
                                        "SPEND",
                                        r.getItemName(),
                                        String.valueOf(r.getOrderId()),
                                        r.getAmount(),
                                        r.getRedeemedAt()
                                ))
                )
                .sorted(Comparator.comparing(RewardTransactionResponse::getOccurredAt).reversed())
                .collect(Collectors.toList());
    }
}
