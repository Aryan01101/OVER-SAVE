// Navigate to Rewards Shop
function navigateToRewardsShop() {
    window.location.href = 'shopping_page.html';
}

// FR-11 Budget Setting Modal Functions
function openCreateModal() {
    console.log('🎯 Opening budget creation modal...');
    const modal = document.getElementById('budgetModal');
    if (modal) {
        modal.style.display = 'flex';
        modal.classList.add('active');

        // Reset modal title for new budget creation
        const modalTitle = document.querySelector('.modal-title');
        if (modalTitle) {
            modalTitle.textContent = 'Create New Budget';
        }

        // Reset editing state
        editingBudget = null;

        // Reset and enable category dropdown for new budget creation
        const categorySelect = document.getElementById('modal-category');
        if (categorySelect) {
            categorySelect.disabled = false; // Enable category selection for new budgets
            categorySelect.value = ''; // Reset selection
        }

        // Reset amount field
        const amountInput = document.getElementById('modal-amount');
        if (amountInput) {
            amountInput.value = '';
        }

        // Reset custom name field
        const nameInput = document.getElementById('modal-budget-name');
        if (nameInput) {
            nameInput.value = '';
        }

        // Hide suggestions initially
        const suggestions = document.getElementById('name-suggestions');
        if (suggestions) {
            suggestions.style.display = 'none';
        }

        // Load categories when modal opens
        budgetService.loadCategories();

        // Set up category change listener for suggestions
        setupCategoryChangeListener();

        // Focus on category selection
        setTimeout(() => {
            if (categorySelect) categorySelect.focus();
        }, 300);
    }
}

function closeBudgetModal() {
    console.log('❌ Closing budget modal...');
    const modal = document.getElementById('budgetModal');
    if (modal) {
        modal.style.display = 'none';
        modal.classList.remove('active');

        // Reset form
        const form = document.getElementById('modal-budget-form');
        if (form) form.reset();

        // Reset editing state and enable category dropdown
        editingBudget = null;
        const categorySelect = document.getElementById('modal-category');
        if (categorySelect) {
            categorySelect.disabled = false;
        }
    }
}

let editingBudget = null;

// Budget Name Suggestions System
const budgetNameSuggestions = {
    'Food': [
        'Weekly Groceries', 'Daily Meals', 'Dining Out', 'Coffee & Snacks',
        'Lunch Money', 'Grocery Shopping', 'Food Essentials'
    ],
    'Transport': [
        'Monthly Transit', 'Uber & Taxi', 'Fuel Budget', 'Public Transport',
        'Daily Commute', 'Travel Expenses', 'Car Maintenance'
    ],
    'Entertainment': [
        'Weekend Fun', 'Movies & Shows', 'Gaming Budget', 'Social Activities',
        'Streaming Services', 'Concerts & Events', 'Hobby Spending'
    ],
    'Education': [
        'Study Materials', 'Course Fees', 'Books & Supplies', 'Online Learning',
        'Professional Development', 'University Expenses', 'Skill Building'
    ],
    'Health': [
        'Medical Expenses', 'Pharmacy Bills', 'Gym Membership', 'Wellness',
        'Doctor Visits', 'Health Insurance', 'Vitamins & Supplements'
    ],
    'Shopping': [
        'Clothing Budget', 'Personal Items', 'Household Goods', 'Tech & Gadgets',
        'Monthly Shopping', 'Essentials Only', 'Seasonal Shopping'
    ],
    'Personal': [
        'Personal Care', 'Miscellaneous', 'Emergency Fund', 'Savings Goal',
        'Rainy Day Fund', 'Personal Projects', 'Self Care'
    ],
    'Fitness': [
        'Gym & Fitness', 'Sports Equipment', 'Workout Gear', 'Training Classes',
        'Fitness Goals', 'Active Lifestyle', 'Health & Fitness'
    ],
    'Travel': [
        'Vacation Fund', 'Weekend Trips', 'Travel Adventures', 'Holiday Budget',
        'Getaway Money', 'Exploration Fund', 'Trip Planning'
    ]
};

function setupCategoryChangeListener() {
    const categorySelect = document.getElementById('modal-category');
    if (!categorySelect) return;

    // Remove existing listeners
    categorySelect.removeEventListener('change', handleCategoryChange);
    // Add new listener
    categorySelect.addEventListener('change', handleCategoryChange);
}

function handleCategoryChange(event) {
    const selectedCategoryId = event.target.value;
    if (!selectedCategoryId) {
        hideSuggestions();
        return;
    }

    // Find category name from the selected option
    const selectedOption = event.target.selectedOptions[0];
    const categoryName = selectedOption.textContent;

    showSuggestionsForCategory(categoryName);
}

function showSuggestionsForCategory(categoryName) {
    const suggestions = budgetNameSuggestions[categoryName] || [];
    const suggestionsContainer = document.getElementById('name-suggestions');
    const buttonsContainer = document.getElementById('suggestion-buttons');

    if (!suggestionsContainer || !buttonsContainer) return;

    if (suggestions.length === 0) {
        hideSuggestions();
        return;
    }

    // Clear existing suggestions
    buttonsContainer.innerHTML = '';

    // Create suggestion buttons
    suggestions.forEach(suggestion => {
        const button = document.createElement('button');
        button.type = 'button';
        button.className = 'suggestion-btn';
        button.textContent = suggestion;
        button.onclick = () => applySuggestion(suggestion);
        buttonsContainer.appendChild(button);
    });

    // Show suggestions container
    suggestionsContainer.style.display = 'block';
}

function hideSuggestions() {
    const suggestionsContainer = document.getElementById('name-suggestions');
    if (suggestionsContainer) {
        suggestionsContainer.style.display = 'none';
    }
}

function applySuggestion(suggestionText) {
    const nameInput = document.getElementById('modal-budget-name');
    if (nameInput) {
        nameInput.value = suggestionText;
        nameInput.focus();

        // Add a subtle animation to show the suggestion was applied
        nameInput.style.background = '#e0f2fe';
        setTimeout(() => {
            nameInput.style.background = '';
        }, 800);
    }
}

// Load budgets from backend on page load
document.addEventListener('DOMContentLoaded', function() {
    loadBudgets();
    loadBudgetSummary();
    initializeQuickAddBudget();
});

// Load and display all budgets
async function loadBudgets() {
    console.log('📊 Loading budgets from backend...');
    const container = document.getElementById('budgets-container');
    const alertsContainer = document.getElementById('budget-alerts-container');

    if (!container) {
        console.error('❌ Budgets container not found');
        return;
    }

    try {
        // Load all categories first
        const categories = await budgetService.loadCategories();

        if (!categories || categories.length === 0) {
            container.innerHTML = `
                <div class="empty-state" style="text-align: center; padding: 3rem; color: #6b7280;">
                    <div style="font-size: 3rem; margin-bottom: 1rem;">📋</div>
                    <h3>No budgets set yet</h3>
                    <p>Create your first budget using the form below to start tracking your spending.</p>
                </div>
            `;
            return;
        }

        // Load budget summaries for each category
        const budgetData = [];
        for (const category of categories) {
            try {
                const summary = await budgetService.getBudgetSummary(category.categoryId);
                if (summary && summary.budget > 0) {
                    budgetData.push({
                        ...category,
                        summary
                    });
                }
            } catch (error) {
                console.warn(`⚠️ Could not load budget for category ${category.name}:`, error);
            }
        }

        // Display budgets or empty state
        if (budgetData.length === 0) {
            container.innerHTML = `
                <div class="empty-state" style="text-align: center; padding: 3rem; color: #6b7280;">
                    <div style="font-size: 3rem; margin-bottom: 1rem;">💰</div>
                    <h3>No budgets configured</h3>
                    <p>Create your first budget using the form below to start tracking your spending.</p>
                </div>
            `;
            alertsContainer.innerHTML = '';
        } else {
            // Render budget items
            container.innerHTML = budgetData.map(item => renderBudgetItem(item)).join('');

            // Generate alerts based on actual data
            generateBudgetAlerts(budgetData, alertsContainer);
        }

    } catch (error) {
        console.error('❌ Error loading budgets:', error);
        container.innerHTML = `
            <div style="text-align: center; padding: 3rem; color: #ef4444;">
                ❌ Error loading budget data.<br>
                <button onclick="loadBudgets()" style="color: var(--primary); background: none; border: none; cursor: pointer;">Try again</button>
            </div>
        `;
    }
}

// Render individual budget item
function renderBudgetItem(item) {
    console.log('🎯 renderBudgetItem received:', item);
    console.log('📋 summary data:', item.summary);

    const { name, categoryId, summary } = item;
    const spent = summary.spent || 0;
    const budget = summary.budget;
    const customName = summary.customName || null; // Get custom name from backend
    const percentage = budget > 0 ? Math.round((spent / budget) * 100) : 0;

    console.log(`🏷️ Category: ${name}, Custom Name: ${customName}, Display Name: ${customName || name}`);
    const remaining = budget - spent;

    // Determine progress fill class and warning text
    let progressClass = 'safe';
    let warningText = '';
    if (percentage >= 100) {
        progressClass = 'danger';
        warningText = '🔥 Over budget!';
    } else if (percentage >= 90) {
        progressClass = 'danger';
        warningText = '🔥 Over limit risk';
    } else if (percentage >= 80) {
        progressClass = 'warning';
        warningText = '⚠️ High usage';
    }

    // Category icon mapping
    const iconMap = {
        'Food': '🍔', 'Transport': '🚗', 'Entertainment': '🎮',
        'Housing': '🏠', 'Education': '📚', 'Health': '🏥',
        'Shopping': '🛍️', 'Utilities': '💡', 'Groceries': '🛒',
        'Personal': '👤', 'Fitness': '💪', 'Travel': '✈️'
    };
    const icon = iconMap[name] || '💰';

    // Background color based on usage
    let bgColor = '#d1fae5'; // Green
    if (percentage >= 80) bgColor = '#fee2e2'; // Red
    else if (percentage >= 60) bgColor = '#fef3c7'; // Yellow
    else if (percentage >= 40) bgColor = '#e0e7ff'; // Blue

    // Display name: custom name if exists, otherwise category name
    const displayName = customName || name;
    const categoryInfo = customName ? `${name} • ${customName}` : name;

    return `
        <div class="budget-item">
            <div class="budget-icon" style="background: ${bgColor};">${icon}</div>
            <div class="budget-details">
                <div class="budget-category">${displayName}</div>
                <div class="budget-amounts">$${spent.toFixed(2)} spent of $${budget.toFixed(2)} budget</div>
                <div class="progress-bar">
                    <div class="progress-fill ${progressClass}" style="width: ${Math.min(percentage, 100)}%"></div>
                </div>
                <div class="progress-text">
                    ${percentage}% used • $${remaining.toFixed(2)} remaining${warningText ? ' • ' + warningText : ''}
                </div>
            </div>
            <div class="budget-actions">
                <button class="action-btn edit-btn" onclick="editBudget('${categoryId}', ${budget}, '${customName || ''}')">✏️ Edit</button>
                <button class="action-btn delete-btn" onclick="deleteBudget('${categoryId}')">🗑️ Delete</button>
            </div>
        </div>
    `;
}

// Generate dynamic budget alerts
function generateBudgetAlerts(budgetData, container) {
    const alerts = [];

    budgetData.forEach(item => {
        const { name, summary } = item;
        const spent = summary.spent || 0;
        const budget = summary.budget;
        const customName = summary.customName || null;
        const displayName = customName || name; // Use custom name if available
        const percentage = budget > 0 ? Math.round((spent / budget) * 100) : 0;

        if (percentage >= 100) {
            alerts.push({
                type: 'danger',
                icon: '⚠️',
                message: `<strong>${displayName} budget exceeded!</strong> You've spent $${spent.toFixed(2)} of $${budget.toFixed(2)} (${percentage}%). Consider reducing spending for the rest of the month.`
            });
        } else if (percentage >= 80) {
            alerts.push({
                type: 'warning',
                icon: '🚨',
                message: `<strong>${displayName} budget warning:</strong> You're at ${percentage}% ($${spent.toFixed(2)}/$${budget.toFixed(2)}). Slow down to stay on track.`
            });
        }
    });

    container.innerHTML = alerts.map(alert => `
        <div class="alert ${alert.type}">
            <div class="alert-icon">${alert.icon}</div>
            <div class="alert-text">${alert.message}</div>
        </div>
    `).join('');
}

// FR-11 Modal Form Submission Handler
document.addEventListener('DOMContentLoaded', function() {
    const modalForm = document.getElementById('modal-budget-form');
    if (modalForm) {
        modalForm.addEventListener('submit', async function(e) {
            e.preventDefault();
            console.log('📋 FR-11 Budget form submitted...');

            // FR-11: Get form data (category + amount + custom name)
            const formData = new FormData(this);
            const budgetData = {
                categoryId: document.getElementById('modal-category').value,
                amount: document.getElementById('modal-amount').value,
                customName: document.getElementById('modal-budget-name').value || null
            };

            console.log('📊 Budget data:', budgetData);

            // Show loading state
            const submitBtn = this.querySelector('button[type="submit"]');
            const originalText = submitBtn.textContent;
            submitBtn.textContent = '💾 Saving...';
            submitBtn.disabled = true;

            try {
                // FR-11: System validates and saves the budget
                const result = await budgetService.setBudget(budgetData);

                if (result.success) {
                    // Award coins only for budget creation, not editing
                    if (typeof coinSystem !== 'undefined' && !editingBudget) {
                        coinSystem.earnFromBudgetGoal();
                    }

                    // Now close modal (this resets editingBudget to null)
                    closeBudgetModal();

                    // Reset editing state
                    editingBudget = null;

                    setTimeout(() => {
                        loadBudgets();
                        loadBudgetSummary();
                    }, 500);
                }
            } catch (error) {
                console.error('❌ Error in budget submission:', error);
            } finally {
                // Reset button state
                submitBtn.textContent = originalText;
                submitBtn.disabled = false;
            }
        });
    }

    // Close modal when clicking outside
    const modal = document.getElementById('budgetModal');
    if (modal) {
        modal.addEventListener('click', function(e) {
            if (e.target === this) {
                closeBudgetModal();
            }
        });
    }
});

// Duplicate modal functions removed - using FR-11 versions above

async function editBudget(categoryId, currentAmount, customName = null) {
    console.log(`🖉 Editing budget for category ${categoryId}, current amount: ${currentAmount}, custom name: ${customName}`);

    editingBudget = categoryId;

    // Open modal and load categories first
    const modal = document.getElementById('budgetModal');
    if (modal) {
        modal.style.display = 'flex';
        modal.classList.add('active');

        // Set modal title for editing
        const modalTitle = document.querySelector('.modal-title');
        if (modalTitle) {
            modalTitle.textContent = 'Edit Budget';
        }

        // Load categories and wait for completion
        await budgetService.loadCategories();

        // Pre-select the category and disable it (can't change category when editing)
        const categorySelect = document.getElementById('modal-category');
        if (categorySelect) {
            categorySelect.value = categoryId;
            categorySelect.disabled = true; // Disable category selection when editing
        }

        // Pre-fill the amount
        const amountInput = document.getElementById('modal-amount');
        if (amountInput) {
            amountInput.value = currentAmount;
            amountInput.focus(); // Focus on amount since category is already selected
        }

        // Pre-fill the custom name if it exists
        const nameInput = document.getElementById('modal-budget-name');
        if (nameInput) {
            nameInput.value = customName || '';
        }

        // Hide suggestions when editing (category can't be changed)
        hideSuggestions();
    }
}

async function deleteBudget(categoryId) {
    // Get category name for better user experience
    const categories = await budgetService.loadCategories();
    const category = categories.find(cat => cat.categoryId == categoryId);
    const categoryName = category ? category.name : `Category ${categoryId}`;

    if (confirm(`Are you sure you want to delete the budget for ${categoryName}? This action cannot be undone.`)) {
        console.log(`Deleting budget for: ${categoryName} (ID: ${categoryId})`);

        try {
            const result = await budgetService.deleteBudget(categoryId);

            if (result.success) {
                // Refresh the budget list and summary after deletion
                setTimeout(() => {
                    loadBudgets();
                    loadBudgetSummary();
                }, 500);
            }
        } catch (error) {
            console.error('❌ Error deleting budget:', error);
        }
    }
}


// Template application
function applyTemplate(templateType) {
    const templates = {
        student: {
            total: '$1,800',
            categories: ['Food: $400', 'Transport: $150', 'Entertainment: $100', 'Education: $200', 'Personal: $100', 'Health: $50']
        },
        conservative: {
            total: '$2,000',
            categories: ['Food: $500', 'Transport: $200', 'Entertainment: $150', 'Shopping: $100', 'Health: $100', 'Personal: $100']
        },
        balanced: {
            total: '$2,500',
            categories: ['Food: $600', 'Transport: $300', 'Entertainment: $200', 'Shopping: $200', 'Health: $150', 'Personal: $150']
        },
        flexible: {
            total: '$3,000',
            categories: ['Food: $750', 'Transport: $400', 'Entertainment: $300', 'Shopping: $300', 'Health: $200', 'Personal: $200']
        }
    };

    const template = templates[templateType];
    if (confirm(`Apply ${templateType} template?\n\nTotal Budget: ${template.total}\n\nCategories:\n${template.categories.join('\n')}\n\nThis will replace your current budgets.`)) {
        console.log(`Applying ${templateType} template`);
        alert(`✅ ${templateType.charAt(0).toUpperCase() + templateType.slice(1)} template applied successfully!`);
    }
}

function copyLastMonth() {
    if (confirm('Copy all budget amounts from October 2024?\n\nThis will overwrite current budget amounts for existing categories.')) {
        console.log('Copying last month budgets');
        alert('✅ Last month\'s budgets copied successfully!');
    }
}


// Real-time budget alerts simulation
setInterval(() => {
    const alerts = document.querySelectorAll('.alert');
    if (alerts.length > 0) {
        const randomAlert = alerts[Math.floor(Math.random() * alerts.length)];
        randomAlert.style.animation = 'pulse 0.5s';
        setTimeout(() => {
            randomAlert.style.animation = '';
        }, 500);
    }
}, 10000);

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

// Load and calculate budget summary statistics
async function loadBudgetSummary() {
    console.log('📊 Loading budget summary...');

    try {
        // Load categories and their budget data
        const categories = await budgetService.loadCategories();

        let totalBudget = 0;
        let totalSpent = 0;
        let activeBudgets = 0;

        // Calculate totals from all categories with budgets
        for (const category of categories) {
            try {
                const summary = await budgetService.getBudgetSummary(category.categoryId);
                if (summary && summary.budget > 0) {
                    totalBudget += summary.budget;
                    totalSpent += summary.spent || 0;
                    activeBudgets++;
                }
            } catch (error) {
                console.warn(`⚠️ Could not load budget for category ${category.name}:`, error);
            }
        }

        const totalRemaining = totalBudget - totalSpent;

        // Update the summary UI
        updateBudgetSummaryUI(totalBudget, totalSpent, totalRemaining, activeBudgets);

    } catch (error) {
        console.error('❌ Error loading budget summary:', error);
        // Keep zeros if there's an error
        updateBudgetSummaryUI(0, 0, 0, 0);
    }
}

// Update budget summary UI elements
function updateBudgetSummaryUI(totalBudget, totalSpent, totalRemaining, activeBudgets) {
    const formatCurrency = (amount) => `$${amount.toFixed(2)}`;

    // Update summary cards
    const totalBudgetElement = document.getElementById('total-budget');
    const amountSpentElement = document.getElementById('amount-spent');
    const amountRemainingElement = document.getElementById('amount-remaining');
    const activeBudgetsElement = document.getElementById('active-budgets');

    if (totalBudgetElement) totalBudgetElement.textContent = formatCurrency(totalBudget);
    if (amountSpentElement) amountSpentElement.textContent = formatCurrency(totalSpent);
    if (amountRemainingElement) amountRemainingElement.textContent = formatCurrency(totalRemaining);
    if (activeBudgetsElement) activeBudgetsElement.textContent = activeBudgets.toString();

    console.log('✅ Budget summary updated:', { totalBudget, totalSpent, totalRemaining, activeBudgets });
}

// Animation styles
const style = document.createElement('style');
style.textContent = `
    @keyframes pulse {
        0% { transform: scale(1); }
        50% { transform: scale(1.05); }
        100% { transform: scale(1); }
    }
`;
document.head.appendChild(style);

// Initialize Quick Add Budget functionality
async function initializeQuickAddBudget() {
    console.log('🚀 Initializing Quick Add Budget...');

    // Load categories for the quick add form
    const quickCategorySelect = document.getElementById('quick-budget-category');
    if (quickCategorySelect) {
        try {
            const categories = await budgetService.loadCategories();

            // Clear existing options except the first one
            quickCategorySelect.innerHTML = '<option value="">Select a category...</option>';

            // Populate with categories
            categories.forEach(category => {
                const option = document.createElement('option');
                option.value = category.categoryId;
                option.textContent = category.name;
                quickCategorySelect.appendChild(option);
            });

            console.log('✅ Categories loaded for Quick Add Budget');
        } catch (error) {
            console.error('❌ Error loading categories for Quick Add:', error);
        }
    }

    // Handle quick add form submission
    const quickAddForm = document.getElementById('quick-add-budget-form');
    if (quickAddForm) {
        quickAddForm.addEventListener('submit', async function(e) {
            e.preventDefault();
            console.log('📋 Quick Add Budget form submitted...');

            // Get form data
            const budgetData = {
                categoryId: document.getElementById('quick-budget-category').value,
                amount: document.getElementById('quick-budget-amount').value,
                customName: document.getElementById('quick-budget-name').value || null
            };

            console.log('📊 Quick Budget data:', budgetData);

            // Show loading state
            const submitBtn = this.querySelector('button[type="submit"]');
            const originalText = submitBtn.textContent;
            submitBtn.textContent = '💾 Saving...';
            submitBtn.disabled = true;

            try {
                // Submit budget
                const result = await budgetService.setBudget(budgetData);

                if (result.success) {
                    // Reset form
                    this.reset();

                    // Award coins for budget creation
                    if (typeof coinSystem !== 'undefined') {
                        coinSystem.earnFromBudgetGoal();
                    }

                    // Refresh budget display
                    setTimeout(() => {
                        loadBudgets();
                        loadBudgetSummary();
                    }, 500);

                    console.log('✅ Quick Budget set successfully');

                    // Show success feedback
                    submitBtn.textContent = '✅ Saved!';
                    setTimeout(() => {
                        submitBtn.textContent = originalText;
                    }, 2000);
                }
            } catch (error) {
                console.error('❌ Error in quick budget submission:', error);
            } finally {
                // Reset button state
                submitBtn.disabled = false;
            }
        });
    }
}