/**
 * FR-14: Dashboard API Service
 * Handles all API calls for dashboard data
 */
const DashboardAPI = {
    /**
     * Get auth headers with Bearer token
     */
    getAuthHeaders() {
        const sessionToken = localStorage.getItem('sessionToken');
        console.log('🔐 Dashboard API - Session Token:', sessionToken ? `${sessionToken.substring(0, 10)}...` : 'NULL');
        console.log('🔐 Dashboard API - Authorization Header:', sessionToken ? `Bearer ${sessionToken.substring(0, 10)}...` : 'MISSING');
        return {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${sessionToken}`
        };
    },

    /**
     * Get complete dashboard data
     */
    async getDashboardData() {
        try {
            const response = await fetch('/api/dashboard', {
                method: 'GET',
                credentials: 'include',
                headers: this.getAuthHeaders()
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            const data = await response.json();

            // FR-14: Check if data is available
            if (!data.dataAvailable) {
                console.warn('Dashboard data unavailable:', data.message);
                return {
                    dataAvailable: false,
                    message: data.message || 'Data currently unavailable'
                };
            }

            return data;
        } catch (error) {
            console.error('Error fetching dashboard data:', error);
            return {
                dataAvailable: false,
                message: 'Failed to load dashboard data. Please try again.'
            };
        }
    },

    /**
     * Get financial aggregates only
     */
    async getFinancialAggregates() {
        try {
            const response = await fetch('/api/dashboard/financial-aggregates', {
                method: 'GET',
                credentials: 'include',
                headers: this.getAuthHeaders()
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            return await response.json();
        } catch (error) {
            console.error('Error fetching financial aggregates:', error);
            return null;
        }
    },

    /**
     * Get spending trend data for charts
     * @param {string} period - "WEEK", "MONTH", or "YEAR"
     */
    async getSpendingTrend(period = 'WEEK') {
        try {
            const response = await fetch(`/api/dashboard/spending-trend?period=${period}`, {
                method: 'GET',
                credentials: 'include',
                headers: this.getAuthHeaders()
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            return await response.json();
        } catch (error) {
            console.error('Error fetching spending trend:', error);
            return null;
        }
    },

    /**
     * Format currency value
     */
    formatCurrency(amount) {
        if (amount === null || amount === undefined || isNaN(amount)) return '';
        return '$' + parseFloat(amount).toLocaleString('en-US', {
            minimumFractionDigits: 2,
            maximumFractionDigits: 2
        });
    },

    /**
     * Format percentage
     */
    formatPercentage(value) {
        if (value == null) return '0%';
        const sign = value > 0 ? '+' : '';
        return sign + parseFloat(value).toFixed(1) + '%';
    },

    /**
     * Format date
     */
    formatDate(dateString) {
        if (!dateString) return '';
        const date = new Date(dateString);
        const now = new Date();
        const yesterday = new Date(now);
        yesterday.setDate(yesterday.getDate() - 1);

        // Check if today
        if (date.toDateString() === now.toDateString()) {
            return 'Today';
        }

        // Check if yesterday
        if (date.toDateString() === yesterday.toDateString()) {
            return 'Yesterday';
        }

        // Otherwise format as "Mon Jan 1, 2024"
        const options = { weekday: 'short', year: 'numeric', month: 'short', day: 'numeric' };
        return date.toLocaleDateString('en-US', options);
    },

    /**
     * Get icon for transaction type
     */
    getTransactionIcon(type, categoryName) {
        if (type === 'INCOME') {
            return '💰';
        }
        // Default expense icon or use categoryName-based icon
        return this.getCategoryIcon(categoryName);
    },

    /**
     * Get category icon (fallback if backend doesn't provide one)
     */
    getCategoryIcon(categoryName) {
        if (!categoryName) return '💰';

        const iconMap = {
            'food': '🍔',
            'dining': '🍔',
            'transport': '🚗',
            'transportation': '🚗',
            'entertainment': '🎮',
            'shopping': '🛍️',
            'groceries': '🛒',
            'education': '📚',
            'health': '🏥',
            'healthcare': '🏥',
            'utilities': '💡',
            'housing': '🏠',
            'rent': '🏠'
        };

        return iconMap[categoryName.toLowerCase()] || '💰';
    },

    /**
     * Get background color based on transaction type
     */
    getTransactionColor(type) {
        return type === 'INCOME' ? '#d1fae5' : '#fee2e2';
    }
};

// Export for use in other scripts
if (typeof module !== 'undefined' && module.exports) {
    module.exports = DashboardAPI;
}

console.log('✅ Dashboard API service loaded');
