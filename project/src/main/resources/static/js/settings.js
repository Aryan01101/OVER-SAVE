// ===============================
// SETTINGS SERVICE
// ===============================
const settingsService = {
  apiUrl: '/api/export',

  // ----- Auth helpers -----
  getToken() {
    try {
      if (typeof AuthManager?.getSessionToken === 'function') {
        return AuthManager.getSessionToken();
      }
    } catch (_) {}
    return localStorage.getItem('sessionToken') || null;
  },

  getUserInfo() {
    try {
      if (typeof AuthManager?.getUserInfo === 'function') {
        return AuthManager.getUserInfo();
      }
    } catch (_) {}
    const ui = localStorage.getItem('userInfo');
    if (ui) {
      try { return JSON.parse(ui); } catch (_) {}
    }
    return null;
  },

  getUserIdFallback() {
    const ui = this.getUserInfo();
    if (ui && ui.userId) return ui.userId;
    return sessionStorage.getItem('userId') || '1';
  },

  buildHeaders(json = false) {
    const token = this.getToken();
    const headers = {};
    if (json) headers['Content-Type'] = 'application/json';

    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    } else {
      const fallbackId = this.getUserIdFallback();
      console.warn('⚠️ No session token found; falling back to X-User-Id:', fallbackId);
      headers['X-User-Id'] = fallbackId;
    }
    return headers;
  },

  async _fetch(method, path, body, isJsonRequest = true, acceptCsv = false) {
    const headers = this.buildHeaders(isJsonRequest);

    if (acceptCsv) {
      headers['Accept'] = 'text/csv';
    }

    const res = await fetch(`${this.apiUrl}${path}`, {
      method,
      headers,
      body: body && isJsonRequest ? JSON.stringify(body) : body,
    });

    if (res.status === 401) {
      window.location.href = '/html/login.html';
      return;
    }

    if (!res.ok) {
      const text = await res.text().catch(() => '');
      console.error(`❌ ${method} ${path} failed:`, res.status, text);
      throw new Error(text || `HTTP ${res.status} ${res.statusText}`);
    }

    return res;
  },

  // =======================
  // EXPORT TRANSACTIONS
  // =======================
  async exportUserData() {
    const user = this.getUserInfo();
    if (!user) {
      this.showErrorMessage('User not logged in.');
      window.location.href = '/html/login.html';
      return;
    }

    const userId = user.userId || this.getUserIdFallback();
    const url = `/transactions?userId=${userId}`;

    try {
      const res = await this._fetch('GET', url, null, false, true);
      const blob = await res.blob();

      const link = document.createElement('a');
      link.href = URL.createObjectURL(blob);
      link.download = 'transactions.csv';
      document.body.appendChild(link);
      link.click();
      link.remove();

      this.showSuccessMessage('✅ Export complete!');
    } catch (err) {
      this.showErrorMessage(err.message || 'Export failed');
    }
  },

  // ===============================
  // EMAIL NOTIFICATION PREFERENCES
  // ===============================
  async loadEmailPreference() {
    const user = this.getUserInfo();
    if (!user) return false;
    const res = await this._fetch('GET', `/../users/notifications?userId=${user.userId}`);
    return res.json();
  },

  async updateEmailPreference(enabled) {
    const user = this.getUserInfo();
    if (!user) throw new Error('Not logged in');
    await this._fetch('PUT', `/../users/notifications?userId=${user.userId}`, enabled);
  },

  async sendTestEmail() {
    const user = this.getUserInfo();
    if (!user) {
      this.showErrorMessage('Not logged in');
      return;
    }

    try {
      const res = await fetch(`/api/users/notifications/test?userId=${user.userId}`, {
        method: 'POST',
        headers: this.buildHeaders(true)
      });

      if (!res.ok) throw new Error(await res.text());
      this.showSuccessMessage('📨 Test email sent!');
    } catch (err) {
      this.showErrorMessage(err.message || 'Failed to send test email');
    }
  },

  // ----- Toast helpers -----
  showSuccessMessage(msg) {
    toast(msg, '#10b981', 3000);
  },
  showErrorMessage(msg) {
    toast(msg, '#ef4444', 4000);
  }
};

window.settingsService = settingsService;

// ===============================
// SIMPLE TOAST HELPER
// ===============================
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
// PAGE INITIALIZATION
// ===============================
(function settingsPage() {
  const $ = (s, r=document) => r.querySelector(s);

  function renderUserInfo() {
    const user = settingsService.getUserInfo();
    if (!user) {
      console.warn('⚠️ No user info found. Redirecting.');
      window.location.href = '/html/login.html';
      return;
    }

    $('#userName').textContent = `${user.firstName || ''} ${user.lastName || ''}`.trim() || 'Unknown';
    $('#userEmail').textContent = user.email || '—';
    $('#userId').textContent = user.userId || '—';
  }

  document.addEventListener('DOMContentLoaded', async () => {
    renderUserInfo();

    // Export data
    $('#exportDataBtn')?.addEventListener('click', () => settingsService.exportUserData());

    // Email notifications toggle
    const emailToggle = $('#emailNotificationToggle');
    const saveEmailBtn = $('#saveEmailPrefBtn');
    const testEmailBtn = $('#sendTestEmailBtn');

    try {
      const enabled = await settingsService.loadEmailPreference();
      emailToggle.checked = !!enabled;
    } catch (e) {
      console.warn('Failed to load email preference:', e);
    }

    saveEmailBtn?.addEventListener('click', async () => {
      try {
        await settingsService.updateEmailPreference(emailToggle.checked);
        settingsService.showSuccessMessage('Preferences updated!');
      } catch (err) {
        settingsService.showErrorMessage(err.message);
      }
    });

    testEmailBtn?.addEventListener('click', () => settingsService.sendTestEmail());
  });

  // Tiny animations for toasts
  const style = document.createElement('style');
  style.textContent = `
    @keyframes slideInRight { from { transform: translateX(100%); opacity:0; } to { transform: translateX(0); opacity:1; } }
    @keyframes slideOutRight { from { transform: translateX(0); opacity:1; } to { transform: translateX(100%); opacity:0; } }
  `;
  document.head.appendChild(style);
})();
