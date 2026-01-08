// FR-05: Forgot Password JavaScript
class ForgotPasswordManager {
    constructor() {
        this.init();
    }

    init() {
        this.setupForm();
        this.clearAnyExistingTokens();
    }

    setupForm() {
        const form = document.getElementById('forgotPasswordForm');
        const submitBtn = document.getElementById('resetBtn');

        form.addEventListener('submit', async (e) => {
            e.preventDefault();

            const email = document.getElementById('email').value.trim();

            if (!this.validateEmail(email)) {
                return;
            }

            this.setLoading(submitBtn, true);
            this.hideMessage();

            try {
                const response = await this.sendResetRequest(email);

                if (response.success) {
                    this.showSuccessMessage(response.message);
                    form.reset();
                } else {
                    this.showErrorMessage(response.message || 'An error occurred. Please try again.');
                }
            } catch (error) {
                console.error('Forgot password error:', error);
                this.showErrorMessage('Network error. Please check your connection and try again.');
            }

            this.setLoading(submitBtn, false);
        });

        // Real-time email validation
        const emailInput = document.getElementById('email');
        emailInput.addEventListener('blur', () => {
            const email = emailInput.value.trim();
            if (email) {
                this.validateEmail(email);
            }
        });

        emailInput.addEventListener('input', () => {
            this.clearFieldError('emailError');
        });
    }

    async sendResetRequest(email) {
        const response = await fetch('/api/auth/forgot-password', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ email })
        });

        if (!response.ok) {
            throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }

        return await response.json();
    }

    validateEmail(email) {
        this.clearFieldError('emailError');

        if (!email) {
            this.showFieldError('emailError', 'Email address is required');
            return false;
        }

        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!emailRegex.test(email)) {
            this.showFieldError('emailError', 'Please enter a valid email address');
            return false;
        }

        return true;
    }

    setLoading(button, isLoading) {
        const btnText = button.querySelector('.btn-text');
        const btnLoading = button.querySelector('.btn-loading');

        if (isLoading) {
            btnText.style.display = 'none';
            btnLoading.style.display = 'flex';
            button.disabled = true;
        } else {
            btnText.style.display = 'block';
            btnLoading.style.display = 'none';
            button.disabled = false;
        }
    }

    showSuccessMessage(message) {
        const container = document.getElementById('message-container');
        const messageEl = document.getElementById('message');

        messageEl.textContent = message;
        messageEl.className = 'message success';
        container.style.display = 'block';

        // Scroll to message
        container.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }

    showErrorMessage(message) {
        const container = document.getElementById('message-container');
        const messageEl = document.getElementById('message');

        messageEl.textContent = message;
        messageEl.className = 'message error';
        container.style.display = 'block';

        // Scroll to message
        container.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }

    hideMessage() {
        const container = document.getElementById('message-container');
        container.style.display = 'none';
    }

    showFieldError(fieldId, message) {
        const errorElement = document.getElementById(fieldId);
        if (errorElement) {
            errorElement.textContent = message;
            errorElement.style.display = 'block';
        }
    }

    clearFieldError(fieldId) {
        const errorElement = document.getElementById(fieldId);
        if (errorElement) {
            errorElement.textContent = '';
            errorElement.style.display = 'none';
        }
    }

    clearAnyExistingTokens() {
        // Clear any existing auth tokens since user is resetting password
        localStorage.removeItem('sessionToken');
        localStorage.removeItem('userInfo');
    }
}

// Initialize when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    new ForgotPasswordManager();
});

// Add some additional CSS for message styling
const style = document.createElement('style');
style.textContent = `
    .message {
        padding: 15px;
        border-radius: 8px;
        margin-bottom: 20px;
        font-weight: 500;
        line-height: 1.5;
    }

    .message.success {
        background: #d4edda;
        color: #155724;
        border: 1px solid #c3e6cb;
    }

    .message.error {
        background: #f8d7da;
        color: #721c24;
        border: 1px solid #f5c6cb;
    }

    .loading-container {
        text-align: center;
        padding: 40px 20px;
    }

    .loading-container .spinner {
        width: 40px;
        height: 40px;
        margin: 0 auto 20px;
    }

    .error-container {
        text-align: center;
        padding: 40px 20px;
    }

    .error-icon {
        font-size: 3rem;
        margin-bottom: 20px;
    }

    .success-container {
        text-align: center;
        padding: 40px 20px;
    }

    .success-icon {
        font-size: 3rem;
        margin-bottom: 20px;
    }

    .security-info {
        margin-top: 30px;
        padding: 20px;
        background: #f8f9fa;
        border-radius: 8px;
        border: 1px solid #e9ecef;
    }

    .security-item {
        display: flex;
        align-items: center;
        margin: 10px 0;
        font-size: 14px;
        color: #6c757d;
    }

    .security-icon {
        margin-right: 10px;
        font-size: 16px;
    }

    .btn-loading {
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 8px;
    }

    .spinner {
        width: 20px;
        height: 20px;
        border: 2px solid #ffffff33;
        border-top: 2px solid #ffffff;
        border-radius: 50%;
        animation: spin 1s linear infinite;
    }

    @keyframes spin {
        0% { transform: rotate(0deg); }
        100% { transform: rotate(360deg); }
    }
`;

document.head.appendChild(style);