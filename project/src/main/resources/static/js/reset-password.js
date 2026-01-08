// FR-05: Reset Password JavaScript
class ResetPasswordManager {
    constructor() {
        this.token = null;
        this.passwordRequirements = {
            length: false,
            uppercase: false,
            number: false,
            special: false
        };
        this.init();
    }

    init() {
        this.extractTokenFromURL();
        this.validateToken();
    }

    extractTokenFromURL() {
        const urlParams = new URLSearchParams(window.location.search);
        this.token = urlParams.get('token');

        if (!this.token) {
            this.showError('No reset token found in URL', 'Invalid reset link. Please request a new password reset.');
            return;
        }
    }

    async validateToken() {
        if (!this.token) {
            return;
        }

        try {
            const response = await fetch(`/api/auth/reset-password/${this.token}`, {
                method: 'GET'
            });

            const result = await response.json();

            if (result.success) {
                this.showForm();
            } else {
                this.showError('Invalid or expired token', result.message || 'This reset link is no longer valid.');
            }
        } catch (error) {
            console.error('Token validation error:', error);
            this.showError('Network error', 'Unable to validate reset link. Please check your connection.');
        }
    }

    showForm() {
        document.getElementById('loading-container').style.display = 'none';
        document.getElementById('form-container').style.display = 'block';
        this.setupForm();
    }

    showError(title, message) {
        document.getElementById('loading-container').style.display = 'none';
        document.getElementById('error-container').style.display = 'block';
        document.getElementById('error-message').textContent = message;
    }

    setupForm() {
        const form = document.getElementById('resetPasswordForm');
        const submitBtn = document.getElementById('resetBtn');
        const passwordInput = document.getElementById('password');
        const confirmPasswordInput = document.getElementById('confirmPassword');

        // Password strength checking
        passwordInput.addEventListener('input', (e) => {
            this.checkPasswordStrength(e.target.value);
            this.clearFieldError('passwordError');
        });

        // Confirm password validation
        confirmPasswordInput.addEventListener('input', () => {
            this.validatePasswordMatch();
            this.clearFieldError('confirmPasswordError');
        });

        // Form submission
        form.addEventListener('submit', async (e) => {
            e.preventDefault();

            const password = passwordInput.value;
            const confirmPassword = confirmPasswordInput.value;

            if (!this.validateForm(password, confirmPassword)) {
                return;
            }

            this.setLoading(submitBtn, true);
            this.hideMessage();

            try {
                const response = await this.resetPassword(password);

                if (response.success) {
                    this.showSuccess(response.message);
                } else {
                    this.showErrorMessage(response.message || 'Password reset failed. Please try again.');
                }
            } catch (error) {
                console.error('Reset password error:', error);
                this.showErrorMessage('Network error. Please check your connection and try again.');
            }

            this.setLoading(submitBtn, false);
        });
    }

    async resetPassword(newPassword) {
        const response = await fetch('/api/auth/reset-password', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                token: this.token,
                newPassword: newPassword
            })
        });

        if (!response.ok) {
            throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }

        return await response.json();
    }

    validateForm(password, confirmPassword) {
        let isValid = true;

        // Validate password strength
        if (!this.validatePasswordStrength(password)) {
            isValid = false;
        }

        // Validate password match
        if (!this.validatePasswordMatch()) {
            isValid = false;
        }

        return isValid;
    }

    validatePasswordStrength(password) {
        const requirements = this.checkPasswordStrength(password);

        if (!requirements.length) {
            this.showFieldError('passwordError', 'Password must be at least 12 characters long');
            return false;
        }

        if (!requirements.uppercase) {
            this.showFieldError('passwordError', 'Password must contain at least one uppercase letter');
            return false;
        }

        if (!requirements.number) {
            this.showFieldError('passwordError', 'Password must contain at least one number');
            return false;
        }

        if (!requirements.special) {
            this.showFieldError('passwordError', 'Password must contain at least one special character');
            return false;
        }

        // Check for common patterns
        if (password.toLowerCase().includes('password') ||
            password.toLowerCase().includes('123456') ||
            password.toLowerCase().includes('qwerty')) {
            this.showFieldError('passwordError', 'Password cannot contain common patterns');
            return false;
        }

        return true;
    }

    checkPasswordStrength(password) {
        const requirements = {
            length: password.length >= 12,
            uppercase: /[A-Z]/.test(password),
            number: /[0-9]/.test(password),
            special: /[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>/?]/.test(password)
        };

        // Update visual indicators
        this.updateRequirementIndicator('req-length', requirements.length);
        this.updateRequirementIndicator('req-uppercase', requirements.uppercase);
        this.updateRequirementIndicator('req-number', requirements.number);
        this.updateRequirementIndicator('req-special', requirements.special);

        // Show strength indicator
        const strengthIndicator = document.getElementById('passwordStrength');
        if (password) {
            strengthIndicator.style.display = 'block';
            const score = Object.values(requirements).filter(Boolean).length;

            if (score < 2) {
                strengthIndicator.className = 'password-strength weak';
                strengthIndicator.textContent = '⚠️ Weak password';
            } else if (score < 4) {
                strengthIndicator.className = 'password-strength medium';
                strengthIndicator.textContent = '🟡 Medium strength';
            } else {
                strengthIndicator.className = 'password-strength strong';
                strengthIndicator.textContent = '✅ Strong password';
            }
        } else {
            strengthIndicator.style.display = 'none';
        }

        this.passwordRequirements = requirements;
        return requirements;
    }

    updateRequirementIndicator(elementId, isMet) {
        const element = document.getElementById(elementId);
        if (element) {
            element.className = isMet ? 'requirement-met' : '';
            element.style.color = isMet ? '#28a745' : '#6c757d';
        }
    }

    validatePasswordMatch() {
        const password = document.getElementById('password').value;
        const confirmPassword = document.getElementById('confirmPassword').value;

        if (confirmPassword && password !== confirmPassword) {
            this.showFieldError('confirmPasswordError', 'Passwords do not match');
            return false;
        } else {
            this.clearFieldError('confirmPasswordError');
            return true;
        }
    }

    showSuccess(message) {
        document.getElementById('form-container').style.display = 'none';
        document.getElementById('success-container').style.display = 'block';

        // Clear any stored tokens for security
        localStorage.removeItem('sessionToken');
        localStorage.removeItem('userInfo');
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
}

// Password toggle functionality
function togglePassword(inputId) {
    const input = document.getElementById(inputId);
    const button = input.parentElement.querySelector('.password-toggle');
    const showIcon = button.querySelector('.toggle-show');
    const hideIcon = button.querySelector('.toggle-hide');

    if (input.type === 'password') {
        input.type = 'text';
        showIcon.style.display = 'none';
        hideIcon.style.display = 'inline';
    } else {
        input.type = 'password';
        showIcon.style.display = 'inline';
        hideIcon.style.display = 'none';
    }
}

// Initialize when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    new ResetPasswordManager();
});

// Add CSS for password requirements and indicators
const style = document.createElement('style');
style.textContent = `
    .password-requirements {
        margin: 15px 0;
        padding: 15px;
        background: #f8f9fa;
        border-radius: 8px;
        border: 1px solid #e9ecef;
    }

    .password-requirements h4 {
        margin: 0 0 10px 0;
        font-size: 14px;
        color: #495057;
    }

    .password-requirements ul {
        margin: 0;
        padding-left: 20px;
        list-style: none;
    }

    .password-requirements li {
        margin: 5px 0;
        font-size: 14px;
        color: #6c757d;
        position: relative;
    }

    .password-requirements li:before {
        content: "○";
        position: absolute;
        left: -20px;
    }

    .password-requirements li.requirement-met:before {
        content: "✓";
        color: #28a745;
    }

    .password-strength {
        margin-top: 8px;
        padding: 8px 12px;
        border-radius: 4px;
        font-size: 14px;
        font-weight: 500;
    }

    .password-strength.weak {
        background: #f8d7da;
        color: #721c24;
        border: 1px solid #f5c6cb;
    }

    .password-strength.medium {
        background: #fff3cd;
        color: #856404;
        border: 1px solid #ffeaa7;
    }

    .password-strength.strong {
        background: #d4edda;
        color: #155724;
        border: 1px solid #c3e6cb;
    }

    .password-input-container {
        position: relative;
    }

    .password-toggle {
        position: absolute;
        right: 12px;
        top: 50%;
        transform: translateY(-50%);
        background: none;
        border: none;
        cursor: pointer;
        padding: 4px;
        border-radius: 4px;
        transition: background-color 0.2s;
    }

    .password-toggle:hover {
        background: #f8f9fa;
    }

    .security-note {
        margin-top: 20px;
        padding: 15px;
        background: #fff3cd;
        border: 1px solid #ffeaa7;
        border-radius: 8px;
        font-size: 14px;
    }

    .message {
        padding: 15px;
        border-radius: 8px;
        margin-bottom: 20px;
        font-weight: 500;
        line-height: 1.5;
    }

    .message.error {
        background: #f8d7da;
        color: #721c24;
        border: 1px solid #f5c6cb;
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