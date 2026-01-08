// Shared FAB actions for all pages
(function() {
    const ensureServices = () => {
        if (!window.incomeService || !window.expenseService) {
            console.warn('FABActions: Missing income/expense services. Ensure income-service.js and expense-service.js are loaded.');
            return false;
        }
        return true;
    };

    const openIncomeModal = () => {
        if (!ensureServices()) {
            window.location.href = '/html/transactions_page.html';
            return;
        }

        if (typeof window.openIncomeModal === 'function') {
            window.openIncomeModal();
            return;
        }

        if (typeof window.openAddTransactionModal === 'function') {
            window.openAddTransactionModal('income');
            return;
        }

        const modal = document.getElementById('incomeModal');
        if (modal) {
            modal.classList.add('show');
            modal.style.display = 'flex';
            return;
        }

        window.location.href = '/html/transactions_page.html?open=income';
    };

    const openExpenseModal = () => {
        if (!ensureServices()) {
            window.location.href = '/html/transactions_page.html';
            return;
        }

        if (typeof window.openExpenseModal === 'function') {
            window.openExpenseModal();
            return;
        }

        if (typeof window.openAddTransactionModal === 'function') {
            window.openAddTransactionModal('expense');
            return;
        }

        const modal = document.getElementById('expenseModal');
        if (modal) {
            modal.classList.add('show');
            modal.style.display = 'flex';
            return;
        }

        window.location.href = '/html/transactions_page.html?open=expense';
    };

    const openBudget = () => {
        if (typeof window.openSetBudgetModal === 'function') {
            window.openSetBudgetModal();
        } else {
            window.location.href = '/html/budgets_page.html';
        }
    };

    const openGoal = () => {
        if (typeof window.openCreateGoalModal === 'function') {
            window.openCreateGoalModal();
        } else {
            window.location.href = '/html/goals_page.html';
        }
    };

    window.FABActions = {
        openIncome: openIncomeModal,
        openExpense: openExpenseModal,
        openBudget,
        openGoal
    };
})();
