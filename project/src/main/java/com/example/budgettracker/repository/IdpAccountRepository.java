package com.example.budgettracker.repository;

import com.example.budgettracker.model.IdpAccount;
import com.example.budgettracker.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IdpAccountRepository extends JpaRepository<IdpAccount, Long> {

    Optional<IdpAccount> findByProviderAndSubjectId(String provider, String subjectId);

    Optional<IdpAccount> findByUser(User user);

    boolean existsByProviderAndSubjectId(String provider, String subjectId);
}