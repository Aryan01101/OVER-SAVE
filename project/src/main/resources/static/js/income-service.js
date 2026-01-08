// Income Service for OVER-SAVE Application
class IncomeService {
    constructor() {
        this.baseUrl = '/api/income';
        this.defaultAccountId = null;
    }
    getUserId() {
        const userInfo = JSON.parse(localStorage.getItem('userInfo'));
        return userInfo?.userId || null;
    }

    /**
     * Get authentication headers with Bearer token
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

        const userId = this.getUserId();
        if (userId) {
            headers['X-USER-ID'] = userId;
        }

        return headers;
    }

    /**
     * Get default account ID for the form
     */
    async getDefaultAccountId(forceRefresh = false) {
        if (!forceRefresh && this.defaultAccountId) {
            return this.defaultAccountId;
        }

        try {
            const response = await fetch('/api/income/default-account', {
                headers: this.getAuthHeaders()
            });
            if (!response.ok) {
                const errorData = await response.json().catch(() => ({}));
                const message = errorData.message || 'No default account available';
                const detail = errorData.detail || '';
                console.warn('⚠️ Failed to resolve default account:', message, detail);
                this.defaultAccountId = null;

                // Provide user-friendly error message
                const userMessage = detail ? `${message}: ${detail}` : message;
                throw new Error(userMessage);
            }

            const data = await response.json();
            if (!data.accountId) {
                this.defaultAccountId = null;
                throw new Error('No account returned from server');
            }

            this.defaultAccountId = data.accountId;
            console.log('📦 Default account ID:', this.defaultAccountId);
            return this.defaultAccountId;
        } catch (error) {
            console.warn('⚠️ Could not fetch default account:', error.message);
            this.defaultAccountId = null;

            // If this is a network error, provide helpful guidance
            if (error.name === 'TypeError') {
                throw new Error('Unable to connect to server. Please ensure you are logged in and try again.');
            }

            throw error;
        }
    }

    /**
     * Add a new income record
     * @param {Object} incomeData - Income data object
     * @param {number} incomeData.amount - Income amount
     * @param {string} incomeData.occurredAt - Date in YYYY-MM-DD format
     * @param {string} incomeData.description - Income description
     * @param {number} incomeData.accountId - Account ID (optional)
     * @returns {Promise<Object>} Response from server
     */
    async addIncome(incomeData) {
        console.log('🚀 Starting income submission...', incomeData);
        console.log('📡 API URL:', this.baseUrl);

        try {
            console.log('📤 Sending request with data:', JSON.stringify(incomeData, null, 2));

            const response = await fetch(this.baseUrl, {
                method: 'POST',
                headers: this.getAuthHeaders(),
                body: JSON.stringify(incomeData)
            });

            console.log('📡 Response status:', response.status);
            console.log('📡 Response headers:', response.headers);

            if (!response.ok) {
                console.error('❌ Response not OK, status:', response.status);
                const errorData = await response.json().catch(() => ({ message: 'Unknown error' }));
                console.error('❌ Error data:', errorData);
                const detail = errorData.detail ? ` (${errorData.detail})` : '';
                if (response.status === 401 || response.status === 403) {
                    // Account mismatch or authorization issue – refresh default account next time
                    this.defaultAccountId = null;
                }
                throw new Error((errorData.message || `HTTP error! status: ${response.status}`) + detail);
            }

            const result = await response.json();
            console.log('✅ Success! Response:', result);
            return result;
        } catch (error) {
            console.error('💥 Error adding income:', error);
            console.error('💥 Error type:', error.constructor.name);
            console.error('💥 Error message:', error.message);
            if (error.name === 'TypeError' && error.message.includes('fetch')) {
                console.error('🔍 This looks like a network connectivity issue. Check:');
                console.error('  - Is your server running on http://localhost:8080?');
                console.error('  - Are you accessing the page from http://localhost:8080?');
                console.error('  - Check browser Network tab for failed requests');
            }
            throw error;
        }
    }

    /**
     * Validate income data before submission
     * @param {Object} incomeData - Income data to validate
     * @returns {Object} Validation result with isValid and errors
     */
    /**
     * FR-8 Validation: Amount > 0, date valid, source required
     */
    validateIncomeData(incomeData) {
        const errors = [];

        // FR-8: Amount > 0
        if (!incomeData.amount || incomeData.amount <= 0) {
            errors.push('Amount must be greater than 0');
        }

        // FR-8: Date valid
        if (!incomeData.occurredAt) {
            errors.push('Date is required');
        }

        if (incomeData.occurredAt && new Date(incomeData.occurredAt) > new Date()) {
            errors.push('Date cannot be in the future');
        }

        // FR-8: Source (description) required
        if (!incomeData.description || incomeData.description.trim().length === 0) {
            errors.push('Source description is required');
        }

        // Internal requirement for account (simplified MVP)
        if (!incomeData.accountId) {
            errors.push('Account is required');
        }

        return {
            isValid: errors.length === 0,
            errors: errors
        };
    }

    /**
     * Format amount for display
     * @param {number} amount - Amount to format
     * @returns {string} Formatted amount
     */
    formatAmount(amount) {
        return new Intl.NumberFormat('en-US', {
            style: 'currency',
            currency: 'USD'
        }).format(amount);
    }

    /**
     * Format date for display
     * @param {string} dateString - Date string to format
     * @returns {string} Formatted date
     */
    formatDate(dateString) {
        return new Date(dateString).toLocaleDateString('en-US', {
            year: 'numeric',
            month: 'short',
            day: 'numeric'
        });
    }

    /**
     * Show success message to user
     * @param {string} message - Success message
     */
    showSuccessMessage(message) {
        this.showMessage(message, 'success');
    }

    /**
     * Fetch all income transactions from server
     * @returns {Promise<Array>} Array of income transactions
     */
    async fetchAllIncome() {
        try {
            const response = await fetch(this.baseUrl, {
                method: 'GET',
                headers: this.getAuthHeaders()
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            return await response.json();
        } catch (error) {
            console.error('Error fetching income transactions:', error);
            return [];
        }
    }

    /**
     * Load and display all income transactions on transactions page
     */
    async loadIncomeTransactions() {
        const transactionsList = document.getElementById('transactions-list');
        if (!transactionsList) return; // Not on transactions page

        try {
            const incomeTransactions = await this.fetchAllIncome();

            // Add each income transaction to the list
            incomeTransactions.forEach(income => {
                this.addIncomeToTransactionsListFromData(income);
            });

            // Initialize analytics after loading all transactions
            setTimeout(() => {
                this.initializeTransactionAnalytics();
            }, 100);

        } catch (error) {
            console.error('Error loading income transactions:', error);
        }
    }

    /**
     * Add income transaction from fetched data (slightly different from real-time add)
     * @param {Object} incomeData - Income data from server
     */
    addIncomeToTransactionsListFromData(incomeData) {
        // Instead of manually adding to DOM, reload all transactions to ensure proper date grouping
        if (typeof loadTransactions === 'function') {
            loadTransactions();
        }
        return;

        // Old code below - kept for reference but not executed
        const transactionsList = document.getElementById('transactions-list');
        if (!transactionsList) return;

        // Create new transaction item
        const transactionItem = document.createElement('div');
        transactionItem.className = 'transaction-item income-transaction';
        const categoryLabel = incomeData.categoryName || 'Income';
        const categorySlug = categoryLabel.toLowerCase();
        transactionItem.setAttribute('data-type', 'income');
        transactionItem.setAttribute('data-category', categorySlug);

        // Format the date
        const transactionDate = new Date(incomeData.occurredAt).toLocaleDateString('en-US', {
            year: 'numeric',
            month: 'short',
            day: 'numeric'
        });

        const createdTime = new Date(incomeData.createdAt).toLocaleTimeString('en-US', {
            hour: '2-digit',
            minute: '2-digit'
        });

        transactionItem.innerHTML = `
            <div class="transaction-icon income">💰</div>
            <div class="transaction-details">
                <div class="transaction-title">${incomeData.description}</div>
                <div class="transaction-category">💰 ${categoryLabel}</div>
                <div class="transaction-date">${transactionDate} • ${createdTime}</div>
            </div>
            <div class="transaction-amount">
                <div class="amount-value income">+${this.formatAmount(incomeData.amount)}</div>
                <div class="transaction-status">
                    <span class="status-badge completed">✓ Completed</span>
                </div>
            </div>
        `;

        // Insert at the top of the list (after any existing transactions)
        const firstTransaction = transactionsList.querySelector('.transaction-item');
        if (firstTransaction) {
            transactionsList.insertBefore(transactionItem, firstTransaction);
        } else {
            transactionsList.appendChild(transactionItem);
        }

        // Add click handler for transaction details
        transactionItem.addEventListener('click', () => {
            this.showTransactionDetails({
                amount: incomeData.amount,
                occurredAt: incomeData.occurredAt,
                description: incomeData.description
            }, incomeData);
        });
    }

    /**
     * Add income transaction to transactions page
     * @param {Object} incomeData - Income data that was successfully added
     * @param {Object} response - Response from server
     */
    addIncomeToTransactionsList(incomeData, response) {
        // Instead of manually adding to DOM, reload all transactions to ensure proper date grouping
        if (typeof loadTransactions === 'function') {
            loadTransactions();
        }
        return;

        // Old code below - kept for reference but not executed
        const transactionsList = document.getElementById('transactions-list');
        if (!transactionsList) return; // Not on transactions page

        // Create new transaction item
        const transactionItem = document.createElement('div');
        transactionItem.className = 'transaction-item income-transaction';
        const categoryLabel = incomeData.categoryName || 'Income';
        const categorySlug = categoryLabel.toLowerCase();
        transactionItem.setAttribute('data-type', 'income');
        transactionItem.setAttribute('data-category', categorySlug);

        // Format the date
        const transactionDate = new Date(incomeData.occurredAt).toLocaleDateString('en-US', {
            year: 'numeric',
            month: 'short',
            day: 'numeric'
        });

        const currentTime = new Date().toLocaleTimeString('en-US', {
            hour: '2-digit',
            minute: '2-digit'
        });

        // Simple income transaction display (FR-8)
        transactionItem.innerHTML = `
            <div class="transaction-icon income">💰</div>
            <div class="transaction-details">
                <div class="transaction-title">${incomeData.description}</div>
                <div class="transaction-category">💰 ${categoryLabel}</div>
                <div class="transaction-date">${transactionDate} • ${currentTime}</div>
            </div>
            <div class="transaction-amount">
                <div class="amount-value income">+${this.formatAmount(incomeData.amount)}</div>
                <div class="transaction-status">
                    <span class="status-badge completed">✓ Completed</span>
                </div>
            </div>
        `;

        // Add animation for new transaction
        transactionItem.style.opacity = '0';
        transactionItem.style.transform = 'translateY(-20px)';

        // Insert at the top of the list
        const firstTransaction = transactionsList.querySelector('.transaction-item');
        if (firstTransaction) {
            transactionsList.insertBefore(transactionItem, firstTransaction);
        } else {
            transactionsList.appendChild(transactionItem);
        }

        // Animate in
        setTimeout(() => {
            transactionItem.style.transition = 'all 0.5s ease-out';
            transactionItem.style.opacity = '1';
            transactionItem.style.transform = 'translateY(0)';
        }, 100);

        // Add click handler for transaction details
        transactionItem.addEventListener('click', () => {
            this.showTransactionDetails(incomeData, response);
        });

        // Update analytics after adding transaction
        this.updateTransactionAnalytics(incomeData.amount, 'income');
    }

    /**
     * Show transaction details modal
     * @param {Object} incomeData - Original income data
     * @param {Object} response - Server response
     */
    showTransactionDetails(incomeData, response) {
        const modal = document.createElement('div');
        modal.className = 'transaction-detail-modal';
        modal.innerHTML = `
            <div class="modal-content">
                <div class="modal-header">
                    <h3>💰 Income Transaction Details</h3>
                    <span class="close" onclick="this.closest('.transaction-detail-modal').remove()">&times;</span>
                </div>
                <div class="transaction-detail-body">
                    <div class="detail-row">
                        <label>Amount:</label>
                        <span class="income-amount">+${this.formatAmount(incomeData.amount)}</span>
                    </div>
                    <div class="detail-row">
                        <label>Description:</label>
                        <span>${incomeData.description}</span>
                    </div>
                    <div class="detail-row">
                        <label>Date:</label>
                        <span>${this.formatDate(incomeData.occurredAt)}</span>
                    </div>
                    <div class="detail-row">
                        <label>Added:</label>
                        <span>${new Date(response.createdAt).toLocaleString('en-US')}</span>
                    </div>
                    <div class="detail-row">
                        <label>Updated Balance:</label>
                        <span class="balance">${this.formatAmount(response.updatedBalance)}</span>
                    </div>
                    <div class="detail-row">
                        <label>Status:</label>
                        <span class="status-badge completed">✓ Completed</span>
                    </div>
                </div>
            </div>
        `;

        // Style the modal
        modal.style.cssText = `
            position: fixed;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            background: rgba(0, 0, 0, 0.5);
            display: flex;
            align-items: center;
            justify-content: center;
            z-index: 10000;
            animation: fadeIn 0.3s ease-out;
        `;

        document.body.appendChild(modal);

        // Close on outside click
        modal.addEventListener('click', (e) => {
            if (e.target === modal) {
                modal.remove();
            }
        });
    }

    /**
     * Update dashboard recent transactions
     * @param {Object} incomeData - Income data
     * @param {Object} response - Server response
     */
    updateDashboardTransactions(incomeData, response) {
        // Update recent transactions section on dashboard
        const transactionList = document.querySelector('.transaction-list');
        if (!transactionList) return; // Not on dashboard

        const transactionItem = document.createElement('div');
        transactionItem.className = 'transaction-item income-transaction';

        const transactionDate = new Date(incomeData.occurredAt).toLocaleDateString('en-US', {
            month: 'short',
            day: 'numeric',
            year: 'numeric'
        });

        const currentTime = new Date().toLocaleTimeString('en-US', {
            hour: '2-digit',
            minute: '2-digit'
        });

        // Simple income display (FR-8)
        const iconStyle = "background: #d1fae5;";
        const icon = '💰';
        const category = 'Income';

        transactionItem.innerHTML = `
            <div class="transaction-icon" style="${iconStyle}">${icon}</div>
            <div class="transaction-details">
                <div class="transaction-title">${incomeData.description}</div>
                <div class="transaction-category">${category} • ${transactionDate}, ${currentTime}</div>
            </div>
            <div class="transaction-amount income">+${this.formatAmount(incomeData.amount)}</div>
        `;

        // Add animation
        transactionItem.style.opacity = '0';
        transactionItem.style.transform = 'translateY(-10px)';

        // Add to top of recent transactions
        const firstItem = transactionList.querySelector('.transaction-item');
        if (firstItem) {
            transactionList.insertBefore(transactionItem, firstItem);
        } else {
            transactionList.appendChild(transactionItem);
        }

        // Animate in
        setTimeout(() => {
            transactionItem.style.transition = 'all 0.3s ease-out';
            transactionItem.style.opacity = '1';
            transactionItem.style.transform = 'translateY(0)';
        }, 50);

        // Limit to 5 recent transactions
        const items = transactionList.querySelectorAll('.transaction-item');
        if (items.length > 5) {
            items[items.length - 1].remove();
        }
    }

    /**
     * Show error message to user
     * @param {string} message - Error message
     */
    showErrorMessage(message) {
        this.showMessage(message, 'error');
    }

    /**
     * Generic message display function
     * @param {string} message - Message text
     * @param {string} type - Message type (success, error, info)
     */
    showMessage(message, type = 'info') {
        // Remove existing messages
        const existingMessages = document.querySelectorAll('.income-message');
        existingMessages.forEach(msg => msg.remove());

        const messageDiv = document.createElement('div');
        messageDiv.className = `income-message ${type}`;
        messageDiv.style.cssText = `
            position: fixed;
            top: 20px;
            right: 20px;
            padding: 12px 20px;
            border-radius: 8px;
            font-weight: 500;
            z-index: 10000;
            min-width: 300px;
            box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
            animation: slideIn 0.3s ease-out;
        `;

        switch (type) {
            case 'success':
                messageDiv.style.backgroundColor = '#10b981';
                messageDiv.style.color = 'white';
                messageDiv.innerHTML = `✅ ${message}`;
                break;
            case 'error':
                messageDiv.style.backgroundColor = '#ef4444';
                messageDiv.style.color = 'white';
                messageDiv.innerHTML = `❌ ${message}`;
                break;
            default:
                messageDiv.style.backgroundColor = '#3b82f6';
                messageDiv.style.color = 'white';
                messageDiv.innerHTML = `ℹ️ ${message}`;
        }

        document.body.appendChild(messageDiv);

        // Auto remove after 5 seconds
        setTimeout(() => {
            if (messageDiv.parentNode) {
                messageDiv.style.animation = 'slideOut 0.3s ease-in';
                setTimeout(() => messageDiv.remove(), 300);
            }
        }, 5000);
    }

    /**
     * Update transaction analytics in summary cards
     * @param {number} amount - Transaction amount
     * @param {string} type - Transaction type ('income' or 'expense')
     */
    updateTransactionAnalytics(amount, type) {
        // Only update if we're on the transactions page
        const summaryCards = document.querySelector('.summary-cards');
        if (!summaryCards) return;

        // Get current values from the DOM using IDs
        const totalIncomeElement = document.getElementById('total-income-summary');
        const totalExpensesElement = document.getElementById('total-expenses-summary');
        const netIncomeElement = document.getElementById('net-income-summary');
        const totalTransactionsElement = document.getElementById('total-transactions-summary');

        if (!totalIncomeElement || !totalExpensesElement || !netIncomeElement || !totalTransactionsElement) {
            console.warn('Could not find all summary card elements');
            return;
        }

        // Parse current values (remove $ and commas)
        const currentTotalIncome = parseFloat(totalIncomeElement.textContent.replace(/[$,]/g, '')) || 0;
        const currentTotalExpenses = parseFloat(totalExpensesElement.textContent.replace(/[$,]/g, '')) || 0;
        const currentTotalTransactions = parseInt(totalTransactionsElement.textContent.replace(/[,]/g, '')) || 0;

        let newTotalIncome = currentTotalIncome;
        let newTotalExpenses = currentTotalExpenses;
        const newTotalTransactions = currentTotalTransactions + 1;

        // Update based on transaction type
        if (type === 'income') {
            newTotalIncome += amount;
        } else if (type === 'expense') {
            newTotalExpenses += amount;
        }

        // Calculate new net income
        const newNetIncome = newTotalIncome - newTotalExpenses;

        // Update the UI with animation
        this.animateValueUpdate(totalIncomeElement, currentTotalIncome, newTotalIncome, '$');
        this.animateValueUpdate(totalExpensesElement, currentTotalExpenses, newTotalExpenses, '$');
        this.animateValueUpdate(netIncomeElement, currentTotalIncome - currentTotalExpenses, newNetIncome, '$');
        this.animateValueUpdate(totalTransactionsElement, currentTotalTransactions, newTotalTransactions, '');

        console.log('📊 Analytics updated:', {
            totalIncome: newTotalIncome,
            totalExpenses: newTotalExpenses,
            netIncome: newNetIncome,
            totalTransactions: newTotalTransactions
        });
    }

    /**
     * Animate value changes in summary cards
     * @param {HTMLElement} element - Element to update
     * @param {number} oldValue - Previous value
     * @param {number} newValue - New value
     * @param {string} prefix - Currency symbol or other prefix
     */
    animateValueUpdate(element, oldValue, newValue, prefix = '') {
        if (oldValue === newValue) return;

        // Add highlight animation
        element.style.transition = 'all 0.3s ease';
        element.style.background = '#e0f2fe';
        element.style.transform = 'scale(1.05)';
        element.style.borderRadius = '8px';
        element.style.padding = '4px 8px';

        // Format the new value
        const formattedValue = prefix ?
            `${prefix}${Math.abs(newValue).toLocaleString('en-US', { minimumFractionDigits: 0, maximumFractionDigits: 0 })}` :
            newValue.toLocaleString('en-US');

        // Update the text
        setTimeout(() => {
            element.textContent = formattedValue;
        }, 150);

        // Reset styling
        setTimeout(() => {
            element.style.background = '';
            element.style.transform = '';
            element.style.padding = '';
        }, 600);
    }

    /**
     * Initialize analytics by calculating totals from existing transactions
     */
    initializeTransactionAnalytics() {
        const transactionsList = document.getElementById('transactions-list');
        if (!transactionsList) return;

        let totalIncome = 0;
        let totalExpenses = 0;
        let totalTransactions = 0;

        // Calculate totals from all existing transactions in the DOM
        const transactionItems = transactionsList.querySelectorAll('.transaction-item');
        transactionItems.forEach(item => {
            const amountElement = item.querySelector('.amount-value');
            if (amountElement) {
                const amountText = amountElement.textContent;
                const amount = parseFloat(amountText.replace(/[+\-$,]/g, '')) || 0;

                if (amountText.includes('+')) {
                    totalIncome += amount;
                } else if (amountText.includes('-')) {
                    totalExpenses += amount;
                }
                totalTransactions++;
            }
        });

        // Update summary cards using IDs for more reliable selection
        const totalIncomeElement = document.getElementById('total-income-summary');
        const totalExpensesElement = document.getElementById('total-expenses-summary');
        const netIncomeElement = document.getElementById('net-income-summary');
        const totalTransactionsElement = document.getElementById('total-transactions-summary');

        if (totalIncomeElement) totalIncomeElement.textContent = `$${totalIncome.toLocaleString('en-US')}`;
        if (totalExpensesElement) totalExpensesElement.textContent = `$${totalExpenses.toLocaleString('en-US')}`;
        if (netIncomeElement) netIncomeElement.textContent = `$${(totalIncome - totalExpenses).toLocaleString('en-US')}`;
        if (totalTransactionsElement) totalTransactionsElement.textContent = totalTransactions.toString();

        console.log('📊 Analytics initialized:', {
            totalIncome,
            totalExpenses,
            netIncome: totalIncome - totalExpenses,
            totalTransactions
        });
    }
}

// Create global instance
const incomeService = new IncomeService();

// Add CSS animations
const incomeServiceStyles = document.createElement('style');
incomeServiceStyles.textContent = `
    @keyframes slideIn {
        from {
            transform: translateX(100%);
            opacity: 0;
        }
        to {
            transform: translateX(0);
            opacity: 1;
        }
    }

    @keyframes slideOut {
        from {
            transform: translateX(0);
            opacity: 1;
        }
        to {
            transform: translateX(100%);
            opacity: 0;
        }
    }
`;
document.head.appendChild(incomeServiceStyles);
