/**
 * FR-7: Expense Recording Service
 * Handles expense transaction creation and validation
 */
const expenseService = {
    /**
     * Get authorization headers with Bearer token
     */
    getAuthHeaders() {
        const headers = {
            'Content-Type': 'application/json'
        };

        const sessionToken = localStorage.getItem('sessionToken');
        if (sessionToken) {
            headers['Authorization'] = `Bearer ${sessionToken}`;
        } else {
            console.warn('⚠️ No session token found - user may not be authenticated');
        }

        return headers;
    },

    /**
     * Add a new expense transaction
     * @param {Object} expenseData - {amount, description, categoryId, accountId, occurredAt}
     * @returns {Promise<Object>} Response from server
     */
    async addExpense(expenseData) {
        try {
            const sessionToken = localStorage.getItem('sessionToken');
            if (!sessionToken) {
                throw new Error('Not authenticated. Please log in.');
            }

            const response = await fetch('/api/expenses', {
                method: 'POST',

                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${sessionToken}`
                },

                body: JSON.stringify(expenseData)
            });

            if (!response.ok) {
                const errorData = await response.json();
                const detail = errorData.detail ? ` (${errorData.detail})` : '';
                throw new Error((errorData.message || 'Failed to add expense') + detail);
            }

            return await response.json();
        } catch (error) {
            console.error('Error adding expense:', error);
            throw error;
        }
    },

    /**
     * Get all expense transactions
     * @returns {Promise<Array>} List of expenses
     */
    async getAllExpenses() {
        try {

            const sessionToken = localStorage.getItem('sessionToken');
            if (!sessionToken) {
                throw new Error('Not authenticated. Please log in.');
            }

            const response = await fetch('/api/expenses', {
                headers: {
                    'Authorization': `Bearer ${sessionToken}`
                }

            });
            if (!response.ok) {
                throw new Error('Failed to fetch expenses');
            }
            return await response.json();
        } catch (error) {
            console.error('Error fetching expenses:', error);
            throw error;
        }
    },

    /**
     * Validate expense data before submission
     * FR-7 validation: amount > 0, date valid, category exists
     * @param {Object} expenseData
     * @returns {Object} {isValid: boolean, errors: string[]}
     */
    validateExpenseData(expenseData) {
        const errors = [];

        // Validate amount
        if (!expenseData.amount || parseFloat(expenseData.amount) <= 0) {
            errors.push('Amount must be greater than 0');
        }

        // Validate description
        if (!expenseData.description || expenseData.description.trim() === '') {
            errors.push('Description is required');
        }

        // Validate date
        if (!expenseData.occurredAt) {
            errors.push('Date is required');
        } else {
            const expenseDate = new Date(expenseData.occurredAt);
            const today = new Date();
            if (expenseDate > today) {
                errors.push('Date cannot be in the future');
            }
        }

        // Validate account
        if (!expenseData.accountId) {
            errors.push('Account is required');
        }

        return {
            isValid: errors.length === 0,
            errors: errors
        };
    },

    /**
     * Format amount for display
     * @param {number} amount
     * @returns {string} Formatted amount (e.g., "$123.45")
     */
    formatAmount(amount) {
        return new Intl.NumberFormat('en-US', {
            style: 'currency',
            currency: 'USD'
        }).format(amount);
    },

    /**
     * Show success message to user
     * @param {string} message
     */
    showSuccessMessage(message) {
        const notification = document.createElement('div');
        notification.className = 'success-toast';
        notification.style.cssText = `
            position: fixed;
            top: 100px;
            right: 2rem;
            background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
            color: white;
            padding: 1rem 1.5rem;
            border-radius: 12px;
            font-size: 0.875rem;
            font-weight: 600;
            z-index: 1000;
            animation: slideInRight 0.5s ease-out;
            box-shadow: 0 4px 15px rgba(239, 68, 68, 0.3);
        `;
        notification.innerHTML = `
            <div style="display: flex; align-items: center; gap: 0.5rem;">
                <span style="font-size: 1.25rem;">✅</span>
                <span>${message}</span>
            </div>
        `;
        document.body.appendChild(notification);

        setTimeout(() => notification.remove(), 3000);
    },

    /**
     * Show error message to user
     * @param {string} message
     */
    showErrorMessage(message) {
        alert('❌ Error: ' + message);
    },

    /**
     * Add expense to the transactions list in UI
     * @param {Object} expenseData
     * @param {Object} response - API response
     */
    addExpenseToTransactionsList(expenseData, response) {
        // Instead of manually adding to DOM, reload all transactions to ensure proper date grouping
        if (typeof loadTransactions === 'function') {
            loadTransactions();
        }
        return;

        // Old code below - kept for reference but not executed
        const transactionsList = document.getElementById('transactions-list');
        if (!transactionsList) return;

        const newTransaction = this.createExpenseTransactionElement(expenseData, response);
        transactionsList.insertBefore(newTransaction, transactionsList.firstChild);
    },

    /**
     * Create transaction HTML element
     * @param {Object} expenseData
     * @param {Object} response
     * @returns {HTMLElement}
     */
    createExpenseTransactionElement(expenseData, response) {
        const div = document.createElement('div');
        div.className = 'transaction-item';
        div.setAttribute('data-type', 'expense');
        div.setAttribute('data-category', response.categoryName || 'other');

        const categoryEmoji = this.getCategoryEmoji(response.categoryName);
        const formattedDate = this.formatDate(expenseData.occurredAt);

        div.innerHTML = `
            <div class="transaction-icon expense">${categoryEmoji}</div>
            <div class="transaction-details">
                <div class="transaction-title">${expenseData.description}</div>
                <div class="transaction-category">${categoryEmoji} ${response.categoryName}</div>
                <div class="transaction-date">${formattedDate}</div>
            </div>
            <div class="transaction-amount">
                <div class="amount-value expense">-${this.formatAmount(expenseData.amount)}</div>
                <div class="transaction-status status-completed">Completed</div>
            </div>
        `;

        return div;
    },

    /**
     * Get emoji for category
     * @param {string} categoryName
     * @returns {string} emoji
     */
    getCategoryEmoji(categoryName) {
        // Normalize category name to lowercase for matching
        const normalized = (categoryName || '').toLowerCase().trim();

        const categoryEmojis = {
            // Food categories
            'food': '🍔',
            'food & dining': '🍔',
            'groceries': '🛒',

            // Transport
            'transport': '🚗',
            'transportation': '🚗',

            // Shopping
            'shopping': '🛍️',

            // Entertainment
            'entertainment': '🎮',

            // Education
            'education': '📚',

            // Health
            'health': '🏥',

            // Fitness
            'fitness': '💪',

            // Utilities & Bills
            'utilities': '💡',
            'bills': '🏠',
            'bills & utilities': '💡',

            // Housing
            'rent': '🏠',
            'housing': '🏠',

            // Income
            'income': '💰',

            // Default
            'other': '💸',
            'uncategorized': '💸'
        };

        return categoryEmojis[normalized] || '💸';
    },

    /**
     * Format date for display
     * @param {string} dateString
     * @returns {string}
     */
    formatDate(dateString) {
        const date = new Date(dateString);
        const today = new Date();
        const yesterday = new Date(today);
        yesterday.setDate(yesterday.getDate() - 1);

        if (date.toDateString() === today.toDateString()) {
            return 'Today • ' + date.toLocaleTimeString('en-US', { hour: 'numeric', minute: '2-digit' });
        } else if (date.toDateString() === yesterday.toDateString()) {
            return 'Yesterday • ' + date.toLocaleTimeString('en-US', { hour: 'numeric', minute: '2-digit' });
        } else {
            return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
        }
    },

    /**
     * Update dashboard after expense is added
     * @param {Object} expenseData
     * @param {Object} response
     */
    updateDashboardTransactions(expenseData, response) {
        // Update summary cards if they exist
        const expensesCard = document.querySelector('.summary-card.expenses .summary-value');
        if (expensesCard) {
            const currentExpenses = parseFloat(expensesCard.textContent.replace(/[$,]/g, ''));
            const newExpenses = currentExpenses + parseFloat(expenseData.amount);
            expensesCard.textContent = this.formatAmount(newExpenses);
        }

        // Update net income
        const netCard = document.querySelector('.summary-card.net .summary-value');
        if (netCard) {
            const currentNet = parseFloat(netCard.textContent.replace(/[$,]/g, ''));
            const newNet = currentNet - parseFloat(expenseData.amount);
            netCard.textContent = this.formatAmount(newNet);
        }

        // Update transaction count
        const countCard = document.querySelector('.summary-card.transactions .summary-value');
        if (countCard) {
            const currentCount = parseInt(countCard.textContent);
            countCard.textContent = currentCount + 1;
        }
    },

    /**
     * Update analytics after transaction
     * @param {number} amount
     * @param {string} type - 'income' or 'expense'
     */
    updateTransactionAnalytics(amount, type) {
        // This can be extended to update charts and analytics
        console.log(`Transaction recorded: ${type} of ${this.formatAmount(amount)}`);
    },

    /**
     * Load all expenses on page load
     */
    async loadExpenseTransactions() {
        try {
            const expenses = await this.getAllExpenses();
            console.log('Loaded expenses:', expenses.length);
            // Optionally populate the UI with loaded expenses
        } catch (error) {
            console.error('Failed to load expenses:', error);
        }
    }
};
