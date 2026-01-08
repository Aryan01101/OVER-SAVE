const subscriptionService = {
  apiUrl: '/api/subscriptions',

  // ----- Auth helpers -----
  getToken() {
    try {
      if (typeof AuthManager?.getSessionToken === 'function') {
        return AuthManager.getSessionToken();
      }
    } catch (_) {}
    return localStorage.getItem('sessionToken') || null;
  },

  getUserIdFallback() {
    // Only used if no token (dev fallback)
    const ui = localStorage.getItem('userInfo');
    if (ui) {
      try { return JSON.parse(ui).userId; } catch (_) {}
    }
    return sessionStorage.getItem('userId') || '1';
  },

  buildHeaders(json = false) {
    const token = this.getToken();
    const headers = {};
    if (json) headers['Content-Type'] = 'application/json';

    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    } else {
      // Dev fallback: some endpoints may accept X-User-Id
      const fallbackId = this.getUserIdFallback();
      console.warn('⚠️ No session token found; falling back to X-User-Id:', fallbackId);
      headers['X-User-Id'] = fallbackId;
    }
    return headers;
  },

  async _fetch(method, path, body) {
    const res = await fetch(`${this.apiUrl}${path}`, {
      method,
      headers: this.buildHeaders(true),
      body: body ? JSON.stringify(body) : undefined
    });

    if (res.status === 401) {
      // Not authenticated — go to login
      window.location.href = '/html/login.html';
      return;
    }

    if (!res.ok) {
      const text = await res.text().catch(() => '');
      console.error(`❌ ${method} ${path} failed:`, res.status, text);
      throw new Error(text || `HTTP ${res.status} ${res.statusText}`);
    }

    if (res.status === 204) return null;
    const contentType = res.headers.get('content-type') || '';
    return contentType.includes('application/json') ? res.json() : res.text();
  },

  // ----- API methods -----
  list(activeOnly = false) {
    console.log('Fetching subscriptions…');
    return this._fetch('GET', `?activeOnly=${activeOnly}`);
  },

  create(payload) {
    console.log('Creating subscription:', payload);
    return this._fetch('POST', '', payload);
  },

  update(id, payload) {
    console.log(`Updating subscription ${id}:`, payload);
    return this._fetch('PUT', `/${id}`, payload);
  },

  pause(id) {
    console.log(`Pausing subscription ${id}`);
    return this._fetch('PATCH', `/${id}/pause`);
  },

  resume(id) {
    console.log(`Resuming subscription ${id}`);
    return this._fetch('PATCH', `/${id}/resume`);
  },

  delete(id) {
    console.log(`Deleting subscription ${id}`);
    return this._fetch('DELETE', `/${id}`);
  },

  // ----- toasts -----
  showSuccessMessage(msg) {
    toast(msg, '#10b981', 3000);
  },
  showErrorMessage(msg) {
    toast(msg, '#ef4444', 4000);
  }
};

window.subscriptionService = subscriptionService;

// Simple toast helper shared below
function toast(message, bg, ms) {
  const el = document.createElement('div');
  el.textContent = message;
  el.style.cssText = `
    position: fixed; top: 2rem; right: 2rem; z-index: 10000;
    background: ${bg}; color: #fff; padding: 1rem 1.5rem;
    border-radius: .5rem; box-shadow: 0 4px 6px rgba(0,0,0,.1);
    animation: slideInRight .25s ease-out;
  `;
  document.body.appendChild(el);
  setTimeout(() => {
    el.style.animation = 'slideOutRight .25s ease-in';
    setTimeout(() => el.remove(), 250);
  }, ms);
}

// ===============================
// Page wiring 
// ===============================
(function subscriptionsPage() {
  const $ = (s, r=document) => r.querySelector(s);
  const $$ = (s, r=document) => Array.from(r.querySelectorAll(s));

  const aud = n => (Number(n || 0)).toLocaleString('en-AU', { style: 'currency', currency: 'AUD' });
  const fmtDate = iso => iso ? new Date(iso).toLocaleDateString('en-AU', { day: 'numeric', month: 'short', year: 'numeric' }) : '—';
  const daysUntil = iso => {
    if (!iso) return Infinity;
    const today = new Date(); today.setHours(0, 0, 0, 0);
    const d = new Date(iso);
    return Math.round((d - today) / 86400000);
  };
  const freqLabel = f => {
    const x = (f || '').toUpperCase();
    return x === 'WEEKLY' ? '/week'
      : x === 'FORTNIGHTLY' ? '/fortnight'
      : x === 'QUARTERLY' ? '/quarter'
      : (x === 'YEARLY' || x === 'ANNUAL' || x === 'ANNUALLY') ? '/year'
      : '/month';
  };

  let allSubs = [];
  let currentFilter = 'all';

  function renderSummary(subs) {
    const monthlyTotal = subs.filter(s => s.isActive).reduce((sum, s) => sum + Number(s.monthlyEquivalent || 0), 0);
    const activeCount = subs.filter(s => s.isActive).length;
    const dueThisWeek = subs.filter(s => s.isActive && daysUntil(s.nextPostAt) >= 0 && daysUntil(s.nextPostAt) <= 7).length;

    $('.summary-card.total .summary-value').textContent = aud(monthlyTotal);
    $('.summary-card.active .summary-value').textContent = String(activeCount);
    $('.summary-card.upcoming .summary-value').textContent = String(dueThisWeek);
    $('.summary-card.saved .summary-value').textContent = 'Nil';
  }

  function renderList(subs) {
    const root = $('#subscriptions-list');
    root.innerHTML = '';
    if (!subs.length) {
      root.innerHTML = `<div style="padding:1rem;color:#6b7280;">No subscriptions yet. Add one on the right →</div>`;
      return;
    }
    for (const s of subs) {
      const status = s.isActive ? 'active' : 'paused';
      const dueSoon = s.isActive && daysUntil(s.nextPostAt) <= 7 ? 'due-soon' : '';
      const badgeClass = s.isActive ? 'status-active' : 'status-paused';
      const statusText = s.isActive ? 'Active' : 'Paused';
      const nextText = s.isActive
        ? `Next billing: ${fmtDate(s.nextPostAt)}${
          Number.isFinite(daysUntil(s.nextPostAt)) && daysUntil(s.nextPostAt) >= 0
            ? ` (${daysUntil(s.nextPostAt)} day${daysUntil(s.nextPostAt)===1?'':'s'})` : ''}`
        : 'Paused until resumed';

      const row = document.createElement('div');
      row.className = `subscription-item ${dueSoon}`;
      row.dataset.status = status;
      row.dataset.id = s.subscriptionId;
      row.innerHTML = `
        <div class="subscription-logo" style="background:#111827;">🧾</div>
        <div class="subscription-details">
          <div class="subscription-name">${s.merchant}</div>
          <div class="subscription-plan">&nbsp;</div>
          <div class="subscription-next-billing">${nextText}</div>
        </div>
        <div style="text-align:right;margin-right:1rem;">
          <div class="subscription-amount">${aud(s.amount)}</div>
          <div class="subscription-frequency">${freqLabel(s.frequency)}</div>
          <span class="status-badge ${badgeClass}">${statusText}</span>
        </div>
        <div class="subscription-actions">
          <button class="action-btn edit-btn">✏️ Edit</button>
          ${s.isActive
            ? `<button class="action-btn pause-btn">⏸️ Pause</button>`
            : `<button class="action-btn resume-btn">▶️ Resume</button>`
          }
          <button class="action-btn cancel-btn">❌ Cancel</button>
        </div>
      `;

      row.querySelector('.edit-btn').addEventListener('click', () => openEditPrompt(s));
      row.querySelector('.pause-btn')?.addEventListener('click', () => onPause(s));
      row.querySelector('.resume-btn')?.addEventListener('click', () => onResume(s));
      row.querySelector('.cancel-btn').addEventListener('click', () => onDelete(s));

      root.appendChild(row);
    }
  }

  function applyFilterUI() {
    $$('.filter-btn').forEach(b => {
      const f = b.dataset.filter || b.textContent.toLowerCase();
      b.classList.toggle('active', f === currentFilter);
    });
    $$('#subscriptions-list .subscription-item').forEach(el => {
      const st = el.dataset.status || 'active';
      el.style.display = (currentFilter === 'all' || st === currentFilter) ? 'flex' : 'none';
    });
  }

  function renderRenewals(subs) {
    const container = $('#renewals-list');
    if (!container) return;
    const upcoming = subs
      .filter(s => s.isActive && s.nextPostAt)
      .sort((a,b)=> new Date(a.nextPostAt) - new Date(b.nextPostAt))
      .slice(0,5);

    container.innerHTML = upcoming.map(s => {
      const urgent = daysUntil(s.nextPostAt) <= 2 ? 'urgent' : '';
      return `
        <div class="renewal-item ${urgent}">
          <div class="renewal-icon">📅</div>
          <div class="renewal-details">
            <div class="renewal-title">${s.merchant}</div>
            <div class="renewal-date">Renews ${fmtDate(s.nextPostAt)}</div>
          </div>
          <div class="renewal-amount">${aud(s.amount)}</div>
        </div>
      `;
    }).join('');
  }

  async function refresh() {
    const subs = await subscriptionService.list(false);
    subs.sort((a,b) => {
      if (!a.nextPostAt && !b.nextPostAt) return 0;
      if (!a.nextPostAt) return 1;
      if (!b.nextPostAt) return -1;
      return new Date(a.nextPostAt) - new Date(b.nextPostAt);
    });
    allSubs = subs;
    renderSummary(allSubs);
    renderList(allSubs);
    renderRenewals(allSubs);
    applyFilterUI();
  }

  async function onCreateFromSidebar(e) {
    e.preventDefault();
    const name = $('#service-name').value.trim();
    const cost = parseFloat($('#service-cost').value);
    const freq = ($('#billing-frequency').value || 'monthly').toUpperCase();
    const firstDate = $('#billing-date').value;

    if (!name || !(cost > 0) || !firstDate) {
      subscriptionService.showErrorMessage('Please fill in all required fields');
      return;
    }

    try {
      const isoDateTime = firstDate.includes('T') ? firstDate : `${firstDate}T00:00:00`;

      await subscriptionService.create({
        merchant: name,
        amount: cost,
        frequency: freq,
        startDate: isoDateTime,
        firstPostAt: isoDateTime,
        isActive: true
      });
      e.target.reset();
      subscriptionService.showSuccessMessage(`Added "${name}"`);
      await refresh();
    } catch (err) {
      subscriptionService.showErrorMessage(err.message || 'Failed to add subscription');
    }
  }

  function openEditPrompt(s) {
    const newName   = prompt('Service name:', s.merchant);                 
    if (newName === null) return;

    const newAmount = prompt('Amount (e.g., 15.99):', String(s.amount));   
    if (newAmount === null) return;

    const newFreq   = prompt('Frequency (WEEKLY/FORTNIGHTLY/MONTHLY/QUARTERLY/YEARLY):', (s.frequency||'').toUpperCase());
    if (newFreq === null) return;

    const newNext = prompt('Next billing date (yyyy-MM-dd or yyyy-MM-ddTHH:mm):',
      s.nextPostAt ? s.nextPostAt.split('T')[0] : ''
    );
    if (newNext === null) return;

    subscriptionService.update(s.subscriptionId, {
      merchant: (newName || s.merchant).trim(),
      amount: Number(newAmount || s.amount),
      frequency: (newFreq || s.frequency).toUpperCase(),
      startDate: s.startDate,
      firstPostAt: newNext ? (newNext.includes('T') ? newNext : `${newNext}T00:00:00`) : s.nextPostAt,
      active: s.isActive
    })
      .then(() => { subscriptionService.showSuccessMessage('Subscription updated'); return refresh(); })
      .catch(err => subscriptionService.showErrorMessage(err.message || 'Failed to update'));
  }

  async function onPause(s) {
    if (!confirm(`Pause ${s.merchant}?`)) return;
    try { await subscriptionService.pause(s.subscriptionId); subscriptionService.showSuccessMessage('Subscription paused'); await refresh(); }
    catch (err) { subscriptionService.showErrorMessage(err.message || 'Failed to pause'); }
  }

  async function onResume(s) {
    if (!confirm(`Resume ${s.merchant}?`)) return;
    try { await subscriptionService.resume(s.subscriptionId); subscriptionService.showSuccessMessage('Subscription resumed'); await refresh(); }
    catch (err) { subscriptionService.showErrorMessage(err.message || 'Failed to resume'); }
  }

  async function onDelete(s) {
    if (!confirm(`Cancel ${s.merchant}? This cannot be undone.`)) return;
    try { await subscriptionService.delete(s.subscriptionId); subscriptionService.showSuccessMessage('Subscription deleted'); await refresh(); }
    catch (err) { subscriptionService.showErrorMessage(err.message || 'Failed to delete'); }
  }

  document.addEventListener('DOMContentLoaded', () => {
    // date min = today
    const todayIso = new Date().toISOString().slice(0,10);
    $$('input[type="date"]').forEach(inp => inp.min = todayIso);

    // filter tabs
    $$('.filter-btn').forEach(btn => {
      btn.addEventListener('click', (ev) => {
        $$('.filter-btn').forEach(b => b.classList.remove('active'));
        ev.currentTarget.classList.add('active');
        currentFilter = ev.currentTarget.dataset.filter || ev.currentTarget.textContent.toLowerCase();
        applyFilterUI();
      });
    });

    // form
    $('#add-subscription-form')?.addEventListener('submit', onCreateFromSidebar);

    // analyze
    $('#analyze-btn')?.addEventListener('click', () => {
      const monthly = allSubs.filter(s=>s.isActive).reduce((sum,s)=> sum + Number(s.monthlyEquivalent||0), 0);
      alert(`📊 Monthly total for active subscriptions: ${aud(monthly)}`);
    });

    // initial load
    refresh().catch(err => {
      subscriptionService.showErrorMessage(err.message || 'Failed to load subscriptions');
    });
  });

  // tiny animations used by toasts
  const style = document.createElement('style');
  style.textContent = `
    @keyframes slideInRight { from { transform: translateX(100%); opacity:0; } to { transform: translateX(0); opacity:1; } }
    @keyframes slideOutRight { from { transform: translateX(0); opacity:1; } to { transform: translateX(100%); opacity:0; } }
  `;
  document.head.appendChild(style);
})();
