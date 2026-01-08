let currentBalance = 0;
let currentUserId = null;
let allItems = [];

// Initialize page
document.addEventListener('DOMContentLoaded', async function () {
    // Get user info from localStorage
    const userInfo = AuthManager.getUserInfo();
    if (!userInfo || !userInfo.userId) {
        window.location.href = '/login.html';
        return;
    }

    currentUserId = userInfo.userId;

    // Load balance and items
    await loadUserBalance();
    await loadItems();
    await loadRecentGrants();

    // Setup modal click handler
    document.getElementById('purchaseModal').addEventListener('click', function (e) {
        if (e.target === this) {
            closePurchaseModal();
        }
    });
});

async function loadUserBalance() {
    try {
        const balanceData = await window.budgetCoinService.fetchUserBalance(currentUserId);
        currentBalance = parseInt(balanceData.balance);
        updateBalanceDisplay();
    } catch (error) {
        console.error('Failed to load balance:', error);
        currentBalance = 0;
        updateBalanceDisplay();
    }
}

async function loadItems() {
    try {
        allItems = await window.budgetCoinService.fetchAllItems();
        renderItems(allItems);
    } catch (error) {
        console.error('Failed to load items:', error);
        showError('Failed to load items. Please refresh the page.');
    }
}

async function loadRecentGrants() {
    try {
        const grants = await window.budgetCoinService.fetchRecentGrants(currentUserId);
        renderRecentGrants(grants.slice(0, 3)); // Show only top 3
    } catch (error) {
        console.error('Failed to load grants:', error);
    }
}

function renderItems(items) {
    const productsGrid = document.querySelector('.products-grid');
    productsGrid.innerHTML = '';

    if (!items || items.length === 0) {
        productsGrid.innerHTML = '<p style="grid-column: 1/-1; text-align: center; color: var(--gray);">No items available at the moment.</p>';
        return;
    }

    items.forEach(item => {
        const productCard = createProductCard(item);
        productsGrid.appendChild(productCard);
    });
}

function createProductCard(item) {
    const card = document.createElement('div');
    card.className = 'product-card';

    const emoji = item.emoji || '🎁';
    const isOutOfStock = item.stockQty <= 0;
    const canAfford = currentBalance >= item.price;
    const canPurchase = !isOutOfStock && canAfford;

    let buttonHtml;
    if (isOutOfStock) {
        buttonHtml = '<button class="buy-btn insufficient-funds" disabled>📦 Out of Stock</button>';
    } else if (!canAfford) {
        buttonHtml = '<button class="buy-btn insufficient-funds" disabled>💸 Insufficient Coins</button>';
    } else {
        buttonHtml = `<button class="buy-btn" onclick="purchaseItemById(${item.itemId}, '${item.itemName.replace(/'/g, "\\'")}', ${item.price})">🛒 Purchase</button>`;
    }

    card.innerHTML = `
            <div class="product-image digital">
                <span style="font-size: 4rem;">${emoji}</span>
            </div>
            <div class="product-info">
                <div class="product-title">${item.itemName}</div>
                <div class="product-description">${item.description || 'No description available.'}</div>
                <div class="product-price">
                    <div class="price-amount">
                        <span>🪙 ${item.price}</span>
                    </div>
                    <div style="font-size: 0.75rem; color: var(--gray);">
                        Stock: ${item.stockQty}
                    </div>
                </div>
                ${buttonHtml}
            </div>
        `;

    return card;
}

function renderRecentGrants(grants) {
    const earningsSection = document.querySelector('.earnings-section');
    const existingItems = earningsSection.querySelectorAll('.earnings-item');
    existingItems.forEach(item => item.remove());

    if (!grants || grants.length === 0) {
        earningsSection.innerHTML += '<p style="color: var(--gray); font-size: 0.875rem;">No recent earnings yet.</p>';
        return;
    }

    grants.forEach(grant => {
        const earningItem = document.createElement('div');
        earningItem.className = 'earnings-item';

        const sourceIcon = getSourceIcon(grant.sourceType);
        const timeAgo = formatTimeAgo(new Date(grant.createdAt));

        earningItem.innerHTML = `
                <div class="earnings-info">
                    <span class="earnings-icon">${sourceIcon}</span>
                    <div class="earnings-details">
                        <div class="earnings-title">${grant.sourceType}</div>
                        <div class="earnings-time">${timeAgo}</div>
                    </div>
                </div>
                <div class="earnings-amount">+${grant.amount} 🪙</div>
            `;

        earningsSection.appendChild(earningItem);
    });
}

function getSourceIcon(sourceType) {
    const icons = {
        'BUDGET_GOAL': '🎯',
        'EXPENSE_LOG': '💸',
        'SAVINGS_MILESTONE': '🏆',
        'WEEKLY_STREAK': '⚡',
        'CHALLENGE': '🎉',
        'PURCHASE_REWARD': '🎁'
    };
    return icons[sourceType] || '💰';
}

function formatTimeAgo(date) {
    const now = new Date();
    const diffMs = now - date;
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMs / 3600000);
    const diffDays = Math.floor(diffMs / 86400000);

    if (diffMins < 1) return 'Just now';
    if (diffMins < 60) return `${diffMins} minute${diffMins > 1 ? 's' : ''} ago`;
    if (diffHours < 24) return `${diffHours} hour${diffHours > 1 ? 's' : ''} ago`;
    if (diffDays < 7) return `${diffDays} day${diffDays > 1 ? 's' : ''} ago`;
    return date.toLocaleDateString();
}

async function purchaseItemById(itemId, itemName, price) {
    try {
        const response = await window.budgetCoinService.purchaseItem(currentUserId, itemId);

        // Update balance
        await loadUserBalance();

        // Show success modal
        document.getElementById('purchase-description').textContent =
            `You have successfully purchased ${itemName} for ${price} Budget Coins!`;
        document.getElementById('purchaseModal').classList.add('active');

        // Add purchase animation
        const coinBalance = document.querySelector('.budget-coin-balance');
        coinBalance.style.animation = 'pulse 0.5s ease-in-out';
        setTimeout(() => {
            coinBalance.style.animation = '';
        }, 500);

        // Reload items to update stock
        await loadItems();

    } catch (error) {
        console.error('Purchase failed:', error);
        let errorMessage = 'Purchase failed. Please try again.';

        if (error.message.includes('Insufficient')) {
            errorMessage = `😔 Insufficient Budget Coins!\n\nYou need ${price} coins but only have ${currentBalance}.\n\nKeep saving and hitting your budget goals to earn more coins!`;
        } else if (error.message.includes('out of stock')) {
            errorMessage = 'Sorry, this item is currently out of stock.';
        }

        alert(errorMessage);
    }
}

function updateBalanceDisplay() {
    document.getElementById('current-balance').textContent = currentBalance.toLocaleString();
}

function closePurchaseModal() {
    document.getElementById('purchaseModal').classList.remove('active');
}

function showEarningsHistory() {
    alert('💰 Earnings History\n\nYou earn Budget Coins by:\n\n🎯 Staying under budget (+50-150 coins)\n📊 Logging daily expenses (+25 coins)\n🏆 Reaching savings milestones (+100-500 coins)\n⚡ Weekly streak bonuses (+200 coins)\n🎉 Completing challenges (+300 coins)');
}

function showError(message) {
    alert(message);
}

// Tab switching (kept for future use if categories are added)
function switchTab(tab) {
    document.querySelectorAll('.tab-btn').forEach(btn => btn.classList.remove('active'));
    document.querySelectorAll('.tab-content').forEach(content => content.classList.remove('active'));

    event.target.classList.add('active');
    document.getElementById(tab + '-tab').classList.add('active');
}

// Responsive sidebar toggle
const menuToggle = document.createElement('button');
menuToggle.innerHTML = '☰';
menuToggle.style.cssText = `
        display: none;
        position: fixed;
        top: 1rem;
        left: 1rem;
        z-index: 1001;
        background: var(--white);
        border: none;
        width: 40px;
        height: 40px;
        border-radius: 10px;
        font-size: 1.5rem;
        cursor: pointer;
        box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
    `;

if (window.innerWidth <= 768) {
    menuToggle.style.display = 'block';
}

window.addEventListener('resize', () => {
    if (window.innerWidth <= 768) {
        menuToggle.style.display = 'block';
    } else {
        menuToggle.style.display = 'none';
    }
});

document.body.appendChild(menuToggle);

menuToggle.addEventListener('click', () => {
    document.querySelector('.sidebar').classList.toggle('active');
});

// Add slide up animation
const style = document.createElement('style');
style.textContent = `
        @keyframes slideUp {
            from {
                opacity: 0;
                transform: translateY(20px);
            }
            to {
                opacity: 1;
                transform: translateY(0);
            }
        }
        @keyframes pulse {
            0%, 100% { transform: scale(1); }
            50% { transform: scale(1.05); }
        }
    `;
document.head.appendChild(style);