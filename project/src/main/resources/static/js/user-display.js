// User Display Manager - Dynamically loads user info on all pages
class UserDisplayManager {
    constructor() {
        this.init();
    }

    init() {
        // Only run on authenticated pages (not login/signup)
        if (this.isAuthPage()) {
            return;
        }

        // Load user info when DOM is ready
        this.loadUserInfo();
    }

    isAuthPage() {
        const path = window.location.pathname;
        return path.includes('login.html') || path.includes('signup.html');
    }

    loadUserInfo() {
        try {
            // Get user info from localStorage (set during login)
            const userInfo = this.getUserInfo();

            if (userInfo) {
                this.updateUserDisplay(userInfo);
            } else {
                // If no user info, redirect to login
                console.warn('No user info found, redirecting to login');
                window.location.href = '/login.html';
            }
        } catch (error) {
            console.error('Error loading user info:', error);
            // On error, redirect to login for security
            window.location.href = '/login.html';
        }
    }

    getUserInfo() {
        const userInfoStr = localStorage.getItem('userInfo');
        return userInfoStr ? JSON.parse(userInfoStr) : null;
    }

    updateUserDisplay(userInfo) {
        // Update user name elements (both sidebar and header)
        const userNameElements = document.querySelectorAll('.user-name, .header-user-name');
        const fullName = `${userInfo.firstName} ${userInfo.lastName}`;
        userNameElements.forEach(el => {
            el.textContent = fullName;
        });

        // Update user email elements (both sidebar and header)
        const userEmailElements = document.querySelectorAll('.user-email, .header-user-email');
        userEmailElements.forEach(el => {
            el.textContent = userInfo.email;
        });

        // Update user avatar initials (both sidebar and header)
        const userAvatarElements = document.querySelectorAll('.user-avatar, .header-user-avatar');
        const initials = (userInfo.firstName.charAt(0) + userInfo.lastName.charAt(0)).toUpperCase();
        userAvatarElements.forEach(el => {
            el.textContent = initials;
        });

        // Update welcome messages if they exist
        const welcomeElements = document.querySelectorAll('.welcome-text, .welcome-message');
        welcomeElements.forEach(el => {
            if (el.innerHTML.includes('Welcome')) {
                el.innerHTML = `Welcome back, ${userInfo.firstName}! 👋`;
            }
        });

        console.log('User display updated:', fullName);
    }

    // Static method for easy access
    static updateUserInfo() {
        const manager = new UserDisplayManager();
        manager.loadUserInfo();
    }

    // Method to refresh user info (useful after profile updates)
    static refreshUserInfo() {
        const manager = new UserDisplayManager();
        manager.loadUserInfo();
    }
}

// Auto-initialize when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    new UserDisplayManager();
});

// Global function for manual refresh
window.refreshUserDisplay = UserDisplayManager.refreshUserInfo;