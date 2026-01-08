/**
 * Goal Service
 * Handles all goal CRUD operations and goal-related business logic
 */

const goalService = {
    // Base API endpoint
    apiUrl: '/api/goals',

    /**
     * Get user ID from localStorage userInfo
     */
    getUserId() {
        const userInfo = JSON.parse(localStorage.getItem('userInfo'));
        return userInfo?.userId || null;
    },

    /**
     * Build authorization headers expected by the backend
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
    },

    /**
     * Get all goals for the current user
     */
    async getAllGoals() {
        try {
            console.log('🎯 Fetching all goals...');
            const response = await fetch(this.apiUrl, {
                method: 'GET',
                headers: this.getAuthHeaders(),
                credentials: 'include' // ensures cookies are sent
            });

            if (!response.ok) throw new Error(`Failed to fetch goals: ${response.status}`);

            const goals = await response.json();
            console.log('✅ Goals loaded:', goals);
            return goals;
        } catch (error) {
            console.error('❌ Error fetching goals:', error);
            throw error;
        }
    },

    /**
     * Create a new goal
     * @param {Object} goalData - {name: string, targetAmount: number, dueDate: string (YYYY-MM-DD)}
     */
    async createGoal(goalData) {
        try {
            console.log('➕ Creating goal:', goalData);

            const response = await fetch(this.apiUrl, {
                method: 'POST',
                headers: this.getAuthHeaders(),
                body: JSON.stringify(goalData)
            });

            if (!response.ok) {
                const errorData = await response.json().catch(() => ({}));
                throw new Error(errorData.message || `Failed to create goal: ${response.status}`);
            }

            const newGoal = await response.json();
            console.log('✅ Goal created:', newGoal);
            this.clearAccountCache();
            return newGoal;
        } catch (error) {
            console.error('❌ Error creating goal:', error);
            throw error;
        }
    },

    /**
     * Get a single goal by ID
     * @param {number} goalId
     */
    async getGoal(goalId) {
        try {
            console.log(`📋 Fetching goal ${goalId}...`);
            const response = await fetch(`${this.apiUrl}/${goalId}`, {
                method: 'GET',
                headers: this.getAuthHeaders()
            });

            if (!response.ok) {
                throw new Error(`Failed to fetch goal: ${response.status}`);
            }

            const goal = await response.json();
            console.log('✅ Goal loaded:', goal);
            return goal;
        } catch (error) {
            console.error('❌ Error fetching goal:', error);
            throw error;
        }
    },

    /**
     * Update a goal
     * @param {number} goalId
     * @param {Object} goalData - {name: string, targetAmount: number, dueDate: string}
     */
    async updateGoal(goalId, goalData) {
        try {
            console.log(`✏️ Updating goal ${goalId}:`, goalData);

            const response = await fetch(`${this.apiUrl}/${goalId}`, {
                method: 'PATCH',
                headers: this.getAuthHeaders(),
                body: JSON.stringify(goalData)
            });

            if (!response.ok) {
                const errorData = await response.json().catch(() => ({}));
                throw new Error(errorData.message || `Failed to update goal: ${response.status}`);
            }

            const updatedGoal = await response.json();
            console.log('✅ Goal updated:', updatedGoal);
            return updatedGoal;
        } catch (error) {
            console.error('❌ Error updating goal:', error);
            throw error;
        }
    },

    /**
     * Delete a goal
     * @param {number} goalId
     */
    async deleteGoal(goalId) {
        try {
            console.log(`🗑️ Deleting goal ${goalId}`);

            const response = await fetch(`${this.apiUrl}/${goalId}`, {
                method: 'DELETE',
                headers: this.getAuthHeaders()
            });

            if (!response.ok) {
                const errorData = await response.json().catch(() => ({}));
                throw new Error(errorData.message || `Failed to delete goal: ${response.status}`);
            }

            console.log('✅ Goal deleted successfully');
            this.clearAccountCache();
            return true;
        } catch (error) {
            console.error('❌ Error deleting goal:', error);
            throw error;
        }
    },

    /**
     * Contribute to a goal
     * @param {Object} contributionData - {fromAccountId: number, goalId: number, amount: number}
     */
    async contributeToGoal(contributionData) {
        try {
            console.log('💰 Adding contribution:', contributionData);

            const response = await fetch(`${this.apiUrl}/contribute`, {
                method: 'POST',
                headers: this.getAuthHeaders(),
                body: JSON.stringify(contributionData)
            });

            if (!response.ok) {
                const errorData = await response.json().catch(() => ({}));
                throw new Error(errorData.message || `Failed to add contribution: ${response.status}`);
            }

            const result = await response.json();
            console.log('✅ Contribution added:', result);
            this.clearAccountCache();
            return result;
        } catch (error) {
            console.error('❌ Error adding contribution:', error);
            throw error;
        }
    },

    /**
     * Get contributions for a goal
     * @param {number} goalId
     * @param {string} from - Optional start date (YYYY-MM-DD)
     * @param {string} to - Optional end date (YYYY-MM-DD)
     */
    async getContributions(goalId, from = null, to = null) {
        try {
            console.log(`📊 Fetching contributions for goal ${goalId}`);

            const params = new URLSearchParams();
            if (from) params.append('from', from);
            if (to) params.append('to', to);

            const url = `${this.apiUrl}/${goalId}/contributions${params.toString() ? '?' + params.toString() : ''}`;

            const response = await fetch(url, {
                method: 'GET',
                headers: this.getAuthHeaders()
            });

            if (!response.ok) {
                throw new Error(`Failed to fetch contributions: ${response.status}`);
            }

            const contributions = await response.json();
            console.log('✅ Contributions loaded:', contributions);
            return contributions;
        } catch (error) {
            console.error('❌ Error fetching contributions:', error);
            throw error;
        }
    },

    /**
     * Get default account ID (needed for contributions)
     */
    async getDefaultAccountId() {
        const account = await this.getPrimaryContributionAccount();
        if (!account) {
            throw new Error('No accounts found');
        }
        return account.id;
    },

    /**
     * Get account balance from the default account
     * @returns {Promise<number>} Current account balance
     */
    async getAccountBalance() {
        const account = await this.getPrimaryContributionAccount();
        if (!account) {
            throw new Error('No accounts found');
        }
        const balance = Number(account.balance || 0);
        console.log('✅ Account balance fetched:', balance);
        return balance;
    },

    /**
     * Validate goal data
     */
    validateGoalData(goalData) {
        const errors = [];

        if (!goalData.name || goalData.name.trim().length === 0) {
            errors.push('Goal name is required');
        }

        if (!goalData.targetAmount || goalData.targetAmount <= 0) {
            errors.push('Target amount must be greater than 0');
        }

        if (!goalData.dueDate) {
            errors.push('Due date is required');
        } else {
            const dueDate = new Date(goalData.dueDate);
            const today = new Date();
            today.setHours(0, 0, 0, 0);
            if (dueDate < today) {
                errors.push('Due date must be in the future');
            }
        }

        return {
            isValid: errors.length === 0,
            errors: errors
        };
    },

    async fetchAccounts(forceRefresh = false) {
        if (!forceRefresh && Array.isArray(this._accountCache)) {
            return this._accountCache;
        }

        const response = await fetch('/api/accounts', {
            headers: this.getAuthHeaders()
        });

        if (!response.ok) {
            throw new Error('Failed to fetch accounts');
        }

        const accounts = await response.json();
        this._accountCache = accounts.map(acc => ({
            id: acc.id,
            name: acc.name,
            balance: Number(acc.balance || 0),
            accountType: acc.accountType
        }));

        return this._accountCache;
    },

    clearAccountCache() {
        this._accountCache = null;
    },

    async getPrimaryContributionAccount(forceRefresh = false) {
        const accounts = await this.fetchAccounts(forceRefresh);
        if (!accounts || accounts.length === 0) {
            return null;
        }
        const cashAccount = accounts.find(acc => acc.accountType === 'CASH');
        return cashAccount || accounts[0];
    },

    /**
     * Show success message to user
     */
    showSuccessMessage(message) {
        const toast = document.createElement('div');
        toast.className = 'success-toast';
        toast.textContent = message;
        toast.style.cssText = `
            position: fixed;
            top: 2rem;
            right: 2rem;
            background: #10b981;
            color: white;
            padding: 1rem 1.5rem;
            border-radius: 0.5rem;
            box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
            z-index: 10000;
            animation: slideInRight 0.3s ease-out;
        `;

        document.body.appendChild(toast);

        setTimeout(() => {
            toast.style.animation = 'slideOutRight 0.3s ease-out';
            setTimeout(() => toast.remove(), 300);
        }, 3000);
    },

    /**
     * Show error message to user
     */
    showErrorMessage(message) {
        const toast = document.createElement('div');
        toast.className = 'error-toast';
        toast.textContent = message;
        toast.style.cssText = `
            position: fixed;
            top: 2rem;
            right: 2rem;
            background: #ef4444;
            color: white;
            padding: 1rem 1.5rem;
            border-radius: 0.5rem;
            box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
            z-index: 10000;
            animation: slideInRight 0.3s ease-out;
        `;

        document.body.appendChild(toast);

        setTimeout(() => {
            toast.style.animation = 'slideOutRight 0.3s ease-out';
            setTimeout(() => toast.remove(), 300);
        }, 4000);
    }
};

// Make it globally available
window.goalService = goalService;
