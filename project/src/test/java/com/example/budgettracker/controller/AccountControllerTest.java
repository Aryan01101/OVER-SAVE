package com.example.budgettracker.controller;

import com.example.budgettracker.dto.Account.AccountResponse;
import com.example.budgettracker.model.Account;
import com.example.budgettracker.model.User;
import com.example.budgettracker.model.enums.AccountType;
import com.example.budgettracker.repository.AccountRepository;
import com.example.budgettracker.service.AuthService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AccountControllerTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AuthService authService;

    @InjectMocks
    private AccountController controller;

    @Before
    public void setUp() {
        // BaseController wires AuthService via field injection; populate manually
        ReflectionTestUtils.setField(controller, "authService", authService);
    }

    @Test
    public void list_returnsOnlyAccountsBelongingToAuthenticatedUser() {
        User user = User.builder().userId(42L).build();
        when(authService.getCurrentUser(anyString())).thenReturn(user);

        Account cash = Account.builder()
            .accountId(1L)
            .name("Main Cash")
            .accountType(AccountType.CASH)
            .balance(new BigDecimal("250.00"))
            .build();

        Account goal = Account.builder()
            .accountId(2L)
            .name("Vacation Goal")
            .accountType(AccountType.GOAL)
            .balance(new BigDecimal("125.00"))
            .build();

        when(accountRepository.findByUser_UserId(42L)).thenReturn(List.of(cash, goal));

        List<AccountResponse> responses = controller.list("Bearer token");

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getAccountType()).isEqualTo(AccountType.CASH);
        assertThat(responses.get(0).getName()).isEqualTo("Main Cash");
        assertThat(responses.get(0).getBalance()).isEqualByComparingTo("250.00");

        assertThat(responses.get(1).getAccountType()).isEqualTo(AccountType.GOAL);
        assertThat(responses.get(1).getName()).isEqualTo("Vacation Goal");
    }
}
