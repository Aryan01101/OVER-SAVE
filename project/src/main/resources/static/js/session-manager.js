// FR-6 Session Management - Frontend Auto-Logout and Timeout Detection
class SessionManager {
    constructor() {
        this.checkInterval = 30000; // Check every 30 seconds
        this.warningShown = false;
        this.checkTimer = null;
        this.lastActivity = Date.now();
        this.init();
    }

    init() {
        // Only run on protected pages (not login/signup)
        if (this.isAuthPage()) {
            return;
        }

        // Start session monitoring
        this.startSessionMonitoring();

        // Track user activity for client-side idle detection
        this.setupActivityTracking();

        // Initial session check
        this.checkSession();
    }

    isAuthPage() {
        const path = window.location.pathname;
        return path.includes('login.html') || path.includes('signup.html');
    }

    startSessionMonitoring() {
        this.checkTimer = setInterval(() => {
            this.checkSession();
        }, this.checkInterval);
    }

    setupActivityTracking() {
        // Track various user interactions
        const events = ['mousedown', 'mousemove', 'keypress', 'scroll', 'touchstart', 'click'];

        events.forEach(event => {
            document.addEventListener(event, () => {
                this.updateActivity();
            }, true);
        });
    }

    updateActivity() {
        this.lastActivity = Date.now();
        this.warningShown = false; // Reset warning if user becomes active
    }

    async checkSession() {
        const sessionToken = localStorage.getItem('sessionToken');

        if (!sessionToken) {
            this.redirectToLogin('No session token found');
            return;
        }

        try {
            // Check with our new session validation endpoint
            const response = await fetch('/api/session/validate', {
                method: 'GET',
                headers: {
                    'Authorization': `Bearer ${sessionToken}`
                }
            });

            if (response.ok) {
                const result = await response.json();

                if (!result.valid) {
                    this.redirectToLogin('Session expired');
                    return;
                }

                // Session is valid, check for idle warning
                this.checkIdleWarning();

            } else if (response.status === 401) {
                this.redirectToLogin('Session unauthorized');
            } else {
                console.warn('Session check failed:', response.status);
            }

        } catch (error) {
            console.error('Session check error:', error);
            // Don't redirect on network errors, just log
        }
    }

    checkIdleWarning() {
        const now = Date.now();
        const idleTime = now - this.lastActivity;
        const fourMinutes = 4 * 60 * 1000; // 4 minutes in milliseconds

        // Show warning if user has been idle for 4 minutes (1 minute before server timeout)
        if (idleTime > fourMinutes && !this.warningShown) {
            this.showIdleWarning();
        }
    }

    showIdleWarning() {
        this.warningShown = true;

        // Create warning modal
        const modal = document.createElement('div');
        modal.id = 'sessionWarningModal';
        modal.innerHTML = `
            <div class="session-warning-overlay">
                <div class="session-warning-modal">
                    <h3>⏰ Session Timeout Warning</h3>
                    <p>Your session will expire in <strong>1 minute</strong> due to inactivity.</p>
                    <p>Click "Stay Logged In" to continue your session.</p>
                    <div class="session-warning-buttons">
                        <button id="stayLoggedInBtn" class="btn btn-primary">Stay Logged In</button>
                        <button id="logoutNowBtn" class="btn btn-secondary">Logout Now</button>
                    </div>
                </div>
            </div>
        `;

        // Add styles
        modal.style.cssText = `
            position: fixed;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            z-index: 10000;
        `;

        document.body.appendChild(modal);

        // Add event listeners
        document.getElementById('stayLoggedInBtn').addEventListener('click', () => {
            this.renewSession();
            this.closeWarningModal();
        });

        document.getElementById('logoutNowBtn').addEventListener('click', () => {
            this.logout();
        });

        // Auto-close after 1 minute if no action
        setTimeout(() => {
            const modalStillExists = document.getElementById('sessionWarningModal');
            if (modalStillExists) {
                this.redirectToLogin('Session timed out');
            }
        }, 60000); // 1 minute
    }

    async renewSession() {
        const sessionToken = localStorage.getItem('sessionToken');

        if (!sessionToken) {
            this.redirectToLogin('No session token');
            return;
        }

        try {
            const response = await fetch('/api/session/renew', {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${sessionToken}`
                }
            });

            if (response.ok) {
                const result = await response.json();

                if (result.success) {
                    this.updateActivity(); // Reset activity timer
                    this.showSuccessMessage('Session renewed successfully');
                } else {
                    this.redirectToLogin('Session renewal failed');
                }
            } else {
                this.redirectToLogin('Session renewal failed');
            }

        } catch (error) {
            console.error('Session renewal error:', error);
            this.redirectToLogin('Session renewal error');
        }
    }

    closeWarningModal() {
        const modal = document.getElementById('sessionWarningModal');
        if (modal) {
            modal.remove();
        }
        this.warningShown = false;
    }

    showSuccessMessage(message) {
        // Create success notification
        const notification = document.createElement('div');
        notification.innerHTML = `
            <div class="session-success-notification">
                ✅ ${message}
            </div>
        `;

        notification.style.cssText = `
            position: fixed;
            top: 20px;
            right: 20px;
            background: #28a745;
            color: white;
            padding: 15px 20px;
            border-radius: 5px;
            z-index: 10001;
            box-shadow: 0 2px 10px rgba(0,0,0,0.2);
        `;

        document.body.appendChild(notification);

        // Remove after 3 seconds
        setTimeout(() => {
            notification.remove();
        }, 3000);
    }

    async logout() {
        const sessionToken = localStorage.getItem('sessionToken');

        if (sessionToken) {
            try {
                await fetch('/api/auth/logout', {
                    method: 'POST',
                    headers: {
                        'Authorization': `Bearer ${sessionToken}`
                    }
                });
            } catch (error) {
                console.error('Logout error:', error);
            }
        }

        this.cleanup();
        localStorage.removeItem('sessionToken');
        localStorage.removeItem('userInfo');
        window.location.href = '/login.html';
    }

    redirectToLogin(reason) {
        console.log('Redirecting to login:', reason);

        // Show brief message
        const notification = document.createElement('div');
        notification.innerHTML = `
            <div class="session-expired-notification">
                🔒 ${reason} - Redirecting to login...
            </div>
        `;

        notification.style.cssText = `
            position: fixed;
            top: 20px;
            right: 20px;
            background: #dc3545;
            color: white;
            padding: 15px 20px;
            border-radius: 5px;
            z-index: 10001;
            box-shadow: 0 2px 10px rgba(0,0,0,0.2);
        `;

        document.body.appendChild(notification);

        // Cleanup and redirect
        setTimeout(() => {
            this.cleanup();
            localStorage.removeItem('sessionToken');
            localStorage.removeItem('userInfo');
            window.location.href = '/login.html';
        }, 2000);
    }

    cleanup() {
        if (this.checkTimer) {
            clearInterval(this.checkTimer);
            this.checkTimer = null;
        }

        const modal = document.getElementById('sessionWarningModal');
        if (modal) {
            modal.remove();
        }
    }

    // Public method to manually check session
    static async checkSessionNow() {
        const manager = window.sessionManager;
        if (manager) {
            await manager.checkSession();
        }
    }

    // Public method to manually renew session
    static async renewSessionNow() {
        const manager = window.sessionManager;
        if (manager) {
            await manager.renewSession();
        }
    }
}

// CSS styles for session warning modal
const sessionWarningStyles = document.createElement('style');
sessionWarningStyles.textContent = `
    .session-warning-overlay {
        position: absolute;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        background: rgba(0, 0, 0, 0.7);
        display: flex;
        justify-content: center;
        align-items: center;
    }

    .session-warning-modal {
        background: white;
        padding: 30px;
        border-radius: 10px;
        box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
        max-width: 400px;
        text-align: center;
        animation: slideIn 0.3s ease-out;
    }

    .session-warning-modal h3 {
        margin: 0 0 15px 0;
        color: #e74c3c;
        font-size: 1.3em;
    }

    .session-warning-modal p {
        margin: 10px 0;
        color: #333;
        line-height: 1.5;
    }

    .session-warning-buttons {
        margin-top: 20px;
        display: flex;
        gap: 10px;
        justify-content: center;
    }

    .session-warning-buttons .btn {
        padding: 10px 20px;
        border: none;
        border-radius: 5px;
        cursor: pointer;
        font-size: 14px;
        font-weight: 500;
        transition: background-color 0.2s;
    }

    .session-warning-buttons .btn-primary {
        background: #007bff;
        color: white;
    }

    .session-warning-buttons .btn-primary:hover {
        background: #0056b3;
    }

    .session-warning-buttons .btn-secondary {
        background: #6c757d;
        color: white;
    }

    .session-warning-buttons .btn-secondary:hover {
        background: #545b62;
    }

    @keyframes slideIn {
        from {
            transform: translateY(-20px);
            opacity: 0;
        }
        to {
            transform: translateY(0);
            opacity: 1;
        }
    }
`;

document.head.appendChild(sessionWarningStyles);

// Initialize session manager when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    window.sessionManager = new SessionManager();
});

// Global functions for manual session management
window.checkSession = SessionManager.checkSessionNow;
window.renewSession = SessionManager.renewSessionNow;