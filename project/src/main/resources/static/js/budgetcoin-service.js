// Budget Coin API Service

class BudgetCoinService {
    constructor() {
        this.baseUrl = '/api';
    }

    async getAuthHeaders() {
        const sessionToken = localStorage.getItem('sessionToken');
        return {
            'Content-Type': 'application/json',
            'Authorization': sessionToken ? `Bearer ${sessionToken}` : ''
        };
    }

    async fetchUserBalance(userId) {
        try {
            const headers = await this.getAuthHeaders();
            const response = await fetch(`${this.baseUrl}/budgetcoin/balance?userId=${userId}`, {
                method: 'GET',
                headers: headers
            });

            if (!response.ok) {
                throw new Error(`Failed to fetch balance: ${response.statusText}`);
            }

            const data = await response.json();
            return data;
        } catch (error) {
            console.error('Error fetching user balance:', error);
            throw error;
        }
    }

    async fetchAllItems() {
        try {
            const headers = await this.getAuthHeaders();
            const response = await fetch(`${this.baseUrl}/item/all`, {
                method: 'GET',
                headers: headers
            });

            if (!response.ok) {
                throw new Error(`Failed to fetch items: ${response.statusText}`);
            }

            const data = await response.json();
            return data;
        } catch (error) {
            console.error('Error fetching items:', error);
            throw error;
        }
    }

    async purchaseItem(userId, itemId) {
        try {
            const headers = await this.getAuthHeaders();
            const response = await fetch(`${this.baseUrl}/budgetcoin/redeem`, {
                method: 'POST',
                headers: headers,
                body: JSON.stringify({
                    userId: userId,
                    itemId: itemId
                })
            });

            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(errorData.message || `Purchase failed: ${response.statusText}`);
            }

            const data = await response.json();
            return data;
        } catch (error) {
            console.error('Error purchasing item:', error);
            throw error;
        }
    }

    async fetchRecentGrants(userId) {
        try {
            const headers = await this.getAuthHeaders();
            const response = await fetch(`${this.baseUrl}/budgetcoin/grants?userId=${userId}`, {
                method: 'GET',
                headers: headers
            });

            if (!response.ok) {
                throw new Error(`Failed to fetch grants: ${response.statusText}`);
            }

            const data = await response.json();
            return data;
        } catch (error) {
            console.error('Error fetching grants:', error);
            throw error;
        }
    }

    async fetchRedeemHistory(userId) {
        try {
            const headers = await this.getAuthHeaders();
            const response = await fetch(`${this.baseUrl}/budgetcoin/redeems?userId=${userId}`, {
                method: 'GET',
                headers: headers
            });

            if (!response.ok) {
                throw new Error(`Failed to fetch redeem history: ${response.statusText}`);
            }

            const data = await response.json();
            return data;
        } catch (error) {
            console.error('Error fetching redeem history:', error);
            throw error;
        }
    }
}

window.budgetCoinService = new BudgetCoinService();
