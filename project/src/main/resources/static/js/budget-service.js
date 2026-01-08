// Budget Service for FR-11 Budget Setting
class BudgetService {
    constructor() {
        this.baseUrl = '/api/budget';
    }

    /**
     * Get authentication headers with Bearer token
     */
    getAuthHeaders() {
        const sessionToken = localStorage.getItem('sessionToken');
        if (!sessionToken) {
            console.warn('⚠️ No session token found - user may not be authenticated');
        }
        return {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${sessionToken}`
        };
    }

    /**
     * FR-11: Load categories for budget setting
     */
    async loadCategories() {
        try {
            console.log('🔄 Loading categories...');
            const response = await fetch(`${this.baseUrl}/categories`, {
                headers: this.getAuthHeaders()
            });

            if (!response.ok) {
                throw new Error(`Failed to load categories: ${response.status}`);
            }

            const categories = await response.json();
            console.log('✅ Categories loaded:', categories);

            this.populateCategoryDropdown(categories);
            return categories;
        } catch (error) {
            console.error('❌ Error loading categories:', error);
            this.showErrorMessage('Failed to load categories. Please refresh the page.');
            return [];
        }
    }

    /**
     * Populate category dropdown with API data
     */
    populateCategoryDropdown(categories) {
        const categorySelect = document.getElementById('modal-category');
        if (!categorySelect) return;

        // Clear existing options except the first one
        categorySelect.innerHTML = '<option value="">Select a category...</option>';

        categories.forEach(category => {
            const option = document.createElement('option');
            option.value = category.categoryId;
            option.textContent = category.name;
            categorySelect.appendChild(option);
        });

        console.log('📋 Category dropdown populated with', categories.length, 'categories');
    }

    /**
     * FR-11: Set budget for selected category
     */
    async setBudget(budgetData) {
        console.log('💰 Setting budget:', budgetData);

        try {
            // FR-11: System validates inputs
            const validation = this.validateBudgetData(budgetData);
            if (!validation.isValid) {
                this.showErrorMessage(validation.errors.join(', '));
                return { success: false, errors: validation.errors };
            }

            const response = await fetch(`${this.baseUrl}/set`, {
                method: 'POST',
                headers: this.getAuthHeaders(),
                body: JSON.stringify({
                    categoryId: parseInt(budgetData.categoryId),
                    amount: parseFloat(budgetData.amount),
                    customName: budgetData.customName || null
                })
            });

            if (!response.ok) {
                const errorData = await response.json().catch(() => ({ message: 'Unknown error' }));
                throw new Error(errorData.message || `HTTP error! status: ${response.status}`);
            }

            const result = await response.json();
            console.log('✅ Budget set successfully:', result);

            // FR-11: System saves the budget and recalculates utilization
            this.showSuccessMessage(
                `Budget of $${budgetData.amount} set for category successfully!`
            );

            // FR-11: Update dashboard utilization if on dashboard page
            if (typeof loadBudgetUtilization === 'function') {
                setTimeout(() => loadBudgetUtilization(), 1000); // Refresh after 1 second
            }

            return { success: true, data: result };

        } catch (error) {
            console.error('❌ Error setting budget:', error);

            // FR-11: DB failure - show error and allow retry
            this.showErrorMessage(
                error.message || 'Failed to save budget. Please try again.'
            );

            return { success: false, error: error.message };
        }
    }

    /**
     * FR-11: Validate budget inputs
     * Constraints: Budget must be a positive number
     * Validity checks: Amount > 0; numeric; non-empty
     */
    validateBudgetData(budgetData) {
        const errors = [];

        // Category selection required
        if (!budgetData.categoryId || budgetData.categoryId === '') {
            errors.push('Please select a category');
        }

        // Amount validation - FR-11 requirements
        if (!budgetData.amount || budgetData.amount === '') {
            errors.push('Amount is required');
        } else {
            const amount = parseFloat(budgetData.amount);

            if (isNaN(amount)) {
                errors.push('Amount must be a valid number');
            } else if (amount <= 0) {
                errors.push('Amount must be greater than 0');
            } else if (amount > 999999.99) {
                errors.push('Amount cannot exceed $999,999.99');
            }
        }

        return {
            isValid: errors.length === 0,
            errors: errors
        };
    }

    /**
     * Get budget summary for a category
     */
    async getBudgetSummary(categoryId) {
        try {
            const response = await fetch(`${this.baseUrl}/summary/${categoryId}`, {
                headers: this.getAuthHeaders()
            });

            if (!response.ok) {
                throw new Error(`Failed to get budget summary: ${response.status}`);
            }

            const summary = await response.json();
            console.log('📊 Budget summary:', summary);
            return summary;
        } catch (error) {
            console.error('❌ Error getting budget summary:', error);
            return null;
        }
    }

    /**
     * Delete budget for a category
     */
    async deleteBudget(categoryId) {
        console.log('🗑️ Deleting budget for category:', categoryId);

        try {
            const response = await fetch(`${this.baseUrl}/delete/${categoryId}`, {
                method: 'DELETE',
                headers: this.getAuthHeaders()
            });

            if (!response.ok) {
                const errorData = await response.json().catch(() => ({ error: 'Unknown error' }));
                throw new Error(errorData.error || `HTTP error! status: ${response.status}`);
            }

            const result = await response.json();
            console.log('✅ Budget deleted successfully:', result);

            this.showSuccessMessage('Budget deleted successfully!');

            return { success: true, data: result };

        } catch (error) {
            console.error('❌ Error deleting budget:', error);

            this.showErrorMessage(
                error.message || 'Failed to delete budget. Please try again.'
            );

            return { success: false, error: error.message };
        }
    }

    /**
     * Show success message to user
     */
    showSuccessMessage(message) {
        this.showMessage(message, 'success');
    }

    /**
     * Show error message to user
     * FR-11: Invalid amount - show validation error; do not save
     */
    showErrorMessage(message) {
        this.showMessage(message, 'error');
    }

    /**
     * Generic message display function (consistent with income notifications)
     * @param {string} message - Message text
     * @param {string} type - Message type (success, error, info)
     */
    showMessage(message, type = 'info') {
        // Remove existing messages
        const existingMessages = document.querySelectorAll('.budget-message');
        existingMessages.forEach(msg => msg.remove());

        const messageDiv = document.createElement('div');
        messageDiv.className = `budget-message ${type}`;
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
     * Format amount for display
     */
    formatAmount(amount) {
        return new Intl.NumberFormat('en-US', {
            style: 'currency',
            currency: 'USD'
        }).format(amount);
    }
}

// Create global instance
const budgetService = new BudgetService();

// Add CSS animations for notifications (consistent with income notifications)
const budgetServiceStyles = document.createElement('style');
budgetServiceStyles.textContent = `
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
document.head.appendChild(budgetServiceStyles);