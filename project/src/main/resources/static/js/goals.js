// Navigate to Rewards Shop
function navigateToRewardsShop() {
    window.location.href = 'shopping_page.html';
}

let selectedGoalIcon = null;
let currentGoal = null;
let allGoals = [];
let contributionAccount = null;

function escapeHtml(value) {
    if (typeof value !== 'string') return '';
    return value
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

// Tab switching
function switchTab(tab) {
    document.querySelectorAll('.tab-btn').forEach(btn => btn.classList.remove('active'));
    document.querySelectorAll('.tab-content').forEach(content => content.classList.remove('active'));

    event.target.classList.add('active');
    document.getElementById(tab + '-tab').classList.add('active');
}

// Icon selection for goal creation
document.querySelectorAll('.icon-option').forEach(option => {
    option.addEventListener('click', function() {
        document.querySelectorAll('.icon-option').forEach(opt => opt.classList.remove('selected'));
        this.classList.add('selected');
        selectedGoalIcon = this.getAttribute('data-icon');
    });
});

// Quick amount selection
document.querySelectorAll('.quick-amount').forEach(option => {
    option.addEventListener('click', function() {
        document.querySelectorAll('.quick-amount').forEach(opt => opt.classList.remove('selected'));
        this.classList.add('selected');

        const amount = this.getAttribute('data-amount');
        if (amount !== 'custom') {
            document.getElementById('contribution-amount').value = amount;
        } else {
            document.getElementById('contribution-amount').focus();
        }
    });
});

// Modal functions
async function openContributeModal(goalName, current, target, goalId) {
    currentGoal = { name: goalName, current: current, target: target, id: goalId };
    document.getElementById('contributeModal').classList.add('active');

    // Update contribution summary
    const remaining = target - current;
    const percentage = ((current / target) * 100).toFixed(1);

    document.getElementById('contribution-summary').innerHTML = `
        <div style="text-align: center;">
            <h4 style="color: var(--dark); margin-bottom: 0.5rem;">Contributing to: ${goalName.charAt(0).toUpperCase() + goalName.slice(1)}</h4>
            <div style="font-size: 1.25rem; font-weight: bold; color: var(--primary);">$${current.toLocaleString()} / $${target.toLocaleString()}</div>
            <div style="color: var(--gray); font-size: 0.875rem;">${percentage}% complete • $${remaining.toLocaleString()} remaining</div>
        </div>
    `;

    document.getElementById('contribute-form').reset();
    document.querySelectorAll('.quick-amount').forEach(opt => opt.classList.remove('selected'));

    const accountInfo = document.getElementById('contribution-account-info');
    const submitBtn = document.querySelector('#contribute-form button[type="submit"]');

    if (submitBtn) submitBtn.disabled = true;
    contributionAccount = null;
    if (accountInfo) {
        accountInfo.textContent = 'Loading account information...';
    }

    try {
        const account = await goalService.getPrimaryContributionAccount();
        if (!account) {
            throw new Error('No available accounts found. Please add a cash account before contributing.');
        }
        contributionAccount = { ...account };
        if (accountInfo) {
            accountInfo.innerHTML = `
                <strong>${escapeHtml(account.name || 'Account')}</strong><br>
                Balance: $${Number(account.balance || 0).toLocaleString()}
            `;
        }
        if (submitBtn) submitBtn.disabled = false;
    } catch (error) {
        console.error('Failed to load contribution account:', error);
        if (accountInfo) {
            accountInfo.textContent = error.message || 'Unable to load account information.';
        }
        showErrorNotification(error.message || 'Unable to load account information.');
    }
}

function closeContributeModal() {
    document.getElementById('contributeModal').classList.remove('active');
    currentGoal = null;
    contributionAccount = null;
}

function editGoal(goalId) {
    console.log(`Editing goal: ${goalId}`);
    alert(`Edit functionality for goal ${goalId} coming soon.\n\nYou will be able to:\n• Change target amount\n• Update target date\n• Modify description\n• Archive/delete goal`);
}

function showGoalOptions(goalId) {
    // Simple implementation for now
    const goal = allGoals.find(g => g.id === goalId);
    if (!goal) return;

    if (confirm(`Delete goal "${goal.name}"?\n\nThis action cannot be undone.`)) {
        deleteGoalById(goalId);
    }
}

async function deleteGoalById(goalId) {
    try {
        await goalService.deleteGoal(goalId);
        showSuccessNotification('Goal deleted successfully!');
        await loadGoals();
    } catch (error) {
        console.error('Error deleting goal:', error);
        showErrorNotification(`Failed to delete goal: ${error.message}`);
    }
}

// Notification helpers
function showSuccessNotification(message) {
    const notification = document.createElement('div');
    notification.style.cssText = `
        position: fixed;
        top: 2rem;
        right: 2rem;
        background: #10b981;
        color: white;
        padding: 1rem 1.5rem;
        border-radius: 12px;
        box-shadow: 0 4px 12px rgba(0,0,0,0.15);
        z-index: 10000;
        max-width: 400px;
        animation: slideInRight 0.3s ease-out;
    `;
    notification.textContent = message;
    document.body.appendChild(notification);

    setTimeout(() => {
        notification.style.animation = 'slideOutRight 0.3s ease-out';
        setTimeout(() => notification.remove(), 300);
    }, 4000);
}

function showErrorNotification(message) {
    const notification = document.createElement('div');
    notification.style.cssText = `
        position: fixed;
        top: 2rem;
        right: 2rem;
        background: #ef4444;
        color: white;
        padding: 1rem 1.5rem;
        border-radius: 12px;
        box-shadow: 0 4px 12px rgba(0,0,0,0.15);
        z-index: 10000;
        max-width: 400px;
        animation: slideInRight 0.3s ease-out;
    `;
    notification.textContent = message;
    document.body.appendChild(notification);

    setTimeout(() => {
        notification.style.animation = 'slideOutRight 0.3s ease-out';
        setTimeout(() => notification.remove(), 300);
    }, 4000);
}

// Contribution form
document.getElementById('contribute-form').addEventListener('submit', async function(e) {
    e.preventDefault();

    const amountInput = document.getElementById('contribution-amount');
    const amount = parseFloat(amountInput.value);

    if (!amount || amount <= 0) {
        showErrorNotification('Please enter a valid contribution amount!');
        return;
    }

    const contribution = amount;
    const oldPercentage = (currentGoal.current / currentGoal.target) * 100;

    if (contribution > (currentGoal.target - currentGoal.current)) {
        if (!confirm(`This contribution ($${contribution.toFixed(2)}) exceeds your goal target by $${(contribution - (currentGoal.target - currentGoal.current)).toFixed(2)}.\n\nDo you want to proceed? The excess will complete the goal.`)) {
            return;
        }
    }

    try {
        if (!contributionAccount) {
            contributionAccount = await goalService.getPrimaryContributionAccount(true);
        }

        if (!contributionAccount) {
            showErrorNotification('No eligible account found for contributions.');
            return;
        }

        const availableBalance = Number(contributionAccount.balance || 0);
        if (contribution > availableBalance) {
            showErrorNotification(`Insufficient balance. You currently have $${availableBalance.toFixed(2)} available.`);
            return;
        }

        const accountId = contributionAccount.id;

        // Add contribution via API
        const contributionData = {
            fromAccountId: accountId,
            goalId: currentGoal.id,
            amount: contribution
        };

        console.log('Adding contribution:', contributionData);
        const result = await goalService.contributeToGoal(contributionData);

        if (result && typeof result.newCashBalance !== 'undefined') {
            contributionAccount.balance = Number(result.newCashBalance);
        } else {
            contributionAccount.balance = availableBalance - contribution;
        }

        const accountInfo = document.getElementById('contribution-account-info');
        if (accountInfo && contributionAccount) {
            accountInfo.innerHTML = `
                <strong>${escapeHtml(contributionAccount.name || 'Account')}</strong><br>
                Balance: $${Number(contributionAccount.balance || 0).toLocaleString()}
            `;
        }

        // Check for milestones
        const newTotal = currentGoal.current + contribution;
        const newPercentage = (newTotal / currentGoal.target) * 100;

        // Check if goal completed
        if (newPercentage >= 100) {
            showCelebration('🎉');
            if (typeof coinSystem !== 'undefined') {
                coinSystem.earnFromSavingsGoal();
            }
            setTimeout(() => {
                showSuccessNotification('🎉 GOAL COMPLETED! 🎉\n\nCongratulations! You\'ve achieved your savings goal!\n\n🏆 Achievement Unlocked: Goal Achiever\n💰 +150 Budget Coins earned! 🪙');
            }, 500);
        } else {
            // Check for milestone achievements
            if (newPercentage >= 75 && oldPercentage < 75) {
                setTimeout(() => showCelebration('🔥'), 500);
                if (typeof coinSystem !== 'undefined') {
                    coinSystem.earnCoins(75, '75% milestone reached');
                }
                setTimeout(() => showSuccessNotification('🔥 75% Milestone Reached!\n\nAlmost there! +75 Budget Coins earned! 🪙'), 1000);
            } else if (newPercentage >= 50 && oldPercentage < 50) {
                setTimeout(() => showCelebration('⭐'), 500);
                if (typeof coinSystem !== 'undefined') {
                    coinSystem.earnCoins(50, '50% milestone reached');
                }
                setTimeout(() => showSuccessNotification('⭐ 50% Milestone Reached!\n\nHalfway there! +50 Budget Coins earned! 🪙'), 1000);
            } else if (newPercentage >= 25 && oldPercentage < 25) {
                setTimeout(() => showCelebration('🎯'), 500);
                if (typeof coinSystem !== 'undefined') {
                    coinSystem.earnCoins(25, '25% milestone reached');
                }
                setTimeout(() => showSuccessNotification('🎯 25% Milestone Reached!\n\n+25 Budget Coins earned! 🪙'), 1000);
            } else {
                showSuccessNotification(`✅ Contribution of $${contribution.toFixed(2)} added successfully to ${currentGoal.name}!`);
            }
        }

        // Reload goals and close modal
        await loadGoals();
        closeContributeModal();
    } catch (error) {
        console.error('Error adding contribution:', error);
        showErrorNotification(`Failed to add contribution: ${error.message}`);
    }
});

// Celebration animation
function showCelebration(emoji) {
    const celebration = document.createElement('div');
    celebration.className = 'celebration';
    celebration.textContent = emoji;
    document.body.appendChild(celebration);

    setTimeout(() => {
        celebration.remove();
    }, 1000);
}

// Close modals when clicking outside
document.querySelectorAll('.modal').forEach(modal => {
    modal.addEventListener('click', function(e) {
        if (e.target === this) {
            this.classList.remove('active');
        }
    });
});

// Goal card interactions
document.querySelectorAll('.goal-card').forEach(card => {
    card.addEventListener('click', function(e) {
        if (!e.target.closest('.goal-actions') && !e.target.closest('.goal-actions-menu')) {
            const goalTitle = this.querySelector('.goal-title').textContent;
            console.log(`Viewing details for: ${goalTitle}`);
        }
    });
});

// Simulate progress updates
setInterval(() => {
    const progressBars = document.querySelectorAll('.goal-progress-fill');
    progressBars.forEach(bar => {
        bar.style.animation = 'shimmer 2s ease-in-out';
    });
}, 10000);

// Auto-save reminder
setInterval(() => {
    const nearCompleteGoals = document.querySelectorAll('.goal-card.near-complete');
    if (nearCompleteGoals.length > 0) {
        console.log('Reminder: You have goals close to completion!');
    }
}, 30000);

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
        document.querySelector('.sidebar').classList.remove('active');
    }
});

document.body.appendChild(menuToggle);

menuToggle.addEventListener('click', () => {
    document.querySelector('.sidebar').classList.toggle('active');
});

function initializeQuickAddGoal() {
    console.log('🚀 Initializing unified Quick Add Goal...');

    const quickAddForm = document.getElementById('quick-add-goal-form');
    if (!quickAddForm) return;

    quickAddForm.addEventListener('submit', async function (e) {
        e.preventDefault();
        console.log('📋 Unified Quick Add Goal submitted...');

        // Gather form values
        const name = document.getElementById('quick-goal-name').value.trim();
        const targetAmount = parseFloat(document.getElementById('quick-goal-target').value);
        const dueDate = document.getElementById('quick-goal-deadline').value;
        const description = document.getElementById('quick-goal-description').value.trim() || null;

        if (!name || !targetAmount || targetAmount <= 0 || !dueDate) {
            showErrorNotification('Please fill in all required fields, including a valid target date.');
            return;
        }

        const goalData = {
            name,
            targetAmount,
            dueDate, 
            description
        };

        // Validate via goalService if available
        const validation = goalService.validateGoalData
            ? goalService.validateGoalData(goalData)
            : { isValid: true };
        if (!validation.isValid) {
            showErrorNotification(validation.errors.join(', '));
            return;
        }

        const submitBtn = this.querySelector('button[type="submit"]');
        const originalText = submitBtn.textContent;
        submitBtn.textContent = '💾 Creating...';
        submitBtn.disabled = true;

        try {
            await goalService.createGoal(goalData);
            showCelebration('🎯');
            if (typeof coinSystem !== 'undefined') {
                coinSystem.earnCoins(50, 'New goal created');
            }
            showSuccessNotification(`✅ Goal "${name}" created successfully! +50 Budget Coins earned! 🪙`);
            this.reset();
            await loadGoals();
        } catch (error) {
            console.error('❌ Error creating goal:', error);
            showErrorNotification(`Failed to create goal: ${error.message}`);
        } finally {
            submitBtn.textContent = originalText;
            submitBtn.disabled = false;
        }
    });
}

// Initial load - fetch goals from API
document.addEventListener('DOMContentLoaded', async function() {
    await loadGoals();
    initializeQuickAddGoal();
});

// Refresh goals when page becomes visible
document.addEventListener('visibilitychange', function() {
    if (!document.hidden) {
        console.log('🔄 Page visible - refreshing goals...');
        loadGoals();
    }
});

// Refresh goals when window gains focus
window.addEventListener('focus', function() {
    console.log('🔄 Window focused - refreshing goals...');
    loadGoals();
});

// Load goals from API
async function loadGoals() {
    try {
        console.log('🎯 Loading goals from API...');
        allGoals = await goalService.getAllGoals();
        console.log(`✅ Loaded ${allGoals.length} goals`);

        renderGoals();
        updateGoalsSummary();
    } catch (error) {
        console.error('❌ Error loading goals:', error);
        showErrorNotification('Failed to load goals. Please refresh the page.');
    }
}

// Render goals in the active tab
function renderGoals() {
    const activeContainer = document.querySelector('#active-tab .goals-grid');
    const completedContainer = document.querySelector('#completed-tab .goals-grid');

    if (!activeContainer || !completedContainer) return;

    // Separate active and completed goals
    const activeGoals = allGoals.filter(g => g.status !== 'Achieved');
    const completedGoals = allGoals.filter(g => g.status === 'Achieved');

    // Render active goals
    if (activeGoals.length === 0) {
        activeContainer.innerHTML = `
            <div style="text-align: center; padding: 3rem; color: var(--gray); grid-column: 1/-1;">
                <div style="font-size: 3rem; margin-bottom: 1rem;">🎯</div>
                <h3>No active goals yet</h3>
                <p>Create your first savings goal to get started!</p>
            </div>
        `;
    } else {
        activeContainer.innerHTML = activeGoals.map(goal => createGoalCard(goal)).join('');
    }

    // Render completed goals
    if (completedGoals.length === 0) {
        completedContainer.innerHTML = `
            <div style="text-align: center; padding: 3rem; color: var(--gray); grid-column: 1/-1;">
                <div style="font-size: 3rem; margin-bottom: 1rem;">✅</div>
                <h3>No completed goals yet</h3>
                <p>Keep saving to achieve your goals!</p>
            </div>
        `;
    } else {
        completedContainer.innerHTML = completedGoals.map(goal => createGoalCard(goal)).join('');
    }
}

// Create HTML for a goal card
function createGoalCard(goal) {
    const progress = goal.progress || 0;
    const current = goal.currentAmount || 0;
    const target = goal.targetAmount || 0;
    const remaining = target - current;
    const isNearComplete = progress >= 75 && goal.status !== 'Achieved';
    const isCompleted = goal.status === 'Achieved';

    // Determine progress class
    let progressClass = 'low';
    if (progress >= 75) progressClass = 'high';
    else if (progress >= 50) progressClass = 'medium';
    if (isCompleted) progressClass = 'complete';

    // Format date if available
    const dueDateText = goal.dueDate ? new Date(goal.dueDate).toLocaleDateString('en-US', {
        month: 'short', day: 'numeric', year: 'numeric'
    }) : '';

    return `
        <div class="goal-card ${isNearComplete ? 'near-complete' : ''} ${isCompleted ? 'completed' : ''}">
            <div class="goal-header">
                <div class="goal-info">
                    <div class="goal-icon">🎯</div>
                    <div class="goal-title">${goal.name}</div>
                    <div class="goal-description">${dueDateText ? 'Target: ' + dueDateText : ''}</div>
                </div>
                ${!isCompleted ? `<div class="goal-actions-menu">
                    <button class="goal-menu-btn" onclick="showGoalOptions(${goal.id})">⋮</button>
                </div>` : ''}
            </div>

            <div class="goal-progress-section">
                <div class="goal-amounts">
                    <span class="current-amount">$${current.toLocaleString('en-US', {minimumFractionDigits: 0, maximumFractionDigits: 0})}</span>
                    <span class="target-amount">of $${target.toLocaleString('en-US', {minimumFractionDigits: 0, maximumFractionDigits: 0})}</span>
                </div>
                <div class="goal-progress-bar">
                    <div class="goal-progress-fill ${progressClass}" style="width: ${progress}%"></div>
                </div>
                <div class="progress-stats">
                    <span class="progress-percentage">${isCompleted ? '✅ COMPLETED' : progress.toFixed(1) + '% complete'}</span>
                    <span class="remaining-amount">${isCompleted ? '' : '$' + remaining.toLocaleString('en-US', {minimumFractionDigits: 0, maximumFractionDigits: 0}) + ' to go'}</span>
                </div>
            </div>

            ${isNearComplete && !isCompleted ? `<div class="achievement-badge">
                🔥 Almost there! Just $${remaining.toLocaleString('en-US', {minimumFractionDigits: 0, maximumFractionDigits: 0})} more
            </div>` : ''}

            ${isCompleted ? `<div class="achievement-badge">
                🏆 Goal Achieved!
            </div>` : ''}

            ${!isCompleted ? `<div class="goal-actions">
                <button class="action-btn contribute-btn" onclick="openContributeModal('${goal.name}', ${current}, ${target}, ${goal.id})">
                    💰 Contribute
                </button>
                <button class="action-btn edit-btn" onclick="editGoal(${goal.id})">
                    ✏️ Edit
                </button>
            </div>` : ''}
        </div>
    `;
}

// Update goals summary cards
function updateGoalsSummary() {
    const activeGoals = allGoals.filter(g => g.status !== 'Achieved');
    const totalTarget = allGoals.reduce((sum, g) => sum + (g.targetAmount || 0), 0);
    const totalSaved = allGoals.reduce((sum, g) => sum + (g.currentAmount || 0), 0);
    const totalRemaining = totalTarget - totalSaved;

    document.querySelector('.summary-card.total .summary-value').textContent =
        `$${totalTarget.toLocaleString('en-US', {minimumFractionDigits: 0, maximumFractionDigits: 0})}`;
    document.querySelector('.summary-card.saved .summary-value').textContent =
        `$${totalSaved.toLocaleString('en-US', {minimumFractionDigits: 0, maximumFractionDigits: 0})}`;
    document.querySelector('.summary-card.remaining .summary-value').textContent =
        `$${totalRemaining.toLocaleString('en-US', {minimumFractionDigits: 0, maximumFractionDigits: 0})}`;
    document.querySelector('.summary-card.active .summary-value').textContent = activeGoals.length;
}

// Smart notifications
function checkGoalMilestones() {
    // This would typically run after each contribution
    // Check if user has reached 25%, 50%, 75%, or 100% milestones
    console.log('Checking for milestone achievements...');
}

// Goal insights
setInterval(() => {
    console.log('💡 Tip: Add $100 to your Emergency Fund this week to stay on track for your 2-month completion goal!');
}, 60000); // Show tips every minute for demo
