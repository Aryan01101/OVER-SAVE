/* ------------------------------------------------------------
   UI Event Handlers (DOMContentLoaded)
   ------------------------------------------------------------ */
document.addEventListener('DOMContentLoaded', () => {
    // === Filter Buttons ===
    document.querySelectorAll('.filter-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            document.querySelectorAll('.filter-btn').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
        });
    });

    // === Logout Handler ===
    const logoutBtn = document.querySelector('.header-logout-btn');
    if (logoutBtn) {
        logoutBtn.addEventListener('click', () => {
            if (confirm('Are you sure you want to logout?')) {
                AuthManager.logout();
            }
        });
    }

    // === Chart ===
    initializeSpendingChart();
});

/* ------------------------------------------------------------
   Utility & Helper Functions
   ------------------------------------------------------------ */
function getCategoryIcon(categoryName) {
    const icons = {
        Food: '🍔', Transport: '🚗', Entertainment: '🎮',
        Housing: '🏠', Education: '📚', Health: '🏥',
        Shopping: '🛍️', Utilities: '💡', Groceries: '🛒',
        Personal: '👤', Fitness: '💪', Travel: '✈️'
    };
    return icons[categoryName] || '💰';
}

function escapeHtml(str) {
    if (typeof str !== 'string') return str;
    return str
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

function navigateToRewardsShop() {
    window.location.href = 'shopping_page.html';
}

/* ------------------------------------------------------------
   SPENDING TREND CHART 
   ------------------------------------------------------------ */

// Global variable to track current chart period
let currentChartPeriod = 'WEEK';

/**
 * Initialize spending trend chart with filter buttons
 */
function initializeSpendingChart() {
    console.log('📊 Initializing spending chart...');
    
    const filterButtons = document.querySelectorAll('.chart-filters .filter-btn');
    
    if (filterButtons.length === 0) {
        console.warn('⚠️ No chart filter buttons found');
        return;
    }
    
    filterButtons.forEach(button => {
        button.addEventListener('click', async function() {
            // Remove active from all buttons
            filterButtons.forEach(btn => btn.classList.remove('active'));
            
            // Add active to clicked button
            this.classList.add('active');
            
            // Get period from button text (Week/Month/Year)
            const period = this.textContent.trim().toUpperCase();
            currentChartPeriod = period;
            
            console.log('📊 Chart period changed to:', period);
            
            // Reload chart with new period
            await loadSpendingTrendChart(period);
        });
    });
    
    // Load initial chart (Week by default)
    loadSpendingTrendChart('WEEK');
}

/**
 * Load and render spending trend chart for specified period
 */
async function loadSpendingTrendChart(period = 'WEEK') {
    const chartContainer = document.querySelector('.spending-chart');
    
    if (!chartContainer) {
        console.error('❌ Chart container not found');
        return;
    }
    
    // Show loading state
    chartContainer.classList.add('loading');
    chartContainer.innerHTML = '<div class="loading-message">📊 Loading spending trend...</div>';
    
    try {
        console.log(`📊 Fetching spending trend for period: ${period}`);
        
        // Fetch data from backend API
        const trendData = await DashboardAPI.getSpendingTrend(period);
        
        console.log('📊 Received trend data:', trendData);
        
        // Handle empty data - check if ALL amounts are zero or no data points
        const hasAnyData = trendData && trendData.dataPoints && trendData.dataPoints.length > 0;
        const allZero = hasAnyData && trendData.dataPoints.every(dp => parseFloat(dp.amount) === 0);
        
        if (!hasAnyData || allZero) {
            chartContainer.classList.remove('loading');
            chartContainer.style.display = 'flex';
            chartContainer.style.alignItems = 'center';
            chartContainer.style.justifyContent = 'center';
            chartContainer.innerHTML = `
                <div class="empty-state" style="text-align: center; color: #6b7280;">
                    <div style="font-size: 3rem; margin-bottom: 1rem;">📊</div>
                    <h3 style="margin: 0 0 0.5rem 0; font-size: 1.125rem; font-weight: 600; color: #475569;">No Data Yet</h3>
                    <p style="margin: 0; font-size: 0.875rem; color: #6b7280;">No expenses recorded for this ${period.toLowerCase()}.<br>Start adding transactions to see your spending trend!</p>
                </div>
            `;
            chartContainer.classList.add('loaded');
            return;
        }
        
        // Remove loading state
        chartContainer.classList.remove('loading');
        
        // Calculate max amount for scaling bars
        const amounts = trendData.dataPoints.map(dp => parseFloat(dp.amount) || 0);
        const maxAmount = Math.max(...amounts, 1); // Minimum 1 to avoid division by zero
        
        console.log('📊 Max amount:', maxAmount);
        console.log('📊 Data points:', trendData.dataPoints.length);
        
        // Render chart bars
        chartContainer.innerHTML = trendData.dataPoints.map(dp => {
            const amount = parseFloat(dp.amount) || 0;
            const height = (amount / maxAmount * 100) || 0;
            
            // Highlight today/current period
            const bgStyle = dp.isToday
                ? 'background: linear-gradient(to top, #a5b4fc, #e0e7ff);'
                : 'background: linear-gradient(to top, var(--primary), #a5b4fc);';
            
            // Show "Today" for current day with no spending, otherwise show amount
            const displayValue = dp.isToday && amount === 0 
                ? 'Today' 
                : DashboardAPI.formatCurrency(amount);
            
            return `
                <div class="chart-bar" style="height: ${height}%; ${bgStyle}" title="${dp.label}: ${DashboardAPI.formatCurrency(amount)}">
                    <span class="chart-bar-label">${dp.label}</span>
                    <span class="chart-bar-value">${displayValue}</span>
                </div>
            `;
        }).join('');
        
        // Add fade-in animation
        requestAnimationFrame(() => {
            chartContainer.classList.add('loaded');
        });
        
        console.log('✅ Chart rendered successfully with', trendData.dataPoints.length, 'bars');
        
    } catch (error) {
        console.error('❌ Error loading spending trend chart:', error);
        chartContainer.classList.remove('loading');
        chartContainer.innerHTML = `
            <div class="loading-message" style="color: #ef4444; padding: 2rem; text-align: center;">
                ❌ Failed to load chart data
                <br>
                <button onclick="loadSpendingTrendChart('${period}')" 
                        style="margin-top: 0.5rem; padding: 0.5rem 1rem; background: var(--primary); color: white; border: none; border-radius: 8px; cursor: pointer;">
                    Retry
                </button>
            </div>
        `;
    }
}

// Make function globally available
window.loadSpendingTrendChart = loadSpendingTrendChart;

/* ------------------------------------------------------------
   Dashboard Section Loaders (Top Categories, Budgets)
   ------------------------------------------------------------ */

// === Load Top Expense Categories ===
async function loadTopExpenseCategories(limit = 3) {
    const container = document.getElementById('budget-utilization-list');
    if (!container) {
        console.warn('⚠️ Top categories container not found');
        return;
    }

    container.innerHTML = `
        <div class="loading-message" style="text-align:center; padding:2rem; color:#6b7280;">
            📊 Loading top categories...
        </div>
    `;

    try {
        const expenses = await expenseService.getAllExpenses();
        
        console.log('📊 Loaded expenses for top categories:', expenses?.length || 0);
        
        if (!expenses || expenses.length === 0) {
            container.innerHTML = `
                <div class="empty-state" style="text-align:center; padding:2rem; color:#6b7280;">
                    <div style="font-size: 2.5rem; margin-bottom: 1rem;">💸</div>
                    <h4 style="margin: 0 0 0.5rem 0; font-weight: 600;">No Expenses Yet</h4>
                    <p style="margin: 0; font-size: 0.875rem;">Add some transactions to see your top spending categories.</p>
                </div>
            `;
            container.classList.add('loaded');
            return;
        }

        // Group totals by category
        const totals = {};
        for (const tx of expenses) {
            const cat = tx.categoryName || 'Uncategorized';
            const amt = parseFloat(tx.amount) || 0;
            totals[cat] = (totals[cat] || 0) + amt;
        }

        // Sort and take top N
        const topCategories = Object.entries(totals)
            .sort((a, b) => b[1] - a[1])
            .slice(0, limit);

        // Render list items
        container.innerHTML = topCategories.map(([cat, total]) => `
            <div class="category-item" style="display:flex; align-items:center; justify-content:space-between; padding:0.75rem 0; border-bottom:1px solid #f1f5f9;">
                <div style="display:flex; align-items:center; gap:0.75rem;">
                    <div class="category-icon" style="font-size:1.25rem;">${getCategoryIcon(cat)}</div>
                    <div class="category-name" style="font-weight:500;">${escapeHtml(cat)}</div>
                </div>
                <div class="category-amount" style="font-weight:600; color:#ef4444;">
                    $${total.toLocaleString(undefined,{minimumFractionDigits:2,maximumFractionDigits:2})}
                </div>
            </div>
        `).join('');

        requestAnimationFrame(() => container.classList.add('loaded'));
        console.log('✅ Top categories rendered');
    } catch (err) {
        console.error('❌ Error loading top categories:', err);
        container.innerHTML = `
            <div style="text-align:center; padding:2rem; color:#ef4444;">
                ⚠️ Error loading top categories.<br>
                <button onclick="loadTopExpenseCategories()" style="color:var(--primary); background:none; border:none; cursor:pointer;">Try again</button>
            </div>
        `;
    }
}

// === Load Budget Status Section ===
async function loadBudgetStatus() {
    const container = document.getElementById('dashboard-budget-status');
    if (!container) {
        console.warn('⚠️ Budget status container not found');
        return;
    }

    container.innerHTML = `
        <div class="loading-message" style="text-align:center; padding:2rem; color:#6b7280;">
            📊 Loading budgets...
        </div>
    `;

    try {
        const categories = await budgetService.loadCategories();
        
        console.log('📊 Loaded categories for budgets:', categories?.length || 0);
        
        if (!categories || categories.length === 0) {
            container.innerHTML = `
                <div class="empty-state" style="text-align:center; padding:2rem; color:#6b7280;">
                    <div style="font-size: 2.5rem; margin-bottom: 1rem;">📋</div>
                    <h4 style="margin: 0 0 0.5rem 0; font-weight: 600;">No Categories Found</h4>
                    <p style="margin: 0 0 1rem 0; font-size: 0.875rem;">Create categories to set up your budgets.</p>
                    <a href="budgets_page.html" style="color:var(--primary); text-decoration: underline;">Set up budgets →</a>
                </div>
            `;
            container.classList.add('loaded');
            return;
        }

        const summaries = await Promise.all(
            categories.map(async category => {
                try {
                    const id = category.categoryId || category.id;
                    const summary = await budgetService.getBudgetSummary(id);
                    return summary && summary.budget > 0
                        ? { ...category, summary }
                        : null;
                } catch (err) {
                    console.warn(`⚠️ Failed to load budget for ${category.name}:`, err);
                    return null;
                }
            })
        );

        const validBudgets = summaries.filter(Boolean);
        
        console.log('📊 Valid budgets found:', validBudgets.length);
        
        if (validBudgets.length === 0) {
            container.innerHTML = `
                <div class="empty-state" style="text-align:center; padding:2rem; color:#6b7280;">
                    <div style="font-size: 2.5rem; margin-bottom: 1rem;">💰</div>
                    <h4 style="margin: 0 0 0.5rem 0; font-weight: 600;">No Budgets Set</h4>
                    <p style="margin: 0 0 1rem 0; font-size: 0.875rem;">Create your first budget to start tracking your spending.</p>
                    <a href="budgets_page.html" style="color:var(--primary); text-decoration: underline;">Create budget →</a>
                </div>
            `;
            container.classList.add('loaded');
            return;
        }

        validBudgets.sort((a, b) => (b.summary.budget || 0) - (a.summary.budget || 0));
        container.innerHTML = validBudgets.slice(0, 4).map(renderBudgetStatusItem).join('');

        requestAnimationFrame(() => container.classList.add('loaded'));
        console.log('✅ Budget status rendered');
    } catch (error) {
        console.error('❌ Error loading budgets:', error);
        container.innerHTML = `
            <div style="text-align:center; padding:2rem; color:#ef4444;">
                ❌ Error loading budget data.<br>
                <button onclick="loadBudgetStatus()" style="color:var(--primary); background:none; border:none; cursor:pointer;">Try again</button>
            </div>
        `;
    }
}

function renderBudgetStatusItem(item) {
    const displayName = item.displayName || (item.summary?.customName || item.summary?.name || item.summary?.title) || item.name || 'Unnamed budget';
    const categoryName = item.name || 'Uncategorized';
    const { spent = 0, budget = 0 } = item.summary || {};
    const percentage = budget > 0 ? Math.round((spent / budget) * 100) : 0;
    const remaining = budget - spent;

    let progressClass = 'safe';
    let warningText = '';
    if (percentage >= 100) { progressClass = 'danger'; warningText = ' ⚠️'; }
    else if (percentage >= 90) { progressClass = 'danger'; warningText = ' ⚠️'; }
    else if (percentage >= 80) { progressClass = 'warning'; }

    const icon = getCategoryIcon(categoryName);

    return `
        <div class="budget-item">
            <div class="budget-header">
                <div class="budget-category"><span>${icon}</span> ${escapeHtml(displayName)}</div>
                <div class="budget-amount">$${spent.toFixed(2)} / $${budget.toFixed(2)}</div>
            </div>
            <div class="progress-bar">
                <div class="progress-fill ${progressClass}" style="width: ${Math.min(percentage, 100)}%"></div>
            </div>
            <div class="progress-text">${percentage}% used • $${remaining.toFixed(2)} remaining${warningText}</div>
        </div>
    `;
}

/* ------------------------------------------------------------
   Dashboard Data Pipeline
   ------------------------------------------------------------ */
async function loadDashboardData() {
    console.log('🚀 FR-14: Loading dashboard data...');

    try {
        const dashboardData = await DashboardAPI.getDashboardData();
        if (!dashboardData.dataAvailable) {
            showFallbackMessage(dashboardData.message);
            return;
        }

        const [goalsData] = await Promise.all([
            goalService.getAllGoals().catch(() => []),
            loadTopExpenseCategories(),
            loadBudgetStatus()
        ]);

        const totalSavings = goalsData.reduce((sum, goal) => sum + (goal.currentAmount || 0), 0);
        const avgProgress = goalsData.length > 0
            ? goalsData.reduce((sum, goal) => sum + (goal.progress || 0), 0) / goalsData.length
            : 0;

        if (dashboardData.financialAggregates) {
            dashboardData.financialAggregates.totalSavings = totalSavings;
            dashboardData.financialAggregates.goalsProgressPercent = avgProgress;
        }

        renderFinancialAggregates(dashboardData.financialAggregates);
        renderRecentTransactions(dashboardData.recentTransactions);
        renderSavingsGoals(goalsData);

        console.log('✅ FR-14: Dashboard data loaded successfully');
    } catch (error) {
        console.error('❌ FR-14: Error loading dashboard:', error);
        showFallbackMessage('Failed to load dashboard data. Please refresh the page.');
    }
}

/* ------------------------------------------------------------
   Rendering Functions
   ------------------------------------------------------------ */
function renderFinancialAggregates(aggregates) {
    if (!aggregates) return;

    const incomeValue = document.getElementById('total-income');
    if (incomeValue) incomeValue.textContent = DashboardAPI.formatCurrency(aggregates.monthlyIncome);
    const expenseValue = document.getElementById('total-expenses');
    if (expenseValue) expenseValue.textContent = DashboardAPI.formatCurrency(aggregates.monthlyExpenses);
    const balanceValue = document.getElementById('account-balance');
    if (balanceValue) balanceValue.textContent = DashboardAPI.formatCurrency(aggregates.currentBalance);
    const savingsRateText = document.getElementById('savings-rate-text');
    if (savingsRateText && aggregates.savingsRate != null)
        savingsRateText.textContent = `${aggregates.savingsRate.toFixed(1)}%`;
    const savingsValue = document.getElementById('total-savings');
    if (savingsValue) savingsValue.textContent = DashboardAPI.formatCurrency(aggregates.totalSavings);
    const goalsProgressText = document.getElementById('goals-progress-text');
    if (goalsProgressText && aggregates.goalsProgressPercent != null)
        goalsProgressText.textContent = `${aggregates.goalsProgressPercent.toFixed(0)}%`;

    const statsSection = document.getElementById('dashboard-stats');
    if (statsSection) {
        statsSection.classList.remove('loaded');
        requestAnimationFrame(() => statsSection.classList.add('loaded'));
    }
}

function renderRecentTransactions(transactions) {
    const container = document.getElementById('dashboard-transactions-container');
    if (!container) return;

    const startTime = Date.now();
    const minLoadingTime = 500;

    const doRender = () => {
        if (!transactions || transactions.length === 0) {
            container.innerHTML = `
                <div class="empty-state" style="text-align:center; padding:2rem; color:#6b7280;">
                    <div style="font-size: 2.5rem; margin-bottom: 1rem;">📋</div>
                    <h4 style="margin: 0 0 0.5rem 0; font-weight: 600;">No Recent Transactions</h4>
                    <p style="margin: 0; font-size: 0.875rem;">Start adding income and expenses to see them here.</p>
                </div>`;
        } else {
            container.innerHTML = transactions.map(txn => `
                <div class="transaction-item">
                    <div class="transaction-icon" style="background:${DashboardAPI.getTransactionColor(txn.type)};">
                        ${txn.categoryIcon || DashboardAPI.getTransactionIcon(txn.type, txn.categoryName)}
                    </div>
                    <div class="transaction-details">
                        <div class="transaction-title">${txn.description || 'Unnamed Transaction'}</div>
                        <div class="transaction-category">
                            ${txn.categoryName || 'Uncategorized'} • ${DashboardAPI.formatDate(txn.occurredAt)}
                        </div>
                    </div>
                    <div class="transaction-amount ${txn.type.toLowerCase()}">
                        ${txn.type === 'INCOME' ? '+' : '-'}${DashboardAPI.formatCurrency(txn.amount)}
                    </div>
                </div>`).join('');
        }
        requestAnimationFrame(() => container.classList.add('loaded'));
    };

    const elapsed = Date.now() - startTime;
    if (elapsed < minLoadingTime) setTimeout(doRender, minLoadingTime - elapsed);
    else doRender();
}

function renderSavingsGoals(goals) {
    const container = document.getElementById('dashboard-goals-container');
    if (!container) return;

    const startTime = Date.now();
    const minLoadingTime = 500;

    const doRender = () => {
        if (goals.length === 0) {
            container.innerHTML = `
                <div class="empty-state" style="text-align:center; padding:2rem; color:#6b7280;">
                    <div style="font-size: 2.5rem; margin-bottom: 1rem;">🎯</div>
                    <h4 style="margin: 0 0 0.5rem 0; font-weight: 600;">No Active Goals</h4>
                    <p style="margin: 0 0 1rem 0; font-size: 0.875rem;">Create your first savings goal to start tracking your progress.</p>
                    <a href="goals_page.html" style="color:var(--primary); text-decoration: underline;">Create goal →</a>
                </div>`;
        } else {
            container.innerHTML = goals.map(goal => {
                const progress = goal.progress || 0;
                const current = goal.currentAmount || 0;
                const target = goal.targetAmount || 0;
                const remaining = target - current;
                const icon = goal.icon || '🎯';
                const dueDateText = goal.dueDate
                    ? new Date(goal.dueDate).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })
                    : '';
                let progressClass = 'danger';
                if (progress >= 75) progressClass = 'safe';
                else if (progress >= 50) progressClass = 'warning';

                return `
                    <div class="goal-item">
                        <div class="goal-header">
                            <div class="goal-name"><span>${icon}</span> ${goal.name}</div>
                            <div class="goal-amount">$${current.toLocaleString()} / $${target.toLocaleString()}</div>
                        </div>
                        <div class="progress-bar">
                            <div class="progress-fill ${progressClass}" style="width:${Math.min(progress,100)}%"></div>
                        </div>
                        <div class="progress-text">
                            ${progress.toFixed(1)}% complete • $${remaining.toLocaleString()} to go
                            ${progress >= 90 ? ' 🎉' : ''}
                            ${dueDateText ? ` • Due: ${dueDateText}` : ''}
                        </div>
                    </div>`;
            }).join('');
        }
        requestAnimationFrame(() => container.classList.add('loaded'));
    };

    const elapsed = Date.now() - startTime;
    if (elapsed < minLoadingTime) setTimeout(doRender, minLoadingTime - elapsed);
    else doRender();
}

/* ------------------------------------------------------------
    Error & Refresh Handling
   ------------------------------------------------------------ */
function showFallbackMessage(message) {
    const mainContent = document.querySelector('.dashboard-content');
    if (!mainContent) return;

    const fallbackDiv = document.createElement('div');
    fallbackDiv.style.cssText = `
        position:fixed; top:50%; left:50%; transform:translate(-50%, -50%);
        background:white; padding:2rem; border-radius:15px;
        box-shadow:0 10px 30px rgba(0,0,0,0.2); text-align:center; z-index:1000;
    `;
    fallbackDiv.innerHTML = `
        <div style="font-size:3rem; margin-bottom:1rem;">⚠️</div>
        <h3 style="margin-bottom:1rem;">Data Unavailable</h3>
        <p style="color:#6b7280; margin-bottom:1.5rem;">${message}</p>
        <button onclick="location.reload()" class="btn btn-primary">Refresh Page</button>
    `;
    mainContent.appendChild(fallbackDiv);
}

/* ------------------------------------------------------------
   Auto-Refresh Logic
   ------------------------------------------------------------ */
let dashboardRefreshInterval;

async function refreshDashboard() {
    console.log('🔄 FR-14: Refreshing dashboard...');
    await loadDashboardData();
}

function startDashboardRefresh() {
    dashboardRefreshInterval = setInterval(refreshDashboard, 30000);
    console.log('✅ FR-14: Auto-refresh enabled (every 30s)');
}

function stopDashboardRefresh() {
    if (dashboardRefreshInterval) {
        clearInterval(dashboardRefreshInterval);
        console.log('⏹️ FR-14: Auto-refresh disabled');
    }
}

document.addEventListener('visibilitychange', () => {
    if (document.hidden) stopDashboardRefresh();
    else {
        refreshDashboard();
        startDashboardRefresh();
    }
});