// Transaction management functionality
let transactions = [];
let filteredTransactions = [];
let currentFilters = {
    search: '',
    category: '',
    type: '',
    date: 'all'
};
let currentTransactionType = window.currentTransactionType || 'expense';

// Initialize when DOM loads
document.addEventListener('DOMContentLoaded', function() {
    initializeTransactions();
    setupEventListeners();
    loadTransactions();
});

// Refresh data when page becomes visible (user returns from another page)
document.addEventListener('visibilitychange', function() {
    if (!document.hidden) {
        console.log('🔄 Page visible - refreshing transactions...');
        loadTransactions();
    }
});

// Refresh data when window gains focus
window.addEventListener('focus', function() {
    console.log('🔄 Window focused - refreshing transactions...');
    loadTransactions();
});

// Listen for category changes (e.g., after merge operations)
window.addEventListener('categoriesChanged', async function(event) {
    console.log('🔄 Categories changed - refreshing transactions...', event.detail);
    await loadTransactions();

    // Also reload categories if we have a loadCategories function
    if (typeof window.loadCategories === 'function') {
        await window.loadCategories();
    }
});

function initializeTransactions() {
    // Initialize empty - transactions will be loaded from API
    transactions = [];
}

function setupEventListeners() {
    // Search functionality
    const searchInput = document.getElementById('transaction-search') || document.getElementById('search-input');
    if (searchInput) {
        searchInput.addEventListener('input', handleSearch);
    }

    // Filter dropdowns
    const categoryFilter = document.getElementById('category-filter');
    if (categoryFilter) {
        categoryFilter.addEventListener('change', handleCategoryFilter);
    }
    const typeFilter = document.getElementById('type-filter');
    if (typeFilter) {
        typeFilter.addEventListener('change', handleTypeFilter);
    }
    const dateFilter = document.getElementById('date-filter');
    if (dateFilter) {
        dateFilter.addEventListener('change', handleDateFilter);
    }

    // Transaction type selector in modal
    const typeOptions = document.querySelectorAll('.type-option');
    typeOptions.forEach(option => {
        option.addEventListener('click', function() {
            typeOptions.forEach(opt => opt.classList.remove('active'));
            this.classList.add('active');
        });
    });

    // Form submission
    const transactionForm = document.getElementById('full-transaction-form');
    if (transactionForm) {
        transactionForm.addEventListener('submit', handleAddTransaction);
    }
}

function handleSearch(event) {
    currentFilters.search = event.target.value.toLowerCase();
    applyFilters();
}

function handleCategoryFilter(event) {
    currentFilters.category = event.target.value;
    applyFilters();
}

function handleTypeFilter(event) {
    currentFilters.type = event.target.value;
    applyFilters();
}

function handleDateFilter(event) {
    currentFilters.date = event.target.value;
    applyFilters();
}

function applyFilters() {
    filteredTransactions = transactions.filter(transaction => {
        const title = (transaction.title || '').toLowerCase();
        const description = (transaction.description || '').toLowerCase();
        const matchesSearch = !currentFilters.search ||
            title.includes(currentFilters.search) ||
            description.includes(currentFilters.search);

        const transactionCategory = (transaction.category || '').toLowerCase().trim();
        const selectedCategory = (currentFilters.category || '').toLowerCase().trim();
        const matchesCategory = !selectedCategory ||
            transactionCategory === selectedCategory;

        const matchesType = !currentFilters.type ||
            transaction.type === currentFilters.type;

        const matchesDate = filterByDate(transaction.date, currentFilters.date);

        return matchesSearch && matchesCategory && matchesType && matchesDate;
    });

    renderTransactions();
    updateTransactionCount();
}

window.setTypeFilter = function(type, event) {
    // Highlight the active tab
    document.querySelectorAll('.filter-btn').forEach(btn => btn.classList.remove('active'));
    if (event && event.target) {
        event.target.classList.add('active');
    }

    // Update filter state
    currentFilters.type = (type === 'all') ? '' : type;

    // Apply unified filtering logic
    applyFilters();
};

function filterByDate(transactionDate, filter) {
    const now = new Date();
    const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());

    switch(filter) {
        case 'today':
            return transactionDate >= today;
        case 'week':
            const weekAgo = new Date(today.getTime() - 7 * 24 * 60 * 60 * 1000);
            return transactionDate >= weekAgo;
        case 'month':
            return transactionDate.getMonth() === now.getMonth() &&
                   transactionDate.getFullYear() === now.getFullYear();
        case 'year':
            return transactionDate.getFullYear() === now.getFullYear();
        default:
            return true;
    }
}

function parseTransactionDate(value) {
    if (!value) return new Date();

    if (value instanceof Date) {
        return new Date(value.getTime());
    }

    if (typeof value === 'number') {
        return new Date(value);
    }

    if (typeof value === 'string') {
        let normalized = value.trim();
        if (!normalized) return new Date();

        // Replace space separator with T for ISO compliance
        if (normalized.includes(' ') && !normalized.includes('T')) {
            normalized = normalized.replace(' ', 'T');
        }

        // If there is no timezone info, assume UTC to avoid Safari parsing issues
        if (!/[zZ]|[+\-]\d{2}:?\d{2}$/.test(normalized)) {
            normalized += 'Z';
        }

        let parsed = new Date(normalized);
        if (!Number.isNaN(parsed.getTime())) {
            return parsed;
        }

        // Fallback: manually construct date using components (treat as local time)
        const [datePart, timePart = '00:00:00'] = normalized.replace(/[zZ]$/, '').split('T');
        const [year, month = 1, day = 1] = datePart.split('-').map(Number);
        const [hour = 0, minute = 0, second = 0] = timePart.split(':').map(Number);
        return new Date(year, month - 1, day, hour, minute, second);
    }

    return new Date(value);
}

function getDateKey(date) {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
}

function parseDateKey(key) {
    if (key instanceof Date) {
        return new Date(key.getTime());
    }

    if (typeof key === 'string') {
        const parts = key.split('-').map(Number);
        if (parts.length === 3 && parts.every(part => !Number.isNaN(part))) {
            const [year, month, day] = parts;
            return new Date(year, month - 1, day);
        }
    }

    const fallback = new Date(key);
    return Number.isNaN(fallback.getTime()) ? new Date() : fallback;
}

async function loadTransactions() {
    try {
        console.log('📊 Loading transactions from API...');

        // Debug: Check authentication state
        const sessionToken = localStorage.getItem('sessionToken');
        const userInfo = localStorage.getItem('userInfo');
        console.log('🔐 Session Token:', sessionToken ? `${sessionToken.substring(0, 15)}...` : 'MISSING');
        console.log('👤 User Info:', userInfo);

        if (!sessionToken) {
            console.error('❌ No session token found! User may not be logged in.');
            throw new Error('Not authenticated');
        }

        const authHeaders = {
            'Authorization': `Bearer ${sessionToken}`
        };

        console.log('📡 Fetching income and expenses from API...');

        // Fetch both income and expenses
        const [incomeResponse, expenseResponse] = await Promise.all([
            fetch('/api/income', { headers: authHeaders }),
            fetch('/api/expenses', { headers: authHeaders })
        ]);

        console.log('📥 Income Response Status:', incomeResponse.status, incomeResponse.ok ? '✅' : '❌');
        console.log('📥 Expense Response Status:', expenseResponse.status, expenseResponse.ok ? '✅' : '❌');

        if (!incomeResponse.ok || !expenseResponse.ok) {
            console.error('❌ API request failed!');
            console.error('  Income:', incomeResponse.status, incomeResponse.statusText);
            console.error('  Expenses:', expenseResponse.status, expenseResponse.statusText);
            throw new Error('Failed to load transactions');
        }

        const incomeData = await incomeResponse.json();
        const expenseData = await expenseResponse.json();

        console.log(`📊 Raw API data received:`);
        console.log(`  - Income transactions: ${incomeData.length}`);
        console.log(`  - Expense transactions: ${expenseData.length}`);

        // Convert API data to transaction format
        transactions = [
            ...incomeData.map((income, index) => {
                const occurredAt = parseTransactionDate(income.occurredAt || income.createdAt);
                return {
                    id: `income-${index}-${Date.now()}`, // Generate unique ID
                    title: income.description,
                    description: income.description,
                    amount: parseFloat(income.amount),
                    type: 'income',
                    category: income.categoryName || 'Income',
                    icon: '💰',
                    date: occurredAt,
                    timestamp: occurredAt.toLocaleTimeString('en-US', {
                        hour: 'numeric',
                        minute: '2-digit',
                        hour12: true
                    }),
                    updatedBalance: income.updatedBalance
                };
            }),
            ...expenseData.map((expense, index) => {
                const occurredAt = parseTransactionDate(expense.occurredAt || expense.createdAt);
                return {
                    id: `expense-${index}-${Date.now()}`, // Generate unique ID
                    title: expense.description,
                    description: expense.description,
                    amount: parseFloat(expense.amount),
                    type: 'expense',
                    category: expense.categoryName || 'Uncategorized',
                    icon: getCategoryIcon(expense.categoryName || 'Uncategorized'),
                    date: occurredAt,
                    timestamp: occurredAt.toLocaleTimeString('en-US', {
                        hour: 'numeric',
                        minute: '2-digit',
                        hour12: true
                    }),
                    updatedBalance: expense.updatedBalance
                };
            })
        ];

        // Sort by date (newest first)
        transactions.sort((a, b) => b.date - a.date);

        console.log(`✅ Loaded ${transactions.length} transactions (${incomeData.length} income, ${expenseData.length} expenses)`);

        filteredTransactions = [...transactions];
        renderTransactions();
        updateSummary();
        updateTransactionCount();
    } catch (error) {
        console.error('❌ Error loading transactions:', error);
        // Show error message to user

        const container = document.querySelector('#transactions-list');

        if (container) {
            container.innerHTML = `
                <div style="text-align: center; padding: 2rem; color: #ef4444;">
                    ❌ Error loading transactions.<br>
                    <button onclick="loadTransactions()" style="margin-top: 1rem; padding: 0.5rem 1rem; background: var(--primary); color: white; border: none; border-radius: 8px; cursor: pointer;">Retry</button>
                </div>
            `;
        }
    }
}

function renderTransactions() {

    console.log('🎨 renderTransactions() called');
    console.log('📊 filteredTransactions count:', filteredTransactions.length);

    const container = document.querySelector('#transactions-list');
    console.log('📦 Container found:', container ? 'YES ✅' : 'NO ❌');

    if (!container) {
        console.error('❌ Container #transactions-list not found!');
        return;
    }

    // Clear container first
    container.innerHTML = '';

    // Show message if no transactions
    if (filteredTransactions.length === 0) {
        console.log('⚠️ No transactions to display!');
        container.innerHTML = `
            <div style="text-align: center; padding: 3rem; color: #94a3b8;">
                <div style="font-size: 3rem; margin-bottom: 1rem;">📊</div>
                <div style="font-size: 1.125rem; font-weight: 600; margin-bottom: 0.5rem;">No transactions yet</div>
                <div style="font-size: 0.875rem;">Start by adding your first income or expense transaction</div>
            </div>
        `;
        return;
    }

    // Group transactions by date
    const groupedTransactions = groupTransactionsByDate(filteredTransactions);
    console.log('📅 Grouped by date:', Object.keys(groupedTransactions));

    Object.keys(groupedTransactions).forEach(dateKey => {
        const dateGroup = document.createElement('div');
        dateGroup.className = 'transaction-date-group';

        const dateHeader = document.createElement('div');
        dateHeader.className = 'date-header';
        dateHeader.textContent = formatDateHeader(dateKey);
        dateGroup.appendChild(dateHeader);

        groupedTransactions[dateKey].forEach(transaction => {
            const transactionElement = createTransactionElement(transaction);
            dateGroup.appendChild(transactionElement);
        });

        container.appendChild(dateGroup);
    });


    console.log('✅ Rendering complete!', Object.keys(groupedTransactions).length, 'date groups rendered');

}

function groupTransactionsByDate(transactions) {
    const grouped = {};

    transactions.forEach(transaction => {
        const transactionDate = parseTransactionDate(transaction.date);
        const dateKey = getDateKey(transactionDate);
        if (!grouped[dateKey]) {
            grouped[dateKey] = [];
        }
        grouped[dateKey].push({ ...transaction, date: transactionDate });
    });

    // Sort by date (newest first)
    const sortedKeys = Object.keys(grouped).sort((a, b) => {
        const dateA = parseDateKey(a);
        const dateB = parseDateKey(b);
        return dateB - dateA;
    });
    const sortedGrouped = {};
    sortedKeys.forEach(key => {
        sortedGrouped[key] = grouped[key].sort((a, b) => b.date - a.date);
    });

    return sortedGrouped;
}

function formatDateHeader(dateString) {
    const date = parseDateKey(dateString);
    const today = new Date();
    const yesterday = new Date(today.getTime() - 24 * 60 * 60 * 1000);

    if (date.toDateString() === today.toDateString()) {
        return 'Today, ' + date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
    } else if (date.toDateString() === yesterday.toDateString()) {
        return 'Yesterday, ' + date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
    } else {
        return date.toLocaleDateString('en-US', {
            month: 'short',
            day: 'numeric',
            year: 'numeric'
        });
    }
}

function createTransactionElement(transaction) {
    const element = document.createElement('div');
    element.className = `transaction-item ${transaction.type}`;
    element.setAttribute('data-type', transaction.type);

    // Determine icon background color based on transaction type
    const isIncome = transaction.type === 'income';
    const iconBg = isIncome ? '#d1fae5' : '#fee2e2';
    const categoryName = getCategoryName(transaction.category);

    // Format date like dashboard: "Nov 1, 2024" or "Today" or "Yesterday"
    const formattedDate = parseTransactionDate(transaction.date).toLocaleDateString('en-US', {
        month: 'short',
        day: 'numeric',
        year: 'numeric'
    });

    // Match dashboard structure: colored icon background, simplified details
    element.innerHTML = `
        <div class="transaction-icon" style="background: ${iconBg};">${transaction.icon}</div>
        <div class="transaction-details">

            <div class="transaction-title">${transaction.description}</div>
            <div class="transaction-category">${categoryName} • ${formattedDate}</div>

        </div>
        <div class="transaction-amount ${transaction.type}">
            ${transaction.type === 'income' ? '+' : '-'}$${transaction.amount.toFixed(2)}
        </div>
    `;

    return element;
}

function formatTransactionDate(date) {
    const today = new Date();
    const yesterday = new Date(today);
    yesterday.setDate(yesterday.getDate() - 1);

    const transDate = parseTransactionDate(date);

    if (transDate.toDateString() === today.toDateString()) {
        return 'Today, ' + transDate.toLocaleTimeString('en-US', { hour: 'numeric', minute: '2-digit' });
    } else if (transDate.toDateString() === yesterday.toDateString()) {
        return 'Yesterday';
    } else {
        return transDate.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
    }
}

function getCategoryName(category) {
    // If category is already a proper name (from backend), return it as-is
    if (!category) return 'Uncategorized';

    // If it's already capitalized or has spaces, it's likely from the backend
    if (category.includes(' ') || category[0] === category[0].toUpperCase()) {
        return category;
    }

    // Otherwise, map lowercase slugs to proper names
    const categoryMap = {
        'food': 'Food & Dining',
        'transport': 'Transportation',
        'transportation': 'Transportation',
        'shopping': 'Shopping',
        'entertainment': 'Entertainment',
        'bills': 'Bills & Utilities',
        'utilities': 'Utilities',
        'income': 'Income',
        'education': 'Education',
        'health': 'Health',
        'fitness': 'Fitness',
        'rent': 'Rent',
        'housing': 'Housing',
        'groceries': 'Groceries',
        'other': 'Other',
        'uncategorized': 'Uncategorized'
    };

    return categoryMap[category.toLowerCase()] || category;
}

function updateSummary() {
    const income = transactions
        .filter(t => t.type === 'income')
        .reduce((sum, t) => sum + t.amount, 0);

    const expenses = transactions
        .filter(t => t.type === 'expense')
        .reduce((sum, t) => sum + t.amount, 0);

    const balance = income - expenses;

    const incomeCard = document.querySelector('.summary-card.income .summary-value');
    if (incomeCard) incomeCard.textContent = `$${income.toFixed(2)}`;
    const expenseCard = document.querySelector('.summary-card.expenses .summary-value');
    if (expenseCard) expenseCard.textContent = `$${expenses.toFixed(2)}`;
    const balanceCard = document.querySelector('.summary-card.net .summary-value') || document.getElementById('net-income-summary');
    if (balanceCard) balanceCard.textContent = `$${balance.toFixed(2)}`;
    const transactionCard = document.querySelector('.summary-card.transactions .summary-value');
    if (transactionCard) transactionCard.textContent = transactions.length;
}

function updateTransactionCount() {
    const countElement = document.querySelector('.transaction-count');
    if (countElement) {
        countElement.textContent = `${filteredTransactions.length} transactions`;
    }
}

// Modal functions
window.openAddTransactionModal = function(type = 'expense') {
    const modal = document.getElementById('addTransactionModal');
    if (!modal) {
        console.warn('transactions.js: addTransactionModal not found.');
        return;
    }
    modal.style.display = 'flex';
    modal.classList.add('show');  // Add show class for test detection
    modal.setAttribute('role', 'dialog');  // Add ARIA role for accessibility and test detection

    // Set default date to now
    const now = new Date();
    const dateInput = modal.querySelector('input[type="date"]');
    if (dateInput) {
        dateInput.value = now.toISOString().slice(0, 10);
    }
    const timeInput = modal.querySelector('input[type="time"]');
    if (timeInput) {
        timeInput.value = now.toTimeString().slice(0, 5);
    }

    // Set the correct tab based on type
    const tabs = modal.querySelectorAll('.quick-tab');
    tabs.forEach(tab => tab.classList.remove('active'));

    if (type === 'income') {
        const incomeTab = Array.from(tabs).find(tab => tab.textContent.includes('Income'));
        if (incomeTab) incomeTab.classList.add('active');
        currentTransactionType = 'income';
        window.currentTransactionType = 'income';
    } else {
        const expenseTab = Array.from(tabs).find(tab => tab.textContent.includes('Expense'));
        if (expenseTab) expenseTab.classList.add('active');
        currentTransactionType = 'expense';
        window.currentTransactionType = 'expense';
    }
};

window.closeAddTransactionModal = function() {
    const modal = document.getElementById('addTransactionModal');
    if (!modal) return;
    modal.style.display = 'none';
    modal.classList.remove('show');  // Remove show class

    const form = document.getElementById('full-transaction-form');
    if (form) {
        form.reset();
    }

    const modalCategoryInput = document.getElementById('modal-selected-category-id');
    if (modalCategoryInput) {
        modalCategoryInput.value = '';
    }
    const modalCategorySearch = document.getElementById('modal-category-search');
    if (modalCategorySearch) {
        modalCategorySearch.value = '';
        modalCategorySearch.classList.remove('has-value');
    }

    // Reset to expense tab
    const tabs = modal.querySelectorAll('.quick-tab');
    tabs.forEach(tab => tab.classList.remove('active'));
    const expenseTab = Array.from(tabs).find(tab => tab.textContent.includes('Expense'));
    if (expenseTab) expenseTab.classList.add('active');
};

async function handleAddTransaction(event) {
    event.preventDefault();

    const form = event.target;
    const modal = document.getElementById('addTransactionModal');

    try {
        // Get form values
        const amount = parseFloat(form.querySelector('input[type="number"]').value);
        const description = form.querySelector('input[type="text"]').value;
        const date = form.querySelector('input[type="date"]').value;
        const time = form.querySelector('input[type="time"]').value || '00:00';
        const modalCategoryInput = document.getElementById('modal-selected-category-id');
        let categoryId = null;
        if (modalCategoryInput && modalCategoryInput.value) {
            const parsed = parseInt(modalCategoryInput.value, 10);
            if (!Number.isNaN(parsed)) {
                categoryId = parsed;
            }
        }

        // Combine date and time
        const safeTime = time && time.length > 0 ? time : '00:00';
        const occurredAt = `${date}T${safeTime}:00`;

        // Determine transaction type from active tab
        const activeTab = modal.querySelector('.quick-tab.active');
        const isIncome = activeTab && activeTab.textContent.includes('Income');

        // Get account ID
        let accountId;
        try {
            accountId = await incomeService.getDefaultAccountId(true);
        } catch (err) {
            throw new Error(err.message || 'Please create an account before adding transactions.');
        }

        if (isIncome) {
            // Add income
            const incomeData = {
                amount,
                description,
                occurredAt,
                accountId,
                categoryId
            };

            await incomeService.addIncome(incomeData);
            showNotification(`💰 Income of $${amount.toFixed(2)} added successfully!`);
        } else {
            // Add expense - need to get category ID
            const expenseData = {
                amount,
                description,
                occurredAt,
                categoryId,
                accountId
            };

            await expenseService.addExpense(expenseData);
            showNotification(`💸 Expense of $${amount.toFixed(2)} added successfully!`);
        }

        // Reload transactions and close modal
        await loadTransactions();
        closeAddTransactionModal();

    } catch (error) {
        console.error('Error adding transaction:', error);
        showNotification(`❌ Failed to add transaction: ${error.message}`);
    }
}

function getCategoryIcon(categoryName) {
    // Normalize category name to lowercase for matching
    const normalized = (categoryName || '').toLowerCase().trim();

    const iconMap = {
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

    return iconMap[normalized] || '💸';
}

function exportTransactions() {
    const csv = generateCSV(filteredTransactions);
    downloadCSV(csv, 'transactions.csv');
    showNotification('Transactions exported successfully!');
}

function generateCSV(transactions) {
    const headers = ['Date', 'Title', 'Description', 'Category', 'Type', 'Amount'];
    const rows = transactions.map(t => [
        parseTransactionDate(t.date).toLocaleDateString(),
        t.title,
        t.description,
        getCategoryName(t.category),
        t.type,
        t.amount.toFixed(2)
    ]);

    return [headers, ...rows]
        .map(row => row.map(field => `"${field}"`).join(','))
        .join('\n');
}

function downloadCSV(csv, filename) {
    const blob = new Blob([csv], { type: 'text/csv' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    window.URL.revokeObjectURL(url);
}

function loadMoreTransactions() {
    showNotification('Loading more transactions...');
    // In a real app, this would fetch more data from the server
}

async function downloadReportChart() {
    try {
        // Retrieve token and user info just like IncomeService
        const sessionToken = localStorage.getItem('sessionToken');
        const userInfo = JSON.parse(localStorage.getItem('userInfo'));
        const userId = userInfo?.userId;

        if (!sessionToken || !userId) {
            alert('⚠️ You must be logged in to download the report.');
            return;
        }

        // Define your date range (last 30 days)
        const endDate = new Date();
        const startDate = new Date();
        startDate.setMonth(endDate.getMonth() - 1);

        // Build query params (ISO local date/time)
        const params = new URLSearchParams({
            userId: userId,
            startDate: startDate.toISOString().slice(0, 19),
            endDate: endDate.toISOString().slice(0, 19)
        });

        // Send authenticated GET request
        const response = await fetch(`/api/reports/chart?${params.toString()}`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${sessionToken}`,
                'X-USER-ID': userId  // keep consistent with your other API calls
            }
        });

        if (!response.ok) {
            throw new Error(`Failed to fetch chart: ${response.status}`);
        }

        // Download the PNG
        const blob = await response.blob();
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `transactions-chart-${new Date().toISOString().slice(0,10)}.png`;
        document.body.appendChild(a);
        a.click();
        a.remove();
        window.URL.revokeObjectURL(url);

        if (typeof showNotification === 'function') {
            showNotification('✅ Chart downloaded successfully!');
        } else {
            console.log('✅ Chart downloaded successfully!');
        }

    } catch (err) {
        console.error('❌ Error downloading chart:', err);
        alert('Failed to download chart: ' + err.message);
    }
}



function showNotification(message) {
    // Create a simple notification
    const notification = document.createElement('div');
    notification.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        background: var(--primary);
        color: white;
        padding: 1rem 1.5rem;
        border-radius: 12px;
        z-index: 10000;
        animation: slideIn 0.3s ease;
    `;
    notification.textContent = message;

    document.body.appendChild(notification);

    setTimeout(() => {
        notification.remove();
    }, 3000);
}

// Close modals when clicking outside
window.addEventListener('click', function(event) {
    const modal = document.getElementById('addTransactionModal');
    if (event.target === modal) {
        closeAddTransactionModal();
    }
});

// Add CSS for notification animation
const notificationStyle = document.createElement('style');
notificationStyle.textContent = `
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
`;
document.head.appendChild(notificationStyle);

// Make loadTransactions globally available for category-service to call
window.loadTransactions = loadTransactions;
