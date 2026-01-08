let selectedCategory = null;
window.currentTransactionType = window.currentTransactionType || 'expense';
let categoryCache = null;
let manageCategoriesData = [];
let categorySuggestions = [];
let categorySearchInitialized = false;
let deleteCategoryId = null;
let mergeSourceCategoryIds = new Set();
let mergeTargetCategoryId = null;

const categorySearchConfigs = [
    {
        inputId: 'category-search',
        dropdownId: 'category-dropdown',
        clearBtnId: 'category-clear',
        hiddenInputId: 'selected-category-id',
        onSelect(category) {
            selectedCategory = category.name;
            const hidden = document.getElementById('selected-category-id');
            if (hidden) {
                hidden.value = category.id;
            }
        },
        onClear() {
            selectedCategory = null;
            const hidden = document.getElementById('selected-category-id');
            if (hidden) {
                hidden.value = '';
            }
        }
    },
    {
        inputId: 'modal-category-search',
        dropdownId: 'modal-category-dropdown',
        clearBtnId: 'modal-category-clear',
        hiddenInputId: 'modal-selected-category-id',
        onSelect(category) {
            const hidden = document.getElementById('modal-selected-category-id');
            if (hidden) {
                hidden.value = category.id;
            }
        },
        onClear() {
            const hidden = document.getElementById('modal-selected-category-id');
            if (hidden) {
                hidden.value = '';
            }
        }
    }
];

function getModalElement(id) {
    const modal = document.getElementById(id);
    if (!modal) {
        console.warn(`Modal with id "${id}" not found.`);
    }
    return modal;
}

function setModalVisibility(id, isVisible) {
    const modal = getModalElement(id);
    if (!modal) {
        return;
    }
    modal.style.display = isVisible ? 'flex' : 'none';
    if (!isVisible) {
        modal.scrollTop = 0;
    }
}

function escapeHtml(str) {
    if (typeof str !== 'string') {
        return '';
    }
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
}

function invalidateCategoryCache() {
    categoryCache = null;
    categorySuggestions = [];
}

async function ensureCategorySuggestions(forceReload = false) {
    if (forceReload) {
        invalidateCategoryCache();
    }
    if (!Array.isArray(categorySuggestions) || categorySuggestions.length === 0) {
        const categories = await loadCategories();
        categorySuggestions = Array.isArray(categories) ? [...categories] : [];
    }
    return categorySuggestions;
}

function filterCategoriesList(categories, searchTerm) {
    const normalized = (searchTerm || '').toLowerCase().trim();
    if (!normalized) {
        return categories;
    }
    return categories.filter(category => {
        return category && category.name && category.name.toLowerCase().includes(normalized);
    });
}

function renderCategoryDropdown(dropdown, categories, config) {
    if (!dropdown) {
        return;
    }

    dropdown.innerHTML = '';

    if (!categories || categories.length === 0) {
        dropdown.innerHTML = '<div class="category-no-results">No categories found</div>';
        return;
    }

    const hiddenValue = config.hiddenInputId
        ? (document.getElementById(config.hiddenInputId)?.value || '')
        : '';

    categories.forEach(category => {
        const item = document.createElement('div');
        item.className = 'category-list-item';
        item.dataset.categoryId = category.id;
        const label = `${category.system ? '🔒 ' : ''}${escapeHtml(category.name || '')}`;
        item.innerHTML = label;

        if (hiddenValue && String(hiddenValue) === String(category.id)) {
            item.classList.add('selected');
        }

        item.addEventListener('click', () => handleCategorySelection(config, category));
        dropdown.appendChild(item);
    });
}

function handleCategorySelection(config, category) {
    if (!category) {
        return;
    }

    if (typeof config.onSelect === 'function') {
        config.onSelect(category);
    } else if (config.hiddenInputId) {
        const hidden = document.getElementById(config.hiddenInputId);
        if (hidden) {
            hidden.value = category.id;
        }
    }

    const input = document.getElementById(config.inputId);
    const dropdown = document.getElementById(config.dropdownId);

    if (input) {
        input.value = category.name || '';
        input.classList.add('has-value');
    }
    if (dropdown) {
        dropdown.classList.remove('show');
    }
}

async function populateCategorySearchOptions(forceReload = false, categoriesOverride = null) {
    try {
        const categories = categoriesOverride || await ensureCategorySuggestions(forceReload);
        categorySearchConfigs.forEach(config => {
            const dropdown = document.getElementById(config.dropdownId);
            if (!dropdown) {
                return;
            }
            const input = document.getElementById(config.inputId);
            const term = input ? input.value : '';
            const filtered = filterCategoriesList(categories, term);
            renderCategoryDropdown(dropdown, filtered, config);
        });
    } catch (error) {
        console.error('Error populating category selectors:', error);
    }
}

function initializeCategorySearchInputs() {
    if (categorySearchInitialized) {
        return;
    }
    categorySearchInitialized = true;

    categorySearchConfigs.forEach(config => {
        const input = document.getElementById(config.inputId);
        const dropdown = document.getElementById(config.dropdownId);
        const clearBtn = config.clearBtnId ? document.getElementById(config.clearBtnId) : null;

        if (!input || !dropdown) {
            return;
        }

        input.addEventListener('focus', async () => {
            try {
                const categories = await ensureCategorySuggestions();
                renderCategoryDropdown(dropdown, categories, config);
                dropdown.classList.add('show');
            } catch (error) {
                console.error('Error preparing categories for selector focus:', error);
            }
        });

        input.addEventListener('input', async event => {
            const value = event.target.value;
            input.classList.toggle('has-value', value.trim().length > 0);
            try {
                const categories = await ensureCategorySuggestions();
                const filtered = filterCategoriesList(categories, value);
                renderCategoryDropdown(dropdown, filtered, config);
                dropdown.classList.add('show');
            } catch (error) {
                console.error('Error filtering categories:', error);
            }
        });

        input.addEventListener('keydown', event => {
            if (event.key === 'Escape') {
                dropdown.classList.remove('show');
                input.blur();
            }
        });

        if (clearBtn) {
            clearBtn.addEventListener('click', async () => {
                input.value = '';
                input.classList.remove('has-value');
                if (config.hiddenInputId) {
                    const hidden = document.getElementById(config.hiddenInputId);
                    if (hidden) {
                        hidden.value = '';
                    }
                }
                if (typeof config.onClear === 'function') {
                    config.onClear();
                }
                try {
                    const categories = await ensureCategorySuggestions();
                    renderCategoryDropdown(dropdown, categories, config);
                } catch (error) {
                    console.error('Error resetting category selector:', error);
                }
                dropdown.classList.remove('show');
                input.focus();
            });
        }
    });

    document.addEventListener('click', event => {
        categorySearchConfigs.forEach(config => {
            const input = document.getElementById(config.inputId);
            const dropdown = document.getElementById(config.dropdownId);
            if (!input || !dropdown) {
                return;
            }
            const container = input.closest('.category-search-container');
            if (container && !container.contains(event.target)) {
                dropdown.classList.remove('show');
            }
        });
    });
}

function renderManageCategoriesLists() {
    const userList = document.getElementById('categories-list');
    const systemList = document.getElementById('system-categories-list');

    if (!userList || !systemList) {
        return;
    }

    const userCategories = manageCategoriesData.filter(cat => !cat.system);
    const systemCategories = manageCategoriesData.filter(cat => cat.system);

    userList.innerHTML = '';
    if (userCategories.length === 0) {
        const placeholder = document.createElement('div');
        placeholder.style.cssText = 'text-align: center; color: #94a3b8; padding: 2rem;';
        placeholder.textContent = 'No custom categories yet. Create one to get started!';
        userList.appendChild(placeholder);
    } else {
        userCategories.forEach(category => {
            const item = document.createElement('div');
            item.className = 'category-item';
            item.style.cssText = 'display: flex; justify-content: space-between; align-items: center; padding: 0.85rem 1rem; border: 1px solid #e2e8f0; border-radius: 0.75rem; background: #f8fafc; margin-bottom: 0.75rem;';

            const name = document.createElement('div');
            name.style.cssText = 'font-weight: 600; color: #334155;';
            name.textContent = category.name;
            item.appendChild(name);

            const actions = document.createElement('div');
            actions.style.cssText = 'display: flex; gap: 0.5rem;';

            const renameBtn = document.createElement('button');
            renameBtn.type = 'button';
            renameBtn.className = 'btn btn-secondary';
            renameBtn.style.cssText = 'padding: 0.4rem 0.8rem; font-size: 0.75rem;';
            renameBtn.textContent = 'Rename';
            renameBtn.addEventListener('click', () => window.openEditCategoryModal(category.id));

            const deleteBtn = document.createElement('button');
            deleteBtn.type = 'button';
            deleteBtn.className = 'btn btn-danger';
            deleteBtn.style.cssText = 'padding: 0.4rem 0.8rem; font-size: 0.75rem;';
            deleteBtn.textContent = 'Delete';
            deleteBtn.addEventListener('click', () => window.openDeleteCategoryModal(category.id));

            actions.appendChild(renameBtn);
            actions.appendChild(deleteBtn);
            item.appendChild(actions);
            userList.appendChild(item);
        });
    }

    systemList.innerHTML = '';
    if (systemCategories.length === 0) {
        const placeholder = document.createElement('div');
        placeholder.style.cssText = 'text-align: center; color: #94a3b8; padding: 1rem;';
        placeholder.textContent = 'No system categories available.';
        systemList.appendChild(placeholder);
    } else {
        systemCategories.forEach(category => {
            const item = document.createElement('div');
            item.style.cssText = 'display: flex; align-items: center; gap: 0.5rem; padding: 0.6rem 0.75rem; border: 1px dashed #cbd5f5; border-radius: 0.65rem; background: #eff6ff; color: #1e3a8a; margin-bottom: 0.5rem; font-size: 0.85rem;';
            item.innerHTML = `🔒 ${escapeHtml(category.name)}`;
            systemList.appendChild(item);
        });
    }
}

async function refreshManageCategories(forceReload = false) {
    const userList = document.getElementById('categories-list');
    if (userList) {
        userList.innerHTML = '<div style="text-align: center; padding: 2rem; color: #94a3b8;">Loading categories...</div>';
    }

    if (forceReload) {
        invalidateCategoryCache();
    }

    try {
        const categories = await loadCategories();
        manageCategoriesData = Array.isArray(categories) ? categories : [];
        categorySuggestions = Array.isArray(manageCategoriesData) ? [...manageCategoriesData] : [];
        renderManageCategoriesLists();
        prepareMergeModalContent();
        await populateCategorySearchOptions(false, manageCategoriesData);
    } catch (error) {
        console.error('Error refreshing categories:', error);
        if (userList) {
            userList.innerHTML = '<div style="text-align: center; padding: 2rem; color: #ef4444;">Failed to load categories. Please try again.</div>';
        }
    }
}


function prepareMergeModalContent() {
    const sourceContainer = document.getElementById('merge-source-categories');
    const targetContainer = document.getElementById('merge-target-categories');
    const confirmBtn = document.getElementById('confirm-merge-btn');

    if (!sourceContainer || !targetContainer || !confirmBtn) {
        return;
    }

    const candidates = manageCategoriesData.filter(cat => !cat.system);

    sourceContainer.innerHTML = '';
    targetContainer.innerHTML = '';
    mergeSourceCategoryIds = new Set();
    mergeTargetCategoryId = null;
    confirmBtn.disabled = true;

    if (candidates.length === 0) {
        sourceContainer.innerHTML = '<div style="text-align: center; padding: 1rem; color: #94a3b8;">No custom categories available.</div>';
        targetContainer.innerHTML = '<div style="text-align: center; padding: 1rem; color: #94a3b8;">No categories available.</div>';
        return;
    }

    candidates.forEach(category => {
        const sourceBtn = document.createElement('button');
        sourceBtn.type = 'button';
        sourceBtn.className = 'btn btn-secondary';
        sourceBtn.style.cssText = 'justify-content: flex-start; padding: 0.75rem; border-radius: 0.75rem; display: flex; width: 100%; transition: background 0.2s ease;';
        sourceBtn.textContent = category.name;
        sourceBtn.addEventListener('click', () => {
            if (mergeSourceCategoryIds.has(category.id)) {
                mergeSourceCategoryIds.delete(category.id);
                sourceBtn.classList.remove('active');
                sourceBtn.style.background = '';
                sourceBtn.style.color = '';
            } else {
                mergeSourceCategoryIds.add(category.id);
                sourceBtn.classList.add('active');
                sourceBtn.style.background = 'var(--primary)';
                sourceBtn.style.color = '#ffffff';
            }
            // If target becomes part of source, clear selection.
            if (mergeTargetCategoryId && mergeSourceCategoryIds.has(mergeTargetCategoryId)) {
                mergeTargetCategoryId = null;
            }
            updateMergeTargetButtons(targetContainer);
            updateMergeConfirmButton(confirmBtn);
        });
        sourceContainer.appendChild(sourceBtn);

        const targetBtn = document.createElement('button');
        targetBtn.type = 'button';
        targetBtn.className = 'btn btn-secondary';
        targetBtn.style.cssText = 'justify-content: flex-start; padding: 0.75rem; border-radius: 0.75rem; display: flex; width: 100%; transition: background 0.2s ease;';
        targetBtn.textContent = category.name;
        targetBtn.dataset.categoryId = String(category.id);
        targetBtn.addEventListener('click', () => {
            if (mergeSourceCategoryIds.has(category.id)) {
                return;
            }
            mergeTargetCategoryId = category.id;
            targetContainer.querySelectorAll('button').forEach(btn => {
                btn.classList.remove('active');
                btn.style.background = '';
                btn.style.color = '';
            });
            targetBtn.classList.add('active');
            targetBtn.style.background = 'var(--primary)';
            targetBtn.style.color = '#ffffff';
            updateMergeConfirmButton(confirmBtn);
        });
        targetContainer.appendChild(targetBtn);
    });

    updateMergeTargetButtons(targetContainer);
}

function updateMergeTargetButtons(targetContainer) {
    targetContainer.querySelectorAll('button').forEach(btn => {
        const id = Number(btn.dataset.categoryId);
        if (mergeSourceCategoryIds.has(id)) {
            btn.classList.remove('active');
            btn.style.background = '';
            btn.style.color = '';
            btn.disabled = true;
            btn.style.opacity = '0.5';
        } else {
            btn.disabled = false;
            btn.style.opacity = '1';
        }
    });
}

function updateMergeConfirmButton(confirmBtn) {
    confirmBtn.disabled = mergeSourceCategoryIds.size === 0 || !mergeTargetCategoryId;
}

async function loadAllTransactionsAndSummary() {
    if (typeof loadTransactions === 'function') {
        await loadTransactions();
    } else {
        console.warn('loadTransactions is not available; skipping refresh.');
    }
}

const params = new URLSearchParams(window.location.search);
const openParam = params.get('open');
if (openParam === 'income' || openParam === 'expense') {
    document.addEventListener('DOMContentLoaded', () => {
        // Switch the Quick Add panel to the correct tab
        if (typeof window.switchQuickTab === 'function') {
            window.switchQuickTab(openParam);
        }

        // Scroll to the Quick Add panel
        const quickAddCard = document.querySelector('.quick-add-card');
        if (quickAddCard) {
            setTimeout(() => {
                quickAddCard.scrollIntoView({ behavior: 'smooth', block: 'center' });

                // Focus on the amount input field
                const amountInput = document.querySelector('#quick-add-form input[type="number"]');
                if (amountInput) {
                    amountInput.focus();
                }
            }, 500); // Small delay to ensure page is fully loaded
        }
    });
}

// FAB Menu Toggle
document.addEventListener('DOMContentLoaded', function() {
    const fab = document.getElementById('fab');
    const fabMenu = document.getElementById('fabMenu');
    const fabIcon = document.getElementById('fabIcon');
    let menuOpen = false;

    if (fab && fabMenu && fabIcon) {
        fab.addEventListener('click', () => {
            menuOpen = !menuOpen;
            fabMenu.classList.toggle('active');
            fabIcon.textContent = menuOpen ? '✕' : '➕';
            fab.style.transform = menuOpen ? 'rotate(45deg)' : 'rotate(0)';
        });
    }
});

// Fetch and cache categories from API
async function loadCategories() {
    if (categoryCache) {
        console.log('📦 Using cached categories:', categoryCache);
        return categoryCache;
    }

    try {
        console.log('🔄 Loading categories from API...');
        const sessionToken = localStorage.getItem('sessionToken');
        console.log('🔑 Session token:', sessionToken ? 'Found ✅' : 'Missing ❌');

        const response = await fetch('/api/categories', {
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${sessionToken}`
            }
        });

        console.log('📡 Response status:', response.status, response.statusText);

        if (!response.ok) {
            const errorText = await response.text();
            console.error('❌ API Error Response:', errorText);
            throw new Error(`Failed to load categories: ${response.status} ${response.statusText}`);
        }

        categoryCache = await response.json();
        console.log('✅ Categories loaded successfully:', categoryCache);
        return categoryCache;
    } catch (error) {
        console.error('❌ Error loading categories:', error);
        console.error('❌ Error stack:', error.stack);
        alert('⚠️ Failed to load categories. Please refresh the page.');
        return [];
    }
}

// Get category ID by name
async function getCategoryIdByName(categoryName) {
    console.log('🔍 Looking up category:', categoryName);
    const categories = await loadCategories();
    console.log('📋 Available categories:', categories.map(c => `"${c.name}" (id: ${c.id})`));

    const normalizedName = categoryName.toLowerCase().trim();
    console.log('🔤 Normalized search name:', `"${normalizedName}"`);

    // Try to find category by case-insensitive name match
    const category = categories.find(cat => {
        const catNameLower = cat.name.toLowerCase().trim();
        console.log(`  Comparing "${catNameLower}" === "${normalizedName}": ${catNameLower === normalizedName}`);
        return catNameLower === normalizedName;
    });

    if (category) {
        console.log('✅ Found category:', category);
        console.log('🆔 Returning category ID:', category.id);
        return category.id;
    } else {
        console.error('❌ Category NOT found!');
        console.error('   Searched for:', `"${normalizedName}"`);
        console.error('   Available:', categories.map(c => `"${c.name.toLowerCase()}"`).join(', '));
        return null;
    }
}

// Quick tab switching
window.switchQuickTab = function(type) {
    window.currentTransactionType = type;
    const tabs = document.querySelectorAll('.quick-add-card .quick-tab');
    tabs.forEach(tab => tab.classList.remove('active'));

    // Activate the correct tab based on type
    const targetTab = Array.from(tabs).find(tab =>
        (type === 'income' && tab.textContent.includes('Income')) ||
        (type === 'expense' && tab.textContent.includes('Expense'))
    );
    if (targetTab) {
        targetTab.classList.add('active');
    }

    // Update UI text based on transaction type
    const quickAddTitle = document.querySelector('.quick-add-card h3');
    const submitButton = document.querySelector('#quick-add-form button[type="submit"]');

    if (type === 'income') {
        quickAddTitle.textContent = 'Quick Add Income';
        submitButton.innerHTML = '💰 Add Income';
    } else {
        quickAddTitle.textContent = 'Quick Add Expense';
        submitButton.innerHTML = '💸 Add Expense';
    }

    // Reset selected category
    document.querySelectorAll('.category-option').forEach(opt => opt.classList.remove('selected'));
    selectedCategory = null;
    const quickCategoryInput = document.getElementById('category-search');
    if (quickCategoryInput) {
        quickCategoryInput.value = '';
        quickCategoryInput.classList.remove('has-value');
    }
    const quickCategoryIdField = document.getElementById('selected-category-id');
    if (quickCategoryIdField) {
        quickCategoryIdField.value = '';
    }
    const quickCategoryDropdown = document.getElementById('category-dropdown');
    if (quickCategoryDropdown) {
        quickCategoryDropdown.classList.remove('show');
    }
};

window.switchModalTab = function(type, event) {
    window.currentTransactionType = type;
    const tabs = document.querySelectorAll('#addTransactionModal .quick-tab');
    tabs.forEach(tab => tab.classList.remove('active'));
    if (event && event.target) {
        event.target.classList.add('active');
    } else {
        // Fallback: find and activate the correct tab based on type
        const targetTab = Array.from(tabs).find(tab =>
            (type === 'income' && tab.textContent.includes('Income')) ||
            (type === 'expense' && tab.textContent.includes('Expense'))
        );
        if (targetTab) targetTab.classList.add('active');
    }
};

// Category selection
document.querySelectorAll('.category-option').forEach(option => {
    option.addEventListener('click', function() {
        document.querySelectorAll('.category-option').forEach(opt => opt.classList.remove('selected'));
        this.classList.add('selected');
        selectedCategory = this.getAttribute('data-category');
    });
});


// Note: Search and filter functionality is handled by transactions.js
// Removed duplicate inline implementations to avoid conflicts

// Note: Modal functions (openAddTransactionModal, closeAddTransactionModal)
// are defined in transactions.js to avoid duplication
// viewTransaction functionality moved to transaction click handlers in transactions.js


function importTransactions() {
    const modal = document.getElementById('importTransactionsModal');
    modal.style.display = 'flex';
    document.body.style.overflow = 'hidden';
}

function closeImportModal() {
    const modal = document.getElementById('importTransactionsModal');
    modal.style.display = 'none';
    document.body.style.overflow = '';
    document.getElementById('importFileInput').value = '';
    document.getElementById('importPreview').style.display = 'none';
    document.getElementById('confirmImportBtn').disabled = true;
}

const uploadArea = document.getElementById('file-upload-area');
const fileInput = document.getElementById('importFileInput');
const confirmBtn = document.getElementById('confirmImportBtn');
let importedTransactions = [];

// Open file picker when clicking the area
uploadArea.addEventListener('click', () => fileInput.click());

// Handle file drop
uploadArea.addEventListener('dragover', (e) => {
    e.preventDefault();
    uploadArea.style.background = '#f1f5f9';
});
uploadArea.addEventListener('dragleave', () => {
    uploadArea.style.background = 'transparent';
});
uploadArea.addEventListener('drop', (e) => {
    e.preventDefault();
    uploadArea.style.background = 'transparent';
    if (e.dataTransfer.files.length > 0) {
        handleImportFile(e.dataTransfer.files[0]);
    }
});

// Handle file selection
fileInput.addEventListener('change', (e) => {
    const file = e.target.files[0];
    if (file) handleImportFile(file);
});

// Parse and preview CSV
async function handleImportFile(file) {
    if (!file.name.endsWith('.csv')) {
        alert('⚠️ Please upload a CSV file.');
        return;
    }

    const text = await file.text();
    const rows = text.split(/\r?\n/).filter(r => r.trim().length > 0);
    if (rows.length < 2) {
        alert('⚠️ The file seems empty or invalid.');
        return;
    }

    const headers = rows[0].split(',').map(h => h.trim().toLowerCase());
    const required = ['datetime', 'description', 'amount', 'type'];
    for (const req of required) {
        if (!headers.includes(req)) {
            alert(`❌ Missing required column: "${req}". Required: ${required.join(', ')}`);
            return;
        }
    }

    importedTransactions = [];

    for (let i = 1; i < rows.length; i++) {
        const cols = rows[i].split(',');
        if (cols.length < headers.length) continue;

        const obj = {};
        headers.forEach((h, idx) => obj[h] = cols[idx]?.trim() ?? '');

        const amount = parseFloat(obj.amount);
        const type = obj.type.toLowerCase();
        if (Number.isNaN(amount) || (type !== 'income' && type !== 'expense')) continue;

        let dateStr = obj.datetime || obj.date || '';
        if (dateStr.includes(' ') && !dateStr.includes('T')) dateStr = dateStr.replace(' ', 'T');
        if (!/[zZ]|[+\-]\d{2}:?\d{2}$/.test(dateStr)) dateStr += 'Z';
        const occurredAt = new Date(dateStr).toISOString();

        importedTransactions.push({
            description: obj.description || 'Imported transaction',
            amount,
            occurredAt,
            type,
            categoryName: obj.category || null,
        });
    }

    if (importedTransactions.length === 0) {
        alert('⚠️ No valid transactions found.');
        return;
    }

    // Preview table
    const preview = document.getElementById('previewTableContainer');
    preview.innerHTML = `
        <table style="width: 100%; font-size: 0.85rem; border-collapse: collapse;">
            <thead>
                <tr style="background: #e2e8f0;">
                    <th style="padding: 4px;">DateTime</th>
                    <th style="padding: 4px;">Description</th>
                    <th style="padding: 4px;">Amount</th>
                    <th style="padding: 4px;">Type</th>
                    <th style="padding: 4px;">Category</th>
                </tr>
            </thead>
            <tbody>
                ${importedTransactions.slice(0, 5).map(tx => `
                    <tr>
                        <td style="padding: 4px;">${new Date(tx.occurredAt).toLocaleString()}</td>
                        <td style="padding: 4px;">${tx.description}</td>
                        <td style="padding: 4px;">${tx.amount.toFixed(2)}</td>
                        <td style="padding: 4px;">${tx.type}</td>
                        <td style="padding: 4px;">${tx.categoryName || '-'}</td>
                    </tr>
                `).join('')}
            </tbody>
        </table>
        <p style="margin-top: 0.5rem; color: #64748b;">
          Showing first 5 of ${importedTransactions.length} rows.
        </p>
    `;

    document.getElementById('importPreview').style.display = 'block';
    confirmBtn.disabled = false;
}

// Confirm import button
confirmBtn.addEventListener('click', async () => {
    confirmBtn.disabled = true;
    confirmBtn.textContent = 'Importing...';

    try {
        const accountId = await incomeService.getDefaultAccountId(true);

        // Step 1: Build unique category list (case-insensitive)
        const uniqueCategories = [
            ...new Set(
                importedTransactions
                    .map(tx => tx.categoryName?.trim())
                    .filter(Boolean)
                    .map(name => name.toLowerCase())
            )
        ];

        // Step 2: Create a map of category name → ID
        const categoryMap = {};
        let allCategories = await loadCategories();

        for (const catNameLower of uniqueCategories) {
            let existing = allCategories.find(
                c => c.name.toLowerCase().trim() === catNameLower
            );

            if (existing) {
                categoryMap[catNameLower] = existing.id;
                continue;
            }

            // Try creating new category if it doesn’t exist
            try {
                const formattedName =
                    catNameLower.charAt(0).toUpperCase() + catNameLower.slice(1);
                const newCategory = await categoryService.createCategory({
                    name: formattedName
                });
                categoryMap[catNameLower] = newCategory?.id || null;

                // Refresh cache for subsequent lookups
                invalidateCategoryCache();
                allCategories = await loadCategories();
            } catch (err) {
                if (
                    err.message &&
                    err.message.toLowerCase().includes('already exists')
                ) {
                    const retryId = await getCategoryIdByName(catNameLower);
                    categoryMap[catNameLower] = retryId;
                } else {
                    console.error(`❌ Failed to create category "${catNameLower}":`, err);
                    categoryMap[catNameLower] = null;
                }
            }
        }

        // Step 3: Import transactions using the map
        let importCount = 0;

        for (const tx of importedTransactions) {
            // Skip if transaction is invalid or missing required fields
            if (!tx || typeof tx.amount === 'undefined' || !tx.description || !tx.occurredAt || !tx.type) {
                console.warn('⚠️ Skipping invalid transaction:', tx);
                continue;
            }

            const normalizedCat =
                tx.categoryName?.toLowerCase().trim() || null;
            const categoryId =
                normalizedCat && categoryMap[normalizedCat]
                    ? categoryMap[normalizedCat]
                    : null;

            const payload = {
                amount: tx.amount,
                description: tx.description,
                occurredAt: tx.occurredAt,
                accountId,
                categoryId
            };

            try {
                if (tx.type === 'income') {
                    await incomeService.addIncome(payload);
                } else {
                    await expenseService.addExpense(payload);
                }

                // Grant coins per transaction
                if (typeof coinSystem !== 'undefined' && coinSystem) {
                    try {
                        coinSystem.earnFromTransaction();
                    } catch (e) {
                        console.warn('⚠️ Failed to award coins for transaction:', e);
                    }
                }

                importCount++;
                // Optional: lightweight progress text
                confirmBtn.textContent = `Importing... (${importCount}/${importedTransactions.length})`;
            } catch (txError) {
                console.error(`❌ Failed to import transaction:`, tx, txError);
                // Continue with next transaction instead of failing entire import
            }
        }

        // Step 4: Refresh everything
        await loadAllTransactionsAndSummary(); // updates Quick Stats + list
        showNotification(`✅ Imported ${importCount} transactions successfully!`);
        closeImportModal();

        if (typeof expenseService.updateDashboardTransactions === 'function') {
            expenseService.updateDashboardTransactions();
        }
        if (typeof incomeService.updateDashboardTransactions === 'function') {
            incomeService.updateDashboardTransactions();
        }

    } catch (err) {
        console.error('❌ Import failed', err);
        alert('❌ Failed to import transactions: ' + err.message);
    } finally {
        confirmBtn.disabled = false;
        confirmBtn.textContent = 'Import';
    }
});

// Navigate to Rewards Shop
function navigateToRewardsShop() {
    window.location.href = 'shopping_page.html';
}

function showEarningsInfo() {
    alert('💰 Budget Coins: 1,247\n\n🎯 Recent Earnings:\n• Logged 3 transactions today: +75 coins\n• Stayed under budget this week: +200 coins\n• Daily streak (5 days): +125 coins\n\n🛍️ Visit Rewards Shop to spend coins!\n\n💡 Earn more by:\n- Logging expenses daily (+25 each)\n- Staying within budgets (+50-200)\n- Reaching milestones (+300-500)\n- Weekly consistency streaks (+200)');
}

// Form submissions
const quickAddForm = document.getElementById('quick-add-form');
if (quickAddForm) {
quickAddForm.addEventListener('submit', async function(e) {
    e.preventDefault();

    const amount = this.querySelector('.amount-input').value;
    const description = this.querySelector('input[placeholder="What was this for?"]').value;
    const quickCategoryIdField = document.getElementById('selected-category-id');
    const quickCategoryId = quickCategoryIdField && quickCategoryIdField.value
        ? parseInt(quickCategoryIdField.value, 10)
        : null;
    if (!selectedCategory) {
        const quickCategoryInputValue = document.getElementById('category-search')?.value;
        if (quickCategoryInputValue && quickCategoryInputValue.trim().length > 0) {
            selectedCategory = quickCategoryInputValue.trim();
        }
    }

    // Validate required fields
    if (!amount || !description) {
        alert('Please fill in all required fields!');
        return;
    }

    // Category is only required for expenses, not for income
    if (window.currentTransactionType === 'expense' && !selectedCategory) {
        // Optional: If category isn't necessary, we can proceed without blocking
        console.warn('No quick-add category selected; proceeding without category.');
    }

    // Handle income transactions through the income service
    if (window.currentTransactionType === 'income') {
        try {
            let categoryId = null;
            if (quickCategoryId !== null && !Number.isNaN(quickCategoryId)) {
                categoryId = quickCategoryId;
            } else if (selectedCategory) {
                categoryId = await getCategoryIdByName(selectedCategory);
                if (!categoryId) {
                    console.warn(`⚠️ Quick-add income category "${selectedCategory}" not found; saving without category.`);
                }
            }
            const nowIso = new Date().toISOString().slice(0, 19);
            const accountId = await incomeService.getDefaultAccountId(true);
            const incomeData = {
                amount: parseFloat(amount),
                description: description,
                accountId: accountId,
                occurredAt: nowIso,
                categoryId: categoryId
            };

            // Validate data
            const validation = incomeService.validateIncomeData(incomeData);
            if (!validation.isValid) {
                alert('Please check your input:\n• ' + validation.errors.join('\n• '));
                return;
            }

            // Submit income through API
            const response = await incomeService.addIncome(incomeData);

            // Show success message
            incomeService.showSuccessMessage(
                `Income of ${incomeService.formatAmount(incomeData.amount)} added successfully!`
            );

            // Add to transactions list with animation
            incomeService.addIncomeToTransactionsList(incomeData, response);

            // Update dashboard if on dashboard page
            incomeService.updateDashboardTransactions(incomeData, response);

            // Award coins for logging transaction
            if (typeof coinSystem !== 'undefined') {
                coinSystem.earnFromTransaction();
            }

            // Refresh the transactions list and summary
            await loadAllTransactionsAndSummary();

        } catch (error) {
            console.error('Error adding income:', error);
            alert('❌ Failed to add income: ' + (error.message || 'Please try again.'));
            return;
        }
    } else {
        // Handle expense transactions through the expense service
        try {
            let categoryId = null;
            if (quickCategoryId !== null && !Number.isNaN(quickCategoryId)) {
                categoryId = quickCategoryId;
            } else if (selectedCategory) {
                categoryId = await getCategoryIdByName(selectedCategory);
                if (!categoryId) {
                    console.warn(`⚠️ Quick-add expense category "${selectedCategory}" not found; saving without category.`);
                }
            }

            const nowIsoExpense = new Date().toISOString().slice(0, 19);
            const accountId = await incomeService.getDefaultAccountId(true);
            const expenseData = {
                amount: parseFloat(amount),
                description: description,
                categoryId: categoryId,
                accountId: accountId,
                occurredAt: nowIsoExpense
            };

            // Validate data
            const validation = expenseService.validateExpenseData(expenseData);
            if (!validation.isValid) {
                alert('Please check your input:\n• ' + validation.errors.join('\n• '));
                return;
            }

            // Submit expense through API
            const response = await expenseService.addExpense(expenseData);

            // Show success message
            expenseService.showSuccessMessage(
                `Expense of ${expenseService.formatAmount(expenseData.amount)} added successfully!`
            );

            // Add to transactions list with animation
            expenseService.addExpenseToTransactionsList(expenseData, response);

            // Update dashboard if on dashboard page
            expenseService.updateDashboardTransactions(expenseData, response);

            // Award coins for logging transaction
            if (typeof coinSystem !== 'undefined') {
                coinSystem.earnFromTransaction();
            }

            // Refresh the transactions list and summary
            await loadAllTransactionsAndSummary();

        } catch (error) {
            console.error('Error adding expense:', error);
            alert('❌ Failed to add expense: ' + (error.message || 'Please try again.'));
            return;
        }
    }

    // Reset form
    this.reset();
    document.querySelectorAll('.category-option').forEach(opt => opt.classList.remove('selected'));
    selectedCategory = null;
    if (quickCategoryIdField) {
        quickCategoryIdField.value = '';
    }
    const quickCategoryInput = document.getElementById('category-search');
    if (quickCategoryInput) {
        quickCategoryInput.classList.remove('has-value');
    }
    const quickCategoryDropdown = document.getElementById('category-dropdown');
    if (quickCategoryDropdown) {
        quickCategoryDropdown.classList.remove('show');
    }
});
}

initializeCategorySearchInputs();
populateCategorySearchOptions();

window.openManageCategoriesModal = async function() {
    setModalVisibility('manageCategoriesModal', true);
    await refreshManageCategories(true);
};

window.closeManageCategoriesModal = function() {
    setModalVisibility('manageCategoriesModal', false);
};

window.openCreateCategoryModal = function() {
    const form = document.getElementById('categoryForm');
    const nameInput = document.getElementById('categoryName');
    const editField = document.getElementById('editCategoryId');
    const title = document.getElementById('category-modal-title');

    if (form) {
        form.reset();
    }
    if (nameInput) {
        nameInput.value = '';
    }
    if (editField) {
        editField.value = '';
    }
    if (title) {
        title.textContent = '➕ Create Category';
    }

    setModalVisibility('createCategoryModal', true);
};

window.openEditCategoryModal = function(categoryId) {
    const category = manageCategoriesData.find(cat => cat.id === categoryId);
    if (!category) {
        alert('Unable to find the selected category.');
        return;
    }

    const nameInput = document.getElementById('categoryName');
    const editField = document.getElementById('editCategoryId');
    const title = document.getElementById('category-modal-title');

    if (nameInput) {
        nameInput.value = category.name;
    }
    if (editField) {
        editField.value = categoryId;
    }
    if (title) {
        title.textContent = '✏️ Rename Category';
    }

    setModalVisibility('createCategoryModal', true);
};

window.closeCreateCategoryModal = function() {
    setModalVisibility('createCategoryModal', false);
};

window.openDeleteCategoryModal = function(categoryId) {
    deleteCategoryId = categoryId;

    const category = manageCategoriesData.find(cat => cat.id === categoryId);
    const nameTarget = document.getElementById('delete-category-name');
    if (nameTarget) {
        nameTarget.textContent = category ? category.name : '';
    }

    setModalVisibility('deleteCategoryModal', true);
};

window.closeDeleteCategoryModal = function() {
    deleteCategoryId = null;
    setModalVisibility('deleteCategoryModal', false);
};

window.openMergeCategoriesModal = function() {
    prepareMergeModalContent();
    setModalVisibility('mergeCategoriesModal', true);
};

window.closeMergeCategoriesModal = function() {
    mergeSourceCategoryIds = new Set();
    mergeTargetCategoryId = null;
    setModalVisibility('mergeCategoriesModal', false);
};

window.confirmDeleteCategory = async function() {
    if (deleteCategoryId === null) {
        alert('Select a category to delete.');
        return;
    }

    // Get category name for confirmation
    const category = manageCategoriesData.find(cat => cat.id === deleteCategoryId);
    const categoryName = category ? category.name : 'this category';

    // Check if category has budgets
    try {
        console.log('💰 Checking for budgets in category to delete...');
        const budgets = await categoryService.getCategoryBudgets(deleteCategoryId);
        const hasBudgets = budgets && budgets.length > 0;

        // Build confirmation message
        let confirmMessage = `⚠️ Are you sure you want to delete "${categoryName}"?\n\n`;
        confirmMessage += `This will:\n`;
        confirmMessage += `• Permanently delete all transactions linked to this category\n`;

        if (hasBudgets) {
            const budgetInfo = budgets.map(b =>
                `  • ${b.yearMonth}: $${b.amount}${b.customName ? ` (${b.customName})` : ''}`
            ).join('\n');
            confirmMessage += `• PERMANENTLY DELETE ${budgets.length} budget(s):\n${budgetInfo}\n\n`;
            confirmMessage += `⚠️ This action CANNOT be undone!`;
        } else {
            confirmMessage += `\nThis action cannot be undone.`;
        }

        // Show confirmation dialog
        const userConfirmed = confirm(confirmMessage);
        if (!userConfirmed) {
            console.log('User cancelled delete operation');
            return;
        }

        await categoryService.deleteCategory(deleteCategoryId);
        invalidateCategoryCache();
        await refreshManageCategories(true);
        loadCategoriesIntoFilter();

        const successMsg = hasBudgets
            ? `Category "${categoryName}" deleted successfully.\nTransactions and budgets have been permanently removed.`
            : `Category "${categoryName}" deleted successfully.\nAll related transactions were removed.`;
        alert(successMsg);
        window.closeDeleteCategoryModal();
    } catch (error) {
        console.error('Failed to delete category:', error);
        alert(error.message || 'Failed to delete category. Please try again.');
    }
};

window.confirmMergeCategories = async function() {
    if (mergeSourceCategoryIds.size === 0 || !mergeTargetCategoryId) {
        alert('Select at least one category to merge and a target category.');
        return;
    }

    const sourceIds = Array.from(mergeSourceCategoryIds);

    try {
        // Get category names for confirmation message
        const sourceCategories = manageCategoriesData.filter(cat => sourceIds.includes(cat.id));
        const targetCategory = manageCategoriesData.find(cat => cat.id === mergeTargetCategoryId);
        const sourceNames = sourceCategories.map(c => c.name).join(', ');
        const targetName = targetCategory ? targetCategory.name : 'target category';

        // Check if any source categories have budgets
        console.log('💰 Checking for budgets in source categories...');
        const budgetChecks = await Promise.all(
            sourceIds.map(id => categoryService.getCategoryBudgets(id))
        );

        const allBudgets = budgetChecks.flat();
        const hasBudgets = allBudgets.length > 0;

        // Build confirmation message
        let confirmMessage = `🔀 Merge Categories Confirmation\n\n`;
        confirmMessage += `Merge: ${sourceNames}\n`;
        confirmMessage += `Into: ${targetName}\n\n`;
        confirmMessage += `This will:\n`;
        confirmMessage += `• Move all transactions to "${targetName}"\n`;

        if (hasBudgets) {
            const budgetInfo = allBudgets.map(b =>
                `  • ${b.yearMonth}: $${b.amount}${b.customName ? ` (${b.customName})` : ''}`
            ).join('\n');
            confirmMessage += `• Process ${allBudgets.length} budget(s):\n${budgetInfo}\n`;
            confirmMessage += `  (If target has a budget for the same month, amounts will be summed)\n`;
        }

        confirmMessage += `• Delete source categories\n\n`;
        confirmMessage += `Do you want to proceed?`;

        // Show confirmation dialog
        const userConfirmed = confirm(confirmMessage);
        if (!userConfirmed) {
            console.log('User cancelled merge operation');
            return;
        }

        // Perform merge - always pass true to reassign budgets to target category
        console.log('🔄 Merging categories with budget reassignment (mergeBudgets=true)');
        await categoryService.mergeCategories(sourceIds, mergeTargetCategoryId, true);
        invalidateCategoryCache();
        await refreshManageCategories(true);
        loadCategoriesIntoFilter();

        const successMsg = hasBudgets
            ? `Categories merged successfully.\nBudgets have been processed: reassigned or summed where conflicts existed.`
            : `Categories merged successfully.`;

        alert(successMsg);
        window.closeMergeCategoriesModal();
    } catch (error) {
        console.error('Failed to merge categories:', error);
        alert(error.message || 'Failed to merge categories. Please try again.');
    }
};

const categoryForm = document.getElementById('categoryForm');
if (categoryForm) {
    categoryForm.addEventListener('submit', async function(event) {
        event.preventDefault();
        const nameInput = document.getElementById('categoryName');
        const editField = document.getElementById('editCategoryId');

        const name = nameInput ? nameInput.value.trim() : '';
        const editId = editField ? parseInt(editField.value, 10) : NaN;

        const validation = categoryService.validateCategoryName(name);
        if (!validation.isValid) {
            alert(validation.errors.join('\n'));
            return;
        }

        try {
            if (!Number.isNaN(editId) && editField && editField.value) {
                await categoryService.updateCategory(editId, { name });
                if (typeof showNotification === 'function') {
                    showNotification('✏️ Category renamed successfully.');
                } else {
                    alert('Category renamed successfully.');
                }
            } else {
                await categoryService.createCategory({ name });
                if (typeof showNotification === 'function') {
                    showNotification('🏷️ Category created successfully!');
                } else {
                    console.log('Category created successfully.');
                }
            }

            invalidateCategoryCache();
            await refreshManageCategories(true);
            loadCategoriesIntoFilter();
            window.closeCreateCategoryModal();
        } catch (error) {
            console.error('Failed to save category:', error);
            alert(error.message || 'Failed to save category. Please try again.');
        }
    });
}

// Load categories into filter dropdown
async function loadCategoriesIntoFilter() {
    try {
        const categories = await loadCategories();
        const filterSelect = document.getElementById('category-filter');

        if (filterSelect) {
            filterSelect.innerHTML = '<option value="">All Categories</option>' +
                categories.map(category => {
                    return `<option value="${category.name.toLowerCase()}">${category.name}</option>`;
                }).join('');
        }
        await populateCategorySearchOptions(false, categories);
    } catch (error) {
        console.error('Error loading categories into filter:', error);
    }
}

// Close modals when clicking outside
const manageCategoriesModal = document.getElementById('manageCategoriesModal');
if (manageCategoriesModal) {
    manageCategoriesModal.addEventListener('click', function(e) {
        if (e.target === this) {
            closeManageCategoriesModal();
        }
    });
}

const createCategoryModalEl = document.getElementById('createCategoryModal');
if (createCategoryModalEl) {
    createCategoryModalEl.addEventListener('click', function(e) {
        if (e.target === this) {
            closeCreateCategoryModal();
        }
    });
}

const deleteCategoryModalEl = document.getElementById('deleteCategoryModal');
if (deleteCategoryModalEl) {
    deleteCategoryModalEl.addEventListener('click', function(e) {
        if (e.target === this) {
            closeDeleteCategoryModal();
        }
    });
}

const mergeCategoriesModalEl = document.getElementById('mergeCategoriesModal');
if (mergeCategoriesModalEl) {
    mergeCategoriesModalEl.addEventListener('click', function(e) {
        if (e.target === this) {
            closeMergeCategoriesModal();
        }
    });
}

// Load categories into filter on page load (in addition to the existing DOMContentLoaded)
setTimeout(() => {
    loadCategoriesIntoFilter();
}, 1000);