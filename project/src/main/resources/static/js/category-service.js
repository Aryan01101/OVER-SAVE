/**
 * Category Service
 * Handles all category CRUD operations and category-related business logic
 */

const categoryService = {
    // Base API endpoint
    apiUrl: '/api/categories',

    /**
     * Get user ID from localStorage userInfo
     */
    getUserId() {
        const userInfo = JSON.parse(localStorage.getItem('userInfo'));
        return userInfo?.userId || null;
    },

    /**
     * Get authorization headers with session token
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
    },

    /**
     * Get all categories for the current user
     */
    async getAllCategories() {
        try {
            console.log('📦 Fetching all categories...');
            const response = await fetch(this.apiUrl, {
                method: 'GET',
                headers: this.getAuthHeaders()
            });

            if (!response.ok) {
                throw new Error(`Failed to fetch categories: ${response.status}`);
            }

            const categories = await response.json();
            console.log('✅ Categories loaded:', categories);
            return categories;
        } catch (error) {
            console.error('❌ Error fetching categories:', error);
            throw error;
        }
    },

    /**
     * Create a new category
     * @param {Object} categoryData - {name: string}
     */
    async createCategory(categoryData) {
        try {
            console.log('➕ Creating category:', categoryData);

            const response = await fetch(this.apiUrl, {
                method: 'POST',
                headers: this.getAuthHeaders(),
                body: JSON.stringify(categoryData)
            });

            if (!response.ok) {
                const errorData = await response.json().catch(() => ({}));
                throw new Error(errorData.message || `Failed to create category: ${response.status}`);
            }

            const newCategory = await response.json();
            console.log('✅ Category created:', newCategory);
            return newCategory;
        } catch (error) {
            console.error('❌ Error creating category:', error);
            throw error;
        }
    },

    /**
     * Update/rename a category
     * @param {number} categoryId
     * @param {Object} categoryData - {name: string}
     */
    async updateCategory(categoryId, categoryData) {
        try {
            console.log(`✏️ Updating category ${categoryId}:`, categoryData);

            const response = await fetch(`${this.apiUrl}/${categoryId}`, {
                method: 'PUT',
                headers: this.getAuthHeaders(),
                body: JSON.stringify(categoryData)
            });

            if (!response.ok) {
                const errorData = await response.json().catch(() => ({}));
                throw new Error(errorData.message || `Failed to update category: ${response.status}`);
            }

            const updatedCategory = await response.json();
            console.log('✅ Category updated:', updatedCategory);
            return updatedCategory;
        } catch (error) {
            console.error('❌ Error updating category:', error);
            throw error;
        }
    },

    /**
     * Get budgets for a category
     * @param {number} categoryId
     */
    async getCategoryBudgets(categoryId) {
        try {
            console.log(`💰 Fetching budgets for category ${categoryId}`);

            const response = await fetch(`${this.apiUrl}/${categoryId}/budgets`, {
                method: 'GET',
                headers: this.getAuthHeaders()
            });

            if (!response.ok) {
                throw new Error(`Failed to fetch category budgets: ${response.status}`);
            }

            const budgets = await response.json();
            console.log('✅ Category budgets:', budgets);
            return budgets;
        } catch (error) {
            console.error('❌ Error fetching category budgets:', error);
            throw error;
        }
    },

    /**
     * Delete a category and all associated data
     * @param {number} categoryId
     */
    async deleteCategory(categoryId) {
        try {
            console.log(`🗑️ Deleting category ${categoryId} (transactions and budgets will be removed)`);

            const response = await fetch(`${this.apiUrl}/${categoryId}`, {
                method: 'DELETE',
                headers: this.getAuthHeaders()
            });

            if (!response.ok) {
                // Try to parse error response
                let errorMessage = null;
                try {
                    const errorData = await response.json();
                    // Try different error message fields
                    errorMessage = errorData.message || errorData.detail || errorData.error || null;
                } catch (e) {
                    // JSON parsing failed, try text
                    try {
                        const errorText = await response.text();
                        if (errorText) errorMessage = errorText;
                    } catch (e2) {
                        // Ignore
                    }
                }

                // Build comprehensive error message
                const statusMsg = `${response.status} ${response.statusText}`;
                const finalMsg = errorMessage
                    ? `Failed to delete category: ${errorMessage}`
                    : `Failed to delete category (${statusMsg})`;

                console.error(`❌ Delete failed [${statusMsg}]:`, errorMessage || 'No error details');
                throw new Error(finalMsg);
            }

            console.log('✅ Category deleted successfully');
            return true;
        } catch (error) {
            console.error('❌ Error deleting category:', error);
            throw error;
        }
    },

    /**
     * Get category summary (total spent, etc.)
     * @param {number} categoryId
     * @param {string} month - Format: YYYY-MM (optional)
     */
    async getCategorySummary(categoryId, month = null) {
        try {
            console.log(`📊 Fetching summary for category ${categoryId}`);

            const url = month
                ? `${this.apiUrl}/${categoryId}/summary?month=${month}`
                : `${this.apiUrl}/${categoryId}/summary`;

            const response = await fetch(url, {
                method: 'GET',
                headers: this.getAuthHeaders()
            });

            if (!response.ok) {
                throw new Error(`Failed to fetch category summary: ${response.status}`);
            }

            const summary = await response.json();
            console.log('✅ Category summary:', summary);
            return summary;
        } catch (error) {
            console.error('❌ Error fetching category summary:', error);
            throw error;
        }
    },

    /**
     * Get all transactions for a category
     * @param {number} categoryId
     * @param {string} month - Format: YYYY-MM (optional)
     * @param {string} type - "Income" or "Expense" (optional)
     */
    async getCategoryRecords(categoryId, month = null, type = null) {
        try {
            console.log(`📋 Fetching records for category ${categoryId}`);

            const params = new URLSearchParams();
            if (month) params.append('month', month);
            if (type) params.append('type', type);

            const url = `${this.apiUrl}/${categoryId}/records${params.toString() ? '?' + params.toString() : ''}`;

            const response = await fetch(url, {
                method: 'GET',
                headers: this.getAuthHeaders()
            });

            if (!response.ok) {
                throw new Error(`Failed to fetch category records: ${response.status}`);
            }

            const records = await response.json();
            console.log('✅ Category records:', records);
            return records;
        } catch (error) {
            console.error('❌ Error fetching category records:', error);
            throw error;
        }
    },

    /**
     * Merge categories
     * @param {number[]} sourceIds - Array of category IDs to merge from
     * @param {number} targetId - Category ID to merge into
     * @param {boolean} mergeBudgets - Whether to merge budgets to target category (optional)
     */
    async mergeCategories(sourceIds, targetId, mergeBudgets = null) {
        try {
            console.log(`🔀 Merging categories ${sourceIds} into ${targetId}, mergeBudgets: ${mergeBudgets}`);

            const params = new URLSearchParams();
            if (mergeBudgets !== null) params.append('mergeBudgets', mergeBudgets);

            const url = `${this.apiUrl}/merge${params.toString() ? '?' + params.toString() : ''}`;

            const response = await fetch(url, {
                method: 'POST',
                headers: this.getAuthHeaders(),
                body: JSON.stringify({
                    sourceIds: sourceIds,
                    targetId: targetId
                })
            });

            if (!response.ok) {
                // Try to parse error response
                let errorMessage = null;
                try {
                    const errorData = await response.json();
                    // Try different error message fields
                    errorMessage = errorData.message || errorData.detail || errorData.error || null;
                } catch (e) {
                    // JSON parsing failed, try text
                    try {
                        const errorText = await response.text();
                        if (errorText) errorMessage = errorText;
                    } catch (e2) {
                        // Ignore
                    }
                }

                // Build comprehensive error message
                const statusMsg = `${response.status} ${response.statusText}`;
                const finalMsg = errorMessage
                    ? `Failed to merge categories: ${errorMessage}`
                    : `Failed to merge categories (${statusMsg})`;

                console.error(`❌ Merge failed [${statusMsg}]:`, errorMessage || 'No error details');
                throw new Error(finalMsg);
            }

            const result = await response.json();
            console.log('✅ Categories merged:', result);

            // Trigger UI refresh after successful merge
            console.log('🔄 Triggering UI refresh after category merge...');

            // Call global reload functions if they exist
            if (typeof window.reloadCategories === 'function') {
                await window.reloadCategories();
            }

            if (typeof window.loadTransactions === 'function') {
                await window.loadTransactions();
            }

            // Emit custom event for other listeners
            window.dispatchEvent(new CustomEvent('categoriesChanged', {
                detail: {
                    action: 'merge',
                    sourceIds: sourceIds,
                    targetId: targetId,
                    result: result
                }
            }));

            return result;
        } catch (error) {
            console.error('❌ Error merging categories:', error);
            throw error;
        }
    },

    /**
     * Validate category name
     */
    validateCategoryName(name) {
        const errors = [];

        if (!name || name.trim().length === 0) {
            errors.push('Category name is required');
        }

        if (name && name.length > 255) {
            errors.push('Category name must be less than 255 characters');
        }

        return {
            isValid: errors.length === 0,
            errors: errors
        };
    },

    /**
     * Show success message to user
     */
    showSuccessMessage(message) {
        // Create a toast notification
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
        // Create a toast notification
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
window.categoryService = categoryService;
