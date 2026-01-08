package com.example.budgettracker.repository;


import com.example.budgettracker.model.Account;
import com.example.budgettracker.model.enums.AccountType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    boolean existsByUser_UserId(Long userId);

    List<Account> findByUser_UserId(Long userId);

    Optional<Account> findFirstByUser_UserIdAndAccountType(Long userId, AccountType accountType);

    boolean existsByUser_UserIdAndNameIgnoreCase(Long userId, String name);
}
