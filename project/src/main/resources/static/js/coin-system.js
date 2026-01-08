// OVER-SAVE Coin System
// This manages the Budget Coins feature across all pages

class CoinSystem {
    constructor() {
        this.currentBalance = 0; // Will be loaded from backend
        this.apiUrl = '/api/budgetcoin';
        this.init();
    }

    async init() {
        // Fetch balance from backend
        await this.loadBalanceFromBackend();

        // Update balance display on page load
        this.updateBalanceDisplay();
    }

    navigateToRewardsShop() {
        window.location.href = 'shopping_page.html';
    }

    async loadBalanceFromBackend() {
        try {
            const userInfo = JSON.parse(localStorage.getItem('userInfo'));
            const sessionToken = localStorage.getItem('sessionToken');

            if (!userInfo || !userInfo.userId || !sessionToken) {
                console.warn('⚠️ No user session found for coin balance');
                // Fall back to localStorage
                const savedBalance = localStorage.getItem('oversave-coin-balance');
                if (savedBalance) {
                    this.currentBalance = parseInt(savedBalance);
                }
                return;
            }

            console.log('📡 Fetching coin balance from backend for user:', userInfo.userId);

            const response = await fetch(`${this.apiUrl}/balance`, {
                method: 'GET',
                headers: {
                    'Authorization': `Bearer ${sessionToken}`,
                    'Content-Type': 'application/json'
                }
            });

            if (!response.ok) {
                throw new Error(`Failed to fetch balance: ${response.status}`);
            }

            const data = await response.json();
            this.currentBalance = Number(data.balance) || 0;

            // Update localStorage as cache
            localStorage.setItem('oversave-coin-balance', this.currentBalance.toString());

            console.log('✅ Coin balance loaded:', this.currentBalance);
            window.dispatchEvent(new CustomEvent('budgetcoin:updated', { detail: { balance: this.currentBalance } }));

        } catch (error) {
            console.error('❌ Error loading coin balance:', error);
            // Fall back to localStorage
            const savedBalance = localStorage.getItem('oversave-coin-balance');
            if (savedBalance) {
                this.currentBalance = parseInt(savedBalance);
            }
        }
    }

    updateBalanceDisplay() {
        const balanceElement = document.getElementById('budget-coin-balance');
        if (balanceElement) {
            balanceElement.textContent = this.currentBalance.toLocaleString();
        }
    }

    async earnCoins(amount, reason, options = {}) {
        if (!amount || amount <= 0) {
            console.warn('⚠️ Invalid coin amount specified for grant:', amount);
            return null;
        }

        try {
            const userInfo = JSON.parse(localStorage.getItem('userInfo'));
            const sessionToken = localStorage.getItem('sessionToken');

            if (!userInfo || !userInfo.userId || !sessionToken) {
                console.warn('⚠️ No user session found, cannot grant coins');
                return null;
            }

            const payload = {
                amount: amount,
                sourceType: options.sourceType || reason || 'Reward',
                rewardEventId: options.rewardEventId || `client-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`
            };

            console.log(`💰 Granting ${amount} coins for: ${payload.sourceType}`);

            const response = await fetch(`${this.apiUrl}/grant`, {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${sessionToken}`,
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(payload)
            });

            if (!response.ok) {
                throw new Error(`Failed to grant coins: ${response.status}`);
            }

            const data = await response.json();

            if (data.balanceAfter !== undefined && data.balanceAfter !== null) {
                this.currentBalance = Number(data.balanceAfter);
            } else {
                this.currentBalance += amount;
            }

            localStorage.setItem('oversave-coin-balance', this.currentBalance.toString());
            this.updateBalanceDisplay();
            this.showEarningNotification(amount, payload.sourceType);
            this.pulseBalance();

            console.log('✅ Coins granted successfully. New balance:', this.currentBalance);
            window.dispatchEvent(new CustomEvent('budgetcoin:updated', { detail: { balance: this.currentBalance } }));
            return data;
        } catch (error) {
            console.error('❌ Error granting coins:', error);
            throw error;
        }
    }

    async spendCoins(payload, legacyItemLabel) {
        const request = (typeof payload === 'object' && payload !== null)
            ? { ...payload }
            : { amount: payload, itemName: legacyItemLabel };

        const spendAmount = Number(request.amount ?? 0);

        if (!request.itemId) {
            console.error('❌ Missing itemId for redemption request.');
            return false;
        }

        if (this.currentBalance < spendAmount) {
            this.showInsufficientFundsAlert(spendAmount);
            return false;
        }

        try {
            const userInfo = JSON.parse(localStorage.getItem('userInfo'));
            const sessionToken = localStorage.getItem('sessionToken');

            if (!userInfo || !userInfo.userId || !sessionToken) {
                console.warn('⚠️ No user session found, cannot redeem coins');
                return false;
            }

            console.log(`💸 Redeeming ${spendAmount} coins for itemId: ${request.itemId}`);

            const response = await fetch(`${this.apiUrl}/redeem`, {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${sessionToken}`,
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    itemId: request.itemId
                })
            });

            if (!response.ok) {
                const errorData = await response.json().catch(() => ({}));
                const errorMsg = errorData.message || `Failed to redeem coins: ${response.status}`;
                throw new Error(errorMsg);
            }

            const data = await response.json();

            if (data.balanceAfter !== undefined && data.balanceAfter !== null) {
                this.currentBalance = Number(data.balanceAfter);
            } else {
                this.currentBalance -= spendAmount;
            }

            localStorage.setItem('oversave-coin-balance', this.currentBalance.toString());
            this.updateBalanceDisplay();

            console.log('✅ Coins redeemed successfully. New balance:', this.currentBalance);

            window.dispatchEvent(new CustomEvent('budgetcoin:updated', { detail: { balance: this.currentBalance } }));

            return data;
        } catch (error) {
            console.error('❌ Error redeeming coins:', error);
            alert(`Failed to redeem coins: ${error.message}`);
            return false;
        }
    }

    showEarningNotification(amount, reason) {
        const notification = document.createElement('div');
        notification.style.cssText = `
            position: fixed;
            top: 100px;
            right: 2rem;
            background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
            color: white;
            padding: 0.75rem 1rem;
            border-radius: 10px;
            font-size: 0.875rem;
            font-weight: 600;
            z-index: 10000;
            animation: slideInRight 0.5s ease-out;
            box-shadow: 0 4px 15px rgba(245, 158, 11, 0.3);
        `;
        notification.textContent = `+${amount} 🪙 ${reason}`;
        document.body.appendChild(notification);

        setTimeout(() => notification.remove(), 3000);
    }

    showInsufficientFundsAlert(cost) {
        alert(`😔 Insufficient Budget Coins!\n\nYou need ${cost} coins but only have ${this.currentBalance}.\n\nKeep saving and hitting your budget goals to earn more coins!`);
    }

    pulseBalance() {
        const coinBalance = document.querySelector('.budget-coin-balance');
        if (coinBalance) {
            coinBalance.style.animation = 'pulse 0.8s ease-in-out';
            setTimeout(() => {
                coinBalance.style.animation = '';
            }, 800);
        }
    }

    showEarningsInfo() {
        alert(`💰 Budget Coins: ${this.currentBalance.toLocaleString()}\n\n🎯 Recent Earnings:\n• Logged 3 transactions today: +75 coins\n• Stayed under budget this week: +200 coins\n• Daily streak (5 days): +125 coins\n\n🛍️ Visit Rewards Shop to spend coins!\n\n💡 Earn more by:\n- Logging expenses daily (+25 each)\n- Staying within budgets (+50-200)\n- Reaching milestones (+300-500)\n- Weekly consistency streaks (+200)`);
    }

    // Common earning triggers
    earnFromTransaction() {
        this.earnCoins(25, 'Transaction logged');
    }

    earnFromBudgetGoal() {
        this.earnCoins(100, 'Budget goal achieved');
    }

    earnFromSavingsGoal() {
        this.earnCoins(150, 'Savings milestone reached');
    }

    earnFromDailyStreak() {
        this.earnCoins(50, 'Daily streak bonus');
    }

    earnFromWeeklyStreak() {
        this.earnCoins(200, 'Weekly streak bonus');
    }

    earnFromChallenge() {
        this.earnCoins(300, 'Challenge completed');
    }

    async fetchHistory() {
        const sessionToken = localStorage.getItem('sessionToken');
        if (!sessionToken) {
            console.warn('⚠️ No session token present when requesting coin history');
            return [];
        }

        try {
            const response = await fetch(`${this.apiUrl}/history`, {
                method: 'GET',
                headers: {
                    'Authorization': `Bearer ${sessionToken}`,
                    'Content-Type': 'application/json'
                }
            });

            if (!response.ok) {
                throw new Error(`Failed to load coin history: ${response.status}`);
            }

            return await response.json();
        } catch (error) {
            console.error('❌ Error fetching coin history:', error);
            return [];
        }
    }
}

// Global coin system instance
let coinSystem;

// Initialize when DOM loads
document.addEventListener('DOMContentLoaded', function() {
    coinSystem = new CoinSystem();

    // Add CSS for notification animation if not already present
    if (!document.getElementById('coin-animations')) {
        const style = document.createElement('style');
        style.id = 'coin-animations';
        style.textContent = `
            @keyframes slideInRight {
                from {
                    opacity: 0;
                    transform: translateX(50px);
                }
                to {
                    opacity: 1;
                    transform: translateX(0);
                }
            }
        `;
        document.head.appendChild(style);
    }
});

// Global function for the earnings info button
function showEarningsInfo() {
    if (coinSystem) {
        coinSystem.showEarningsInfo();
    }
}

// Export for other modules
if (typeof module !== 'undefined' && module.exports) {
    module.exports = CoinSystem;
}
