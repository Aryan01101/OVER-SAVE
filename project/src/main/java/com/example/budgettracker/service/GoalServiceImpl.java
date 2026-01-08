package com.example.budgettracker.service;

import com.example.budgettracker.dto.Goal.*;
import com.example.budgettracker.model.*;
import com.example.budgettracker.model.enums.AccountType;
import com.example.budgettracker.model.enums.CashFlowType;
import com.example.budgettracker.model.enums.GoalStatus;
import com.example.budgettracker.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class GoalServiceImpl implements GoalService {

    private final UserRepository userRepo;
    private final AccountRepository accountRepo;
    private final GoalRepository goalRepo;
    private final CashFlowRepository cashFlowRepo;
    private final CategoryRepository categoryRepo;
    private final TransferRepository transferRepo;


    public GoalServiceImpl(UserRepository userRepo,
                           AccountRepository accountRepo,
                           GoalRepository goalRepo,
                           CashFlowRepository cashFlowRepo,
                           CategoryRepository categoryRepo,
                           TransferRepository transferRepo) {
        this.userRepo = userRepo;
        this.accountRepo = accountRepo;
        this.goalRepo = goalRepo;
        this.cashFlowRepo = cashFlowRepo;
        this.categoryRepo = categoryRepo;
        this.transferRepo = transferRepo;
    }

    @Override
    public GoalResponse create(Long userId, CreateGoalRequest request) {
        GoalRequest goalRequest = new GoalRequest();
        goalRequest.setName(request.getName());
        goalRequest.setTargetAmount(request.getTargetAmount());
        goalRequest.setDueDate(request.getDueDate());
        return createGoal(userId, goalRequest);
    }

    @Override
    public GoalResponse createGoal(Long userId, GoalRequest request) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        boolean goalExists = goalRepo.existsByUser_UserIdAndNameIgnoreCase(userId, request.getName());
        if (goalExists) {
            throw new IllegalArgumentException("Goal with name '" + request.getName() + "' already exists");
        }

        boolean accountExistsForUser = accountRepo.existsByUser_UserIdAndNameIgnoreCase(userId, request.getName());
        if (accountExistsForUser) {
            throw new IllegalArgumentException("Account with name '" + request.getName() + "' already exists");
        }


        Account goalAccount = new Account();
        goalAccount.setUser(user);
        goalAccount.setAccountType(AccountType.GOAL);
        goalAccount.setName(request.getName());
        goalAccount.setBalance(BigDecimal.ZERO);
        try {
            accountRepo.save(goalAccount);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("Account name '" + request.getName() + "' is already in use for this user", e);
        }


        Goal goal = new Goal();
        goal.setUser(user);
        goal.setName(request.getName());
        goal.setTargetAmount(request.getTargetAmount());
        goal.setDueDate(request.getDueDate());
        goal.setLinkedAccount(goalAccount);
        goal.setCurrentAmount(BigDecimal.ZERO);
        goal.setStatus(GoalStatus.IN_PROGRESS);
        goalRepo.save(goal);

        return toResponse(goal);
    }

    @Override
    public List<GoalResponse> list(Long userId) {
        return getAllGoals(userId);
    }

    @Override
    public List<GoalResponse> getAllGoals(Long userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        return goalRepo.findByUser(user).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public GoalResponse contribute(Long userId, Long goalId, ContributionRequest req) {
        // Set the goalId in the request if not already set
        if (req.getGoalId() == null) {
            req.setGoalId(goalId);
        }
        contributeToGoal(userId, req);
        return getGoalById(userId, goalId);
    }

    @Override
    public ContributionResponse contributeToGoal(Long userId, ContributionRequest req) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        Account fromAccount = accountRepo.findById(req.getFromAccountId())
                .orElseThrow(() -> new IllegalArgumentException("Cash account not found"));
        if (!fromAccount.getUser().getUserId().equals(userId)) {
            throw new IllegalArgumentException("Source account not owned by this user");
        }

        Goal goal = goalRepo.findById(req.getGoalId())
                .orElseThrow(() -> new IllegalArgumentException("Goal not found"));
        Account goalAccount = goal.getLinkedAccount();

        BigDecimal amount = req.getAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Contribution amount must be positive");
        }

        if (fromAccount.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient funds in cash account");
        }


        Category transferCategory = ensureGoalTransferCategory(user);


        LocalDateTime now = LocalDateTime.now();

        String goalName = goal.getName() != null ? goal.getName() : "Goal";
        String outflowDescription = "Goal contribution to " + goalName;
        String inflowDescription = "Goal contribution from " + fromAccount.getName();

        // From Account - Expense
        CashFlow outflow = new CashFlow();
        outflow.setAccount(fromAccount);
        outflow.setCategory(transferCategory);
        outflow.setType(CashFlowType.Expense);
        outflow.setAmount(amount);
        outflow.setOccurredAt(now);
        outflow.setDescription(outflowDescription);
        cashFlowRepo.save(outflow);

        // To Goal Account - Income
        CashFlow inflow = new CashFlow();
        inflow.setAccount(goalAccount);
        inflow.setCategory(transferCategory);
        inflow.setType(CashFlowType.Income);
        inflow.setAmount(amount);
        inflow.setOccurredAt(now);
        inflow.setDescription(inflowDescription);
        cashFlowRepo.save(inflow);

        Transfer transfer = new Transfer();
        transfer.setAccountFrom(fromAccount);
        transfer.setAccountTo(goalAccount);
        transfer.setAmount(amount);
        transfer.setCreatedAt(LocalDateTime.now());
        transferRepo.save(transfer);


        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        accountRepo.save(fromAccount);

        goalAccount.setBalance(goalAccount.getBalance().add(amount));
        accountRepo.save(goalAccount);


        goal.setCurrentAmount(goal.getCurrentAmount().add(amount));
        if (goal.getCurrentAmount().compareTo(goal.getTargetAmount()) >= 0) {
            goal.setStatus(GoalStatus.COMPLETED);
        }
        goalRepo.save(goal);

        ContributionResponse response = new ContributionResponse();
        response.setMessage("Contribution successful");
        response.setNewCashBalance(fromAccount.getBalance());
        response.setNewGoalBalance(goalAccount.getBalance());
        return response;
    }

    private Category ensureGoalTransferCategory(User user) {
        final String preferredName = "Goal Transfer";
        final String legacyName = "__Goal_Transfer__";
        Long userId = user.getUserId();

        return categoryRepo.findByUser_UserIdAndNameIgnoreCase(userId, preferredName)
                .orElseGet(() -> {
                    Category category = categoryRepo.findByUser_UserIdAndNameIgnoreCase(userId, legacyName)
                            .map(existing -> {
                                if (!preferredName.equalsIgnoreCase(existing.getName())) {
                                    existing.setName(preferredName);
                                    try {
                                        return categoryRepo.save(existing);
                                    } catch (DataIntegrityViolationException e) {
                                        return categoryRepo.findByUser_UserIdAndNameIgnoreCase(userId, preferredName)
                                                .orElse(existing);
                                    }
                                }
                                return existing;
                            })
                            .orElseGet(() -> {
                                Category c = new Category();
                                c.setUser(user);
                                c.setName(preferredName);
                                c.setSystem(true);
                                try {
                                    return categoryRepo.save(c);
                                } catch (DataIntegrityViolationException e) {
                                    return categoryRepo.findByUser_UserIdAndNameIgnoreCase(userId, preferredName)
                                            .orElseThrow(() -> new RuntimeException("System category creation failed"));
                                }
                            });
                    return category;
                });
    }

    private GoalResponse toResponse(Goal goal) {
        GoalResponse res = new GoalResponse();
        res.setId(goal.getId());
        res.setName(goal.getName());
        res.setTargetAmount(goal.getTargetAmount());
        res.setCurrentAmount(goal.getCurrentAmount());
        res.setDueDate(goal.getDueDate());
        res.setStatus(goal.getStatus().name());
        if (goal.getTargetAmount() != null && goal.getTargetAmount().compareTo(BigDecimal.ZERO) > 0) {
            res.setProgress(goal.getCurrentAmount()
                    .divide(goal.getTargetAmount(), 2, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue());
        } else {
            res.setProgress(0.0);
        }
        return res;
    }

    @Override
    public GoalResponse get(Long userId, Long goalId) {
        return getGoalById(userId, goalId);
    }

    @Override
    public GoalResponse getGoalById(Long userId, Long goalId) {
        Goal goal = goalRepo.findById(goalId)
                .orElseThrow(() -> new IllegalArgumentException("Goal not found"));
        if (!goal.getUser().getUserId().equals(userId)) {
            throw new IllegalArgumentException("Goal not owned by user");
        }
        return toResponse(goal);
    }

    @Override
    public GoalResponse update(Long userId, Long goalId, UpdateGoalRequest request) {
        GoalRequest goalRequest = new GoalRequest();
        goalRequest.setName(request.getName());
        goalRequest.setTargetAmount(request.getTargetAmount());
        goalRequest.setDueDate(request.getDueDate());
        return updateGoal(userId, goalId, goalRequest);
    }

    @Override
    public GoalResponse updateGoal(Long userId, Long goalId, GoalRequest request) {
        Goal goal = goalRepo.findById(goalId)
                .orElseThrow(() -> new IllegalArgumentException("Goal not found"));
        if (!goal.getUser().getUserId().equals(userId)) {
            throw new IllegalArgumentException("Goal not owned by user");
        }

        if (request.getName() != null && !request.getName().isBlank()) {
            boolean exists = goalRepo.existsByUser_UserIdAndNameIgnoreCase(userId, request.getName());
            if (exists && !goal.getName().equalsIgnoreCase(request.getName())) {
                throw new IllegalArgumentException("Goal with name '" + request.getName() + "' already exists");
            }
            goal.setName(request.getName());
        }
        if (request.getTargetAmount() != null) {
            goal.setTargetAmount(request.getTargetAmount());
        }
        if (request.getDueDate() != null) {
            goal.setDueDate(request.getDueDate());
        }

        return toResponse(goalRepo.save(goal));
    }

    @Override
    public void delete(Long userId, Long goalId) {
        deleteGoal(userId, goalId);
    }

    @Override
    @Transactional
    public void deleteGoal(Long userId, Long goalId) {
        Goal goal = goalRepo.findById(goalId)
                .orElseThrow(() -> new IllegalArgumentException("Goal not found"));
        if (!goal.getUser().getUserId().equals(userId)) {
            throw new IllegalArgumentException("Goal not owned by user");
        }

        Account goalAccount = goal.getLinkedAccount();
        BigDecimal remain = goal.getCurrentAmount() == null ? BigDecimal.ZERO : goal.getCurrentAmount();

        if (remain.compareTo(BigDecimal.ZERO) > 0) {
            Account cashAccount = accountRepo.findFirstByUser_UserIdAndAccountType(userId, AccountType.CASH)
                    .orElseGet(() -> {
                        Account acc = new Account();
                        acc.setUser(goal.getUser());
                        acc.setName("Cash");
                        acc.setAccountType(AccountType.CASH);
                        acc.setBalance(BigDecimal.ZERO);
                        return accountRepo.save(acc);
                    });

            Category xferCat = ensureGoalTransferCategory(goal.getUser());
            LocalDateTime now = LocalDateTime.now();

            String goalName = goal.getName() != null ? goal.getName() : "Goal";
            Transfer transfer = new Transfer();
            transfer.setAccountFrom(goalAccount);
            transfer.setAccountTo(cashAccount);
            transfer.setAmount(remain);
            transfer.setCreatedAt(LocalDateTime.now());
            transferRepo.save(transfer);

            // Goal → Cash: Expense
            CashFlow out = new CashFlow();
            out.setAccount(goalAccount);
            out.setCategory(xferCat);
            out.setType(CashFlowType.Expense);
            out.setAmount(remain);
            out.setOccurredAt(now);
            out.setDescription("Goal refund to cash from " + goalName);
            cashFlowRepo.save(out);

            // Cash ← Goal: Income
            CashFlow in = new CashFlow();
            in.setAccount(cashAccount);
            in.setCategory(xferCat);
            in.setType(CashFlowType.Income);
            in.setAmount(remain);
            in.setOccurredAt(now);
            in.setDescription("Goal refund received from " + goalName);
            cashFlowRepo.save(in);


            goalAccount.setBalance(goalAccount.getBalance().subtract(remain));
            accountRepo.save(goalAccount);

            cashAccount.setBalance(cashAccount.getBalance().add(remain));
            accountRepo.save(cashAccount);

            goal.setCurrentAmount(BigDecimal.ZERO);
        }

        goalRepo.delete(goal);
    }

    @Override
    public List<CashFlow> getContributions(Long userId, Long goalId, LocalDate from, LocalDate to) {
        Goal goal = goalRepo.findById(goalId)
                .orElseThrow(() -> new IllegalArgumentException("Goal not found"));
        if (!goal.getUser().getUserId().equals(userId)) {
            throw new IllegalArgumentException("Goal not owned by user");
        }
        LocalDateTime fromDate = (from != null)
                ? from.atStartOfDay()
                : LocalDate.of(1970, 1, 1).atStartOfDay();
        LocalDateTime toDate = (to != null)
                ? to.atTime(LocalTime.MAX)
                : LocalDate.of(3000, 12, 31).atTime(LocalTime.MAX);

        return cashFlowRepo.findByUserCategoryPeriodAndOptionalType(
                userId,
                ensureGoalTransferCategory(goal.getUser()).getCategoryId(),
                fromDate,
                toDate,
                null
        );
    }
}
