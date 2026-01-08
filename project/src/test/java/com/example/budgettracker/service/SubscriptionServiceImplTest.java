package com.example.budgettracker.service;

import com.example.budgettracker.dto.SubscriptionRequest;
import com.example.budgettracker.dto.SubscriptionResponse;
import com.example.budgettracker.model.Subscription;
import com.example.budgettracker.model.User;
import com.example.budgettracker.repository.SubscriptionRepository;
import com.example.budgettracker.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(org.mockito.junit.MockitoJUnitRunner.class)
public class SubscriptionServiceImplTest {

    private static final Long USER_ID = 1L;
    private static final Long SUBSCRIPTION_ID = 100L;
    private static final String MERCHANT = "Netflix";
    private static final BigDecimal AMOUNT = new BigDecimal("15.99");
    private static final String FREQUENCY_MONTHLY = "MONTHLY";
    private static final String FREQUENCY_WEEKLY = "WEEKLY";
    private static final String FREQUENCY_YEARLY = "YEARLY";

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SubscriptionServiceImpl subscriptionService;

    private User testUser;
    private Subscription testSubscription;
    private SubscriptionRequest subscriptionRequest;

    @Before
    public void setUp() {
        testUser = new User();
        testUser.setUserId(USER_ID);
        testUser.setEmail("test@example.com");

        testSubscription = Subscription.builder()
                .subscriptionId(SUBSCRIPTION_ID)
                .merchant(MERCHANT)
                .amount(AMOUNT)
                .frequency(FREQUENCY_MONTHLY)
                .startDate(LocalDateTime.now())
                .isActive(true)
                .nextPostAt(LocalDateTime.now())
                .user(testUser)
                .build();

        subscriptionRequest = new SubscriptionRequest();
        subscriptionRequest.setMerchant(MERCHANT);
        subscriptionRequest.setAmount(AMOUNT);
        subscriptionRequest.setFrequency(FREQUENCY_MONTHLY);
        subscriptionRequest.setStartDate(LocalDateTime.now());
        subscriptionRequest.setIsActive(true);
    }

    // ==================== create Tests ====================

    @Test
    public void create_succeeds_whenUserExists() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> {
            Subscription sub = invocation.getArgument(0);
            sub.setSubscriptionId(SUBSCRIPTION_ID);
            return sub;
        });

        SubscriptionResponse response = subscriptionService.create(USER_ID, subscriptionRequest);

        assertNotNull(response);
        assertThat(response.getSubscriptionId(), is(SUBSCRIPTION_ID));
        assertThat(response.getMerchant(), is(MERCHANT));
        assertThat(response.getAmount(), is(AMOUNT));
        verify(subscriptionRepository).save(any(Subscription.class));
    }

    @Test(expected = EntityNotFoundException.class)
    public void create_throws_whenUserNotFound() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        subscriptionService.create(USER_ID, subscriptionRequest);
    }

    @Test
    public void create_setsDefaultActive_whenNotSpecified() {
        subscriptionRequest.setIsActive(null);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        subscriptionService.create(USER_ID, subscriptionRequest);

        ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
        verify(subscriptionRepository).save(captor.capture());
        Subscription saved = captor.getValue();
        assertTrue(saved.getIsActive());
    }

    @Test
    public void create_usesStartDateForFirstPost_whenFirstPostAtNotProvided() {
        LocalDateTime startDate = LocalDateTime.now();
        subscriptionRequest.setStartDate(startDate);
        subscriptionRequest.setFirstPostAt(null);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        subscriptionService.create(USER_ID, subscriptionRequest);

        ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
        verify(subscriptionRepository).save(captor.capture());
        Subscription saved = captor.getValue();
        assertThat(saved.getNextPostAt(), is(startDate));
    }

    @Test
    public void create_usesProvidedFirstPostAt_whenSpecified() {
        LocalDateTime firstPost = LocalDateTime.now().plusDays(7);
        subscriptionRequest.setFirstPostAt(firstPost);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        subscriptionService.create(USER_ID, subscriptionRequest);

        ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
        verify(subscriptionRepository).save(captor.capture());
        Subscription saved = captor.getValue();
        assertThat(saved.getNextPostAt(), is(firstPost));
    }

    @Test
    public void create_convertsFrequencyToUpperCase() {
        subscriptionRequest.setFrequency("monthly");
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        subscriptionService.create(USER_ID, subscriptionRequest);

        ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
        verify(subscriptionRepository).save(captor.capture());
        Subscription saved = captor.getValue();
        assertThat(saved.getFrequency(), is("MONTHLY"));
    }

    // ==================== list Tests ====================

    @Test
    public void list_returnsActiveOnly_whenActiveOnlyTrue() {
        when(subscriptionRepository.findByUser_UserIdAndIsActiveTrue(USER_ID))
                .thenReturn(Arrays.asList(testSubscription));

        List<SubscriptionResponse> result = subscriptionService.list(USER_ID, true);

        assertThat(result.size(), is(1));
        assertThat(result.get(0).getMerchant(), is(MERCHANT));
        verify(subscriptionRepository).findByUser_UserIdAndIsActiveTrue(USER_ID);
        verify(subscriptionRepository, never()).findByUser_UserIdOrderByNextPostAtAsc(any());
    }

    @Test
    public void list_returnsAll_whenActiveOnlyFalse() {
        when(subscriptionRepository.findByUser_UserIdOrderByNextPostAtAsc(USER_ID))
                .thenReturn(Arrays.asList(testSubscription));

        List<SubscriptionResponse> result = subscriptionService.list(USER_ID, false);

        assertThat(result.size(), is(1));
        verify(subscriptionRepository).findByUser_UserIdOrderByNextPostAtAsc(USER_ID);
        verify(subscriptionRepository, never()).findByUser_UserIdAndIsActiveTrue(any());
    }

    @Test
    public void list_returnsEmptyList_whenNoSubscriptions() {
        when(subscriptionRepository.findByUser_UserIdOrderByNextPostAtAsc(USER_ID))
                .thenReturn(Collections.emptyList());

        List<SubscriptionResponse> result = subscriptionService.list(USER_ID, false);

        assertTrue(result.isEmpty());
    }

    // ==================== update Tests ====================

    @Test
    public void update_succeeds_whenSubscriptionOwnedByUser() {
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(testSubscription));

        subscriptionRequest.setMerchant("Updated Merchant");
        subscriptionRequest.setAmount(new BigDecimal("19.99"));

        SubscriptionResponse response = subscriptionService.update(USER_ID, SUBSCRIPTION_ID, subscriptionRequest);

        assertNotNull(response);
        assertThat(testSubscription.getMerchant(), is("Updated Merchant"));
        assertThat(testSubscription.getAmount(), is(new BigDecimal("19.99")));
    }

    @Test(expected = EntityNotFoundException.class)
    public void update_throws_whenSubscriptionNotFound() {
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.empty());

        subscriptionService.update(USER_ID, SUBSCRIPTION_ID, subscriptionRequest);
    }

    @Test(expected = EntityNotFoundException.class)
    public void update_throws_whenSubscriptionNotOwnedByUser() {
        User differentUser = new User();
        differentUser.setUserId(999L);
        testSubscription.setUser(differentUser);

        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(testSubscription));

        subscriptionService.update(USER_ID, SUBSCRIPTION_ID, subscriptionRequest);
    }

    @Test
    public void update_updatesNextPostAt_whenFirstPostAtProvided() {
        LocalDateTime newFirstPost = LocalDateTime.now().plusDays(14);
        subscriptionRequest.setFirstPostAt(newFirstPost);
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(testSubscription));

        subscriptionService.update(USER_ID, SUBSCRIPTION_ID, subscriptionRequest);

        assertThat(testSubscription.getNextPostAt(), is(newFirstPost));
    }

    @Test
    public void update_updatesIsActive_whenProvided() {
        subscriptionRequest.setIsActive(false);
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(testSubscription));

        subscriptionService.update(USER_ID, SUBSCRIPTION_ID, subscriptionRequest);

        assertFalse(testSubscription.getIsActive());
    }

    // ==================== pause Tests ====================

    @Test
    public void pause_setsIsActiveToFalse() {
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(testSubscription));

        subscriptionService.pause(USER_ID, SUBSCRIPTION_ID);

        assertFalse(testSubscription.getIsActive());
    }

    @Test(expected = EntityNotFoundException.class)
    public void pause_throws_whenSubscriptionNotFound() {
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.empty());

        subscriptionService.pause(USER_ID, SUBSCRIPTION_ID);
    }

    // ==================== resume Tests ====================

    @Test
    public void resume_setsIsActiveToTrue() {
        testSubscription.setIsActive(false);
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(testSubscription));

        subscriptionService.resume(USER_ID, SUBSCRIPTION_ID);

        assertTrue(testSubscription.getIsActive());
    }

    @Test
    public void resume_setsNextPostAtToNow_whenNull() {
        testSubscription.setIsActive(false);
        testSubscription.setNextPostAt(null);
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(testSubscription));

        subscriptionService.resume(USER_ID, SUBSCRIPTION_ID);

        assertNotNull(testSubscription.getNextPostAt());
    }

    @Test(expected = EntityNotFoundException.class)
    public void resume_throws_whenSubscriptionNotFound() {
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.empty());

        subscriptionService.resume(USER_ID, SUBSCRIPTION_ID);
    }

    // ==================== delete Tests ====================

    @Test
    public void delete_succeeds_whenSubscriptionOwnedByUser() {
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(testSubscription));

        subscriptionService.delete(USER_ID, SUBSCRIPTION_ID);

        verify(subscriptionRepository).delete(testSubscription);
    }

    @Test(expected = EntityNotFoundException.class)
    public void delete_throws_whenSubscriptionNotFound() {
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.empty());

        subscriptionService.delete(USER_ID, SUBSCRIPTION_ID);
    }

    @Test(expected = EntityNotFoundException.class)
    public void delete_throws_whenSubscriptionNotOwnedByUser() {
        User differentUser = new User();
        differentUser.setUserId(999L);
        testSubscription.setUser(differentUser);

        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(testSubscription));

        subscriptionService.delete(USER_ID, SUBSCRIPTION_ID);
    }

    // ==================== monthlyEquivalent Tests ====================

    @Test
    public void monthlyEquivalent_calculatesCorrectly_forWeekly() {
        subscriptionRequest.setFrequency(FREQUENCY_WEEKLY);
        subscriptionRequest.setAmount(new BigDecimal("10.00"));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> {
            Subscription sub = invocation.getArgument(0);
            sub.setSubscriptionId(SUBSCRIPTION_ID);
            return sub;
        });

        SubscriptionResponse response = subscriptionService.create(USER_ID, subscriptionRequest);

        // Weekly: amount * (52/12) = 10 * 4.333... = ~43.33
        assertNotNull(response.getMonthlyEquivalent());
        assertTrue(response.getMonthlyEquivalent().compareTo(new BigDecimal("43.00")) > 0);
        assertTrue(response.getMonthlyEquivalent().compareTo(new BigDecimal("44.00")) < 0);
    }

    @Test
    public void monthlyEquivalent_calculatesCorrectly_forFortnightly() {
        subscriptionRequest.setFrequency("FORTNIGHTLY");
        subscriptionRequest.setAmount(new BigDecimal("20.00"));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> {
            Subscription sub = invocation.getArgument(0);
            sub.setSubscriptionId(SUBSCRIPTION_ID);
            return sub;
        });

        SubscriptionResponse response = subscriptionService.create(USER_ID, subscriptionRequest);

        // Fortnightly: amount * (26/12) = 20 * 2.166... = ~43.33
        assertNotNull(response.getMonthlyEquivalent());
        assertTrue(response.getMonthlyEquivalent().compareTo(new BigDecimal("43.00")) > 0);
        assertTrue(response.getMonthlyEquivalent().compareTo(new BigDecimal("44.00")) < 0);
    }

    @Test
    public void monthlyEquivalent_calculatesCorrectly_forMonthly() {
        subscriptionRequest.setFrequency(FREQUENCY_MONTHLY);
        subscriptionRequest.setAmount(new BigDecimal("50.00"));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> {
            Subscription sub = invocation.getArgument(0);
            sub.setSubscriptionId(SUBSCRIPTION_ID);
            return sub;
        });

        SubscriptionResponse response = subscriptionService.create(USER_ID, subscriptionRequest);

        // Monthly: same amount
        assertThat(response.getMonthlyEquivalent(), is(new BigDecimal("50.00")));
    }

    @Test
    public void monthlyEquivalent_calculatesCorrectly_forQuarterly() {
        subscriptionRequest.setFrequency("QUARTERLY");
        subscriptionRequest.setAmount(new BigDecimal("90.00"));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> {
            Subscription sub = invocation.getArgument(0);
            sub.setSubscriptionId(SUBSCRIPTION_ID);
            return sub;
        });

        SubscriptionResponse response = subscriptionService.create(USER_ID, subscriptionRequest);

        // Quarterly: amount / 3 = 90 / 3 = 30.00
        assertThat(response.getMonthlyEquivalent(), is(new BigDecimal("30.00")));
    }

    @Test
    public void monthlyEquivalent_calculatesCorrectly_forYearly() {
        subscriptionRequest.setFrequency(FREQUENCY_YEARLY);
        subscriptionRequest.setAmount(new BigDecimal("120.00"));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> {
            Subscription sub = invocation.getArgument(0);
            sub.setSubscriptionId(SUBSCRIPTION_ID);
            return sub;
        });

        SubscriptionResponse response = subscriptionService.create(USER_ID, subscriptionRequest);

        // Yearly: amount / 12 = 120 / 12 = 10.00
        assertThat(response.getMonthlyEquivalent(), is(new BigDecimal("10.00")));
    }

    @Test
    public void monthlyEquivalent_calculatesCorrectly_forAnnually() {
        subscriptionRequest.setFrequency("ANNUALLY");
        subscriptionRequest.setAmount(new BigDecimal("240.00"));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> {
            Subscription sub = invocation.getArgument(0);
            sub.setSubscriptionId(SUBSCRIPTION_ID);
            return sub;
        });

        SubscriptionResponse response = subscriptionService.create(USER_ID, subscriptionRequest);

        // Annually: amount / 12 = 240 / 12 = 20.00
        assertThat(response.getMonthlyEquivalent(), is(new BigDecimal("20.00")));
    }

    @Test
    public void monthlyEquivalent_fallsBackToMonthly_forUnknownFrequency() {
        subscriptionRequest.setFrequency("UNKNOWN");
        subscriptionRequest.setAmount(new BigDecimal("25.00"));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> {
            Subscription sub = invocation.getArgument(0);
            sub.setSubscriptionId(SUBSCRIPTION_ID);
            return sub;
        });

        SubscriptionResponse response = subscriptionService.create(USER_ID, subscriptionRequest);

        // Unknown: fallback to monthly (same amount)
        assertThat(response.getMonthlyEquivalent(), is(new BigDecimal("25.00")));
    }
}
