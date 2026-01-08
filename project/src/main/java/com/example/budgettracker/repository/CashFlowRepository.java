package com.example.budgettracker.repository;
// src/main/java/com/example/repository/CashFlowRepository.java

import com.example.budgettracker.model.CashFlow;
import com.example.budgettracker.model.enums.CashFlowType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface CashFlowRepository extends JpaRepository<CashFlow, Long> {
    @Query("""
           select coalesce(sum(c.amount), 0)
           from CashFlow c
           where c.category.categoryId = :categoryId
             and c.account.user.userId = :userId
             and c.type = com.example.budgettracker.model.enums.CashFlowType.Expense
             and c.occurredAt between :start and :end
           """)
    BigDecimal sumUserExpenseByCategoryAndPeriod(@Param("userId") Long userId,
                                                 @Param("categoryId") Long categoryId,
                                                 @Param("start") LocalDateTime start,
                                                 @Param("end") LocalDateTime end);

    boolean existsByAccount_User_UserIdAndCategory_CategoryIdAndOccurredAtBetween(
            Long userId, Long categoryId, LocalDateTime start, LocalDateTime end);
    List<CashFlow> findByTypeOrderByCreatedAtDesc(CashFlowType type);

    // Find all cash flows by user and type, ordered by creation time
    @Query("""
        select c from CashFlow c
        where c.account.user.userId = :userId
          and c.type = :type
        order by c.createdAt desc
        """)
    List<CashFlow> findByUserIdAndTypeOrderByCreatedAtDesc(
        @Param("userId") Long userId,
        @Param("type") CashFlowType type
    );


    long countByAccount_User_UserIdAndCategory_CategoryIdAndType(
            Long userId, Long categoryId, CashFlowType type);


    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
           update CashFlow c
              set c.category.categoryId = :toId
            where c.account.user.userId = :userId
              and c.category.categoryId = :fromId
              and c.type = com.example.budgettracker.model.enums.CashFlowType.Expense
           """)
    int reassignUserExpenseCategory(@Param("userId") Long userId,
                                    @Param("fromId") Long fromId,
                                    @Param("toId") Long toId);


    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
           update CashFlow c
              set c.category.categoryId = :toId
            where c.account.user.userId = :userId
              and c.category.categoryId in :fromIds
              and c.type = com.example.budgettracker.model.enums.CashFlowType.Expense
           """)
    int reassignUserExpenseCategoryIn(@Param("userId") Long userId,
                                      @Param("fromIds") List<Long> fromIds,
                                      @Param("toId") Long toId);


    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
           update CashFlow c
              set c.category.categoryId = :toId
            where c.category.user.userId = :userId
              and c.category.categoryId in :fromIds
           """)
    int reassignUserCategoryIn(@Param("userId") Long userId,
                               @Param("fromIds") List<Long> fromIds,
                               @Param("toId") Long toId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
           delete from CashFlow c
            where c.category.user.userId = :userId
              and c.category.categoryId = :categoryId
           """)
    int deleteByUserAndCategory(@Param("userId") Long userId,
                                @Param("categoryId") Long categoryId);


    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
           update CashFlow c
              set c.category.categoryId = :toId
            where c.category.user.userId = :userId
              and c.category.categoryId = :fromId
           """)
    int reassignUserCategory(@Param("userId") Long userId,
                            @Param("fromId") Long fromId,
                            @Param("toId") Long toId);


    long countByAccount_User_UserIdAndCategory_CategoryId(Long userId, Long categoryId);

    @Query("""
    select count(c) from CashFlow c
    where c.account.user.userId = :userId
      and c.category.categoryId = :categoryId
      and c.occurredAt between :start and :end
    """)
    long countByUserAndCategoryAndOccurredAtBetween(
            @Param("userId") Long userId,
            @Param("categoryId") Long categoryId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("""
    select sum(c.amount) from CashFlow c
    where c.account.user.userId = :userId
      and c.category.categoryId = :categoryId
      and c.occurredAt between :start and :end
      and c.type = :type
    """)
    BigDecimal sumByTypeInRange(
            @Param("userId") Long userId,
            @Param("categoryId") Long categoryId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("type") CashFlowType type);

    boolean existsByAccount_User_UserIdAndOccurredAtBetween(
            Long userId, LocalDateTime start, LocalDateTime end);

    @Query("""
    select c from CashFlow c
    where c.account.user.userId = :userId
      and c.category.categoryId = :categoryId
      and c.occurredAt between :start and :end
      and (:type is null or c.type = :type)
    order by c.occurredAt desc, c.cashFlowId desc
    """)
    List<CashFlow> findByUserCategoryPeriodAndOptionalType(
            @Param("userId") Long userId,
            @Param("categoryId") Long categoryId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("type") CashFlowType type);

    @Query("""
        select coalesce(sum(
            case
                when c.type = com.example.budgettracker.model.enums.CashFlowType.Income then c.amount
                when c.type = com.example.budgettracker.model.enums.CashFlowType.Expense then -c.amount
                else 0
            end
        ), 0)
        from CashFlow c
        where c.account.user.userId = :userId
          and c.account.accountId = :accountId
        """)
    BigDecimal computeAccountBalance(
            @Param("userId") Long userId,
            @Param("accountId") Long accountId
    );

    @Query("""
        select coalesce(sum(c.amount), 0)
        from CashFlow c
        where c.account.user.userId = :userId
          and c.type = com.example.budgettracker.model.enums.CashFlowType.Expense
        """)
    BigDecimal sumAllExpenseByUser(@Param("userId") Long userId);

    @Query("""
        select coalesce(sum(
            case
                when c.type = com.example.budgettracker.model.enums.CashFlowType.Income then c.amount
                when c.type = com.example.budgettracker.model.enums.CashFlowType.Expense then -c.amount
                else 0
            end
        ), 0)
        from CashFlow c
        where c.account.user.userId = :userId
        """)
    BigDecimal computeTotalBalanceByUserId(@Param("userId") Long userId);

    @Query("""
        select c from CashFlow c
        where c.account.user.userId = :userId
        order by c.occurredAt desc, c.cashFlowId desc
        """)
    List<CashFlow> findRecentByUserId(@Param("userId") Long userId);

    @Query("""
        select c from CashFlow c
        where c.account.user.userId = :userId
          and c.occurredAt between :start and :end
        order by c.occurredAt desc
        """)
    List<CashFlow> findByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );


    // CashFlowRepository.java
    @Query("""
        select coalesce(
          sum(case when cf.type = com.example.budgettracker.model.enums.CashFlowType.Income
                   then cf.amount else -cf.amount end), 0
        )
        from CashFlow cf
        where cf.account.accountId = :accountId
    """)
    BigDecimal computeAccountBalanceByAccountId(@Param("accountId") Long accountId);

    // Sum all income for a user
    @Query("""
        select coalesce(sum(c.amount), 0)
        from CashFlow c
        where c.account.user.userId = :userId
          and c.type = com.example.budgettracker.model.enums.CashFlowType.Income
        """)
    BigDecimal sumAllIncomeByUser(@Param("userId") Long userId);

    // Find cash flows by user and date range
    @Query("""
        select c from CashFlow c
        where c.account.user.userId = :userId
          and c.occurredAt between :start and :end
        order by c.occurredAt desc
        """)
    List<CashFlow> findByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    List<CashFlow> findByAccount_User_UserIdAndOccurredAtBetween(Long userId, LocalDateTime startDate, LocalDateTime endDate);
}
