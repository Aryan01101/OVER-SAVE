package com.example.budgettracker.service;


import com.example.budgettracker.dto.Budgetcoin.*;
import com.example.budgettracker.model.*;
import com.example.budgettracker.repository.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class BudgetCoinServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RewardGrantRepository rewardGrantRepository;
    @Mock
    private RewardRedeemRepository rewardRedeemRepository;
    @Mock
    private ItemRepository itemRepository;

    @InjectMocks
    private RewardService rewardService;

    private User user;
    private Item item;
    private RewardGrantRequest grantRequest;
    private RewardRedeemRequest redeemRequest;
    private RewardGrant grant;
    private RewardRedeem redeem;

    @Before
    public void setUp() {
        user = new User();
        user.setUserId(1L);
        user.setBudgetCoin(100L);

        item = new Item();
        item.setItemId(2L);
        item.setItemName("Coffee Mug");
        item.setPrice(50L);
        item.setStockQty(5L);

        grantRequest = new RewardGrantRequest();
        grantRequest.setUserId(1L);
        grantRequest.setAmount(BigDecimal.valueOf(200));
        grantRequest.setSourceType("Survey");
        grantRequest.setRewardEventId(String.valueOf(10L));

        redeemRequest = new RewardRedeemRequest();
        redeemRequest.setUserId(1L);
        redeemRequest.setItemId(2L);

        grant = new RewardGrant();
        grant.setGrantId(100L);
        grant.setUser(user);
        grant.setAmount(BigDecimal.valueOf(200));
        grant.setSourceType("Survey");
        grant.setRewardEventId(String.valueOf(10L));
        grant.setCreatedAt(LocalDateTime.now());

        redeem = new RewardRedeem();
        redeem.setOrderId(101L);
        redeem.setUser(user);
        redeem.setItem(item);
        redeem.setAmount(BigDecimal.valueOf(item.getPrice()));
        redeem.setItemName(item.getItemName());
        redeem.setRedeemedAt(LocalDateTime.now());
    }

    // =============================
    // ✅ getBalance()
    // =============================
    @Test
    public void getBalance_returnsCurrentBalance() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        BigDecimal balance = rewardService.getBalance(1L);

        assertEquals(BigDecimal.valueOf(100L), balance);
        verify(userRepository).findById(1L);
    }

    @Test
    public void getBalance_returnsZeroWhenNullBalance() {
        user.setBudgetCoin(null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        BigDecimal balance = rewardService.getBalance(1L);

        assertEquals(BigDecimal.ZERO, balance);
    }

    @Test
    public void getBalance_userNotFound_throwsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> rewardService.getBalance(1L));
    }

    // =============================
    // ✅ grantBudgetCoin()
    // =============================
    @Test
    public void grantBudgetCoin_successfullyUpdatesUserAndSavesGrant() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(rewardGrantRepository.save(any(RewardGrant.class))).thenReturn(grant);
        when(userRepository.save(any(User.class))).thenReturn(user);

        RewardGrantResponse response = rewardService.grantBudgetCoin(grantRequest);

        assertNotNull(response);
        assertEquals(grant.getAmount(), response.getAmount());
        assertEquals("Survey", response.getSourceType());
        assertEquals(Long.valueOf(1L), response.getUserId());
        assertEquals(grant.getRewardEventId(), response.getRewardEventId());
        assertEquals(Long.valueOf(300L), response.getBalanceAfter());
        verify(userRepository).save(user);
        verify(rewardGrantRepository).save(any(RewardGrant.class));
    }

    @Test
    public void grantBudgetCoin_userNotFound_throwsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> rewardService.grantBudgetCoin(grantRequest));
        verifyNoInteractions(rewardGrantRepository);
    }

    // =============================
    // ✅ redeemBudgetCoin()
    // =============================
    @Test
    public void redeemBudgetCoin_successfullyRedeemsItem() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(itemRepository.findById(2L)).thenReturn(Optional.of(item));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(itemRepository.save(any(Item.class))).thenReturn(item);
        when(rewardRedeemRepository.save(any(RewardRedeem.class))).thenReturn(redeem);

        RewardRedeemResponse response = rewardService.redeemBudgetCoin(redeemRequest);

        assertNotNull(response);
        assertEquals(Long.valueOf(1L), response.getUserId());
        assertEquals(Long.valueOf(2L), response.getItemId());
        assertEquals("Coffee Mug", response.getItemName());
        assertEquals(BigDecimal.valueOf(50), response.getAmount());
        assertEquals(Long.valueOf(50L), response.getBalanceAfter());
        verify(userRepository).save(user);
        verify(itemRepository).save(item);
        verify(rewardRedeemRepository).save(any(RewardRedeem.class));
    }

    @Test
    public void redeemBudgetCoin_userNotFound_throwsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> rewardService.redeemBudgetCoin(redeemRequest));
    }

    @Test
    public void redeemBudgetCoin_itemNotFound_throwsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(itemRepository.findById(2L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> rewardService.redeemBudgetCoin(redeemRequest));
    }

    @Test
    public void redeemBudgetCoin_insufficientBalance_throwsException() {
        user.setBudgetCoin(10L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(itemRepository.findById(2L)).thenReturn(Optional.of(item));

        assertThrows(RuntimeException.class, () -> rewardService.redeemBudgetCoin(redeemRequest));
        verify(userRepository, never()).save(any());
    }

    @Test
    public void redeemBudgetCoin_itemOutOfStock_throwsException() {
        item.setStockQty(0L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(itemRepository.findById(2L)).thenReturn(Optional.of(item));

        assertThrows(RuntimeException.class, () -> rewardService.redeemBudgetCoin(redeemRequest));
    }

    // =============================
    // ✅ getGrants()
    // =============================
    @Test
    public void getGrants_returnsGrantResponses() {
        List<RewardGrant> list = new ArrayList<>();
        list.add(grant);
        when(rewardGrantRepository.findByUserUserId(1L)).thenReturn(list);

        List<RewardGrantResponse> result = rewardService.getGrants(1L);

        assertEquals(1, result.size());
        assertEquals(BigDecimal.valueOf(200), result.get(0).getAmount());
        assertEquals("Survey", result.get(0).getSourceType());
        assertEquals("10", result.get(0).getRewardEventId());
        verify(rewardGrantRepository).findByUserUserId(1L);
    }

    @Test
    public void getGrants_returnsEmptyList() {
        when(rewardGrantRepository.findByUserUserId(1L)).thenReturn(Collections.emptyList());
        List<RewardGrantResponse> result = rewardService.getGrants(1L);
        assertTrue(result.isEmpty());
    }

    // =============================
    // ✅ getRedeems()
    // =============================
    @Test
    public void getRedeems_returnsRedeemResponses() {
        List<RewardRedeem> list = new ArrayList<>();
        list.add(redeem);
        when(rewardRedeemRepository.findByUserUserId(1L)).thenReturn(list);

        List<RewardRedeemResponse> result = rewardService.getRedeems(1L);

        assertEquals(1, result.size());
        assertEquals("Coffee Mug", result.get(0).getItemName());
        assertEquals(BigDecimal.valueOf(50), result.get(0).getAmount());
        verify(rewardRedeemRepository).findByUserUserId(1L);
    }

    @Test
    public void getRedeems_returnsEmptyList() {
        when(rewardRedeemRepository.findByUserUserId(1L)).thenReturn(Collections.emptyList());
        List<RewardRedeemResponse> result = rewardService.getRedeems(1L);
        assertTrue(result.isEmpty());
    }

    @Test
    public void getTransactionHistory_mergesAndSortsRecords() {
        RewardGrant olderGrant = RewardGrant.builder()
                .grantId(200L)
                .user(user)
                .amount(BigDecimal.valueOf(150))
                .sourceType("Daily streak")
                .rewardEventId("event-1")
                .build();
        olderGrant.setCreatedAt(LocalDateTime.now().minusDays(1));

        RewardRedeem latestRedeem = RewardRedeem.builder()
                .orderId(300L)
                .user(user)
                .item(item)
                .itemName(item.getItemName())
                .amount(BigDecimal.valueOf(item.getPrice()))
                .build();
        latestRedeem.setRedeemedAt(LocalDateTime.now());

        when(rewardGrantRepository.findByUserUserId(1L)).thenReturn(List.of(olderGrant));
        when(rewardRedeemRepository.findByUserUserId(1L)).thenReturn(List.of(latestRedeem));

        List<RewardTransactionResponse> history = rewardService.getTransactionHistory(1L);

        assertEquals(2, history.size());
        assertEquals("SPEND", history.get(0).getType());
        assertEquals("EARN", history.get(1).getType());
        verify(rewardGrantRepository).findByUserUserId(1L);
        verify(rewardRedeemRepository).findByUserUserId(1L);
    }
}
