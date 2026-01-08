// Authentication JavaScript

class AuthManager {
    constructor() {
        this.init();
    }

    init() {
        // Check if we're on login or signup page
        const loginForm = document.getElementById('loginForm');
        const signupForm = document.getElementById('signupForm');

        if (loginForm) {
            this.setupLoginForm();
        }

        if (signupForm) {
            this.setupSignupForm();
        }

        // Setup IdP buttons
        this.setupIdpButtons();
    }

    setupLoginForm() {
        const form = document.getElementById('loginForm');
        const loginBtn = document.getElementById('loginBtn');

        form.addEventListener('submit', async (e) => {
            e.preventDefault();

            const email = document.getElementById('email').value;
            const password = document.getElementById('password').value;

            if (!this.validateLogin(email, password)) {
                return;
            }

            this.setLoading(loginBtn, true);

            try {
                const response = await this.login(email, password);

                if (response.sessionToken) {
                    // Store session token
                    localStorage.setItem('sessionToken', response.sessionToken);
                    localStorage.setItem('userInfo', JSON.stringify({
                        userId: response.userId,
                        email: response.email,
                        firstName: response.firstName,
                        lastName: response.lastName
                    }));

                    // Verify token was stored before redirecting
                    const storedToken = localStorage.getItem('sessionToken');
                    if (!storedToken) {
                        console.error('❌ Failed to store session token in localStorage');
                        this.showError('Authentication error. Please try again.');
                        return;
                    }

                    console.log('✅ Login successful - Token stored:', response.sessionToken.substring(0, 10) + '...');
                    console.log('✅ Token verified in localStorage:', storedToken.substring(0, 10) + '...');
                    console.log('🔄 Redirecting to:', response.redirectUrl || '/html/oversave-dashboard.html');

                    // Redirect to dashboard
                    window.location.href = response.redirectUrl || '/html/oversave-dashboard.html';
                } else {
                    this.showError(response.message || 'Login failed');
                }
            } catch (error) {
                console.error('Login error:', error);
                this.showError('An error occurred. Please try again.');
            }

            this.setLoading(loginBtn, false);
        });
    }

    setupSignupForm() {
        const form = document.getElementById('signupForm');
        const signupBtn = document.getElementById('signupBtn');
        const passwordInput = document.getElementById('password');
        const confirmPasswordInput = document.getElementById('confirmPassword');

        // Password strength checking
        passwordInput.addEventListener('input', (e) => {
            this.checkPasswordStrength(e.target.value);
        });

        // Confirm password validation
        confirmPasswordInput.addEventListener('input', (e) => {
            this.validatePasswordMatch();
        });

        form.addEventListener('submit', async (e) => {
            e.preventDefault();

            const formData = new FormData(form);
            const signupData = {
                firstName: formData.get('firstName'),
                lastName: formData.get('lastName'),
                middleName: formData.get('middleName') || null,
                email: formData.get('email'),
                password: formData.get('password'),
                allowNotificationEmail: formData.get('allowNotificationEmail') === 'on'
            };

            if (!this.validateSignup(signupData)) {
                return;
            }

            this.setLoading(signupBtn, true);

            try {
                const response = await this.signup(signupData);

                if (response.sessionToken) {
                    // Store session token
                    localStorage.setItem('sessionToken', response.sessionToken);
                    localStorage.setItem('userInfo', JSON.stringify({
                        userId: response.userId,
                        email: response.email,
                        firstName: response.firstName,
                        lastName: response.lastName
                    }));

                    // Verify token was stored before redirecting
                    const storedToken = localStorage.getItem('sessionToken');
                    if (!storedToken) {
                        console.error('❌ Failed to store session token in localStorage');
                        this.showError('Authentication error. Please try again.');
                        return;
                    }

                    console.log('✅ Signup successful - Token stored:', response.sessionToken.substring(0, 10) + '...');
                    console.log('✅ Token verified in localStorage:', storedToken.substring(0, 10) + '...');
                    console.log('🔄 Redirecting to:', response.redirectUrl || '/html/oversave-dashboard.html');

                    // Redirect to dashboard
                    window.location.href = response.redirectUrl || '/html/oversave-dashboard.html';
                } else {
                    this.showError(response.message || 'Registration failed');
                }
            } catch (error) {
                console.error('Signup error:', error);
                this.showError('An error occurred. Please try again.');
            }

            this.setLoading(signupBtn, false);
        });
    }

    setupIdpButtons() {
        const googleLoginBtn = document.getElementById('googleLogin');
        const googleSignupBtn = document.getElementById('googleSignup');

        if (googleLoginBtn) {
            googleLoginBtn.addEventListener('click', () => {
                this.initiateGoogleAuth('login');
            });
        }

        if (googleSignupBtn) {
            googleSignupBtn.addEventListener('click', () => {
                this.initiateGoogleAuth('signup');
            });
        }
    }

    validateLogin(email, password) {
        this.clearErrors();
        let isValid = true;

        if (!email) {
            this.showFieldError('emailError', 'Email is required');
            isValid = false;
        } else if (!this.isValidEmail(email)) {
            this.showFieldError('emailError', 'Please enter a valid email');
            isValid = false;
        }

        if (!password) {
            this.showFieldError('passwordError', 'Password is required');
            isValid = false;
        }

        return isValid;
    }

    validateSignup(data) {
        this.clearErrors();
        let isValid = true;

        if (!data.firstName) {
            this.showFieldError('firstNameError', 'First name is required');
            isValid = false;
        }

        if (!data.lastName) {
            this.showFieldError('lastNameError', 'Last name is required');
            isValid = false;
        }

        if (!data.email) {
            this.showFieldError('emailError', 'Email is required');
            isValid = false;
        } else if (!this.isValidEmail(data.email)) {
            this.showFieldError('emailError', 'Please enter a valid email');
            isValid = false;
        }

        if (!data.password) {
            this.showFieldError('passwordError', 'Password is required');
            isValid = false;
        } else if (data.password.length < 8) {
            this.showFieldError('passwordError', 'Password must be at least 8 characters');
            isValid = false;
        }

        if (!this.validatePasswordMatch()) {
            isValid = false;
        }

        if (!document.getElementById('agreeTerms').checked) {
            this.showError('You must agree to the Terms of Service and Privacy Policy');
            isValid = false;
        }

        return isValid;
    }

    validatePasswordMatch() {
        const password = document.getElementById('password').value;
        const confirmPassword = document.getElementById('confirmPassword').value;

        if (confirmPassword && password !== confirmPassword) {
            this.showFieldError('confirmPasswordError', 'Passwords do not match');
            return false;
        } else {
            this.showFieldError('confirmPasswordError', '');
            return true;
        }
    }

    checkPasswordStrength(password) {
        const strengthIndicator = document.getElementById('passwordStrength');

        if (!password) {
            strengthIndicator.style.display = 'none';
            return;
        }

        let strength = 0;
        if (password.length >= 8) strength++;
        if (/[A-Z]/.test(password)) strength++;
        if (/[a-z]/.test(password)) strength++;
        if (/[0-9]/.test(password)) strength++;
        if (/[^A-Za-z0-9]/.test(password)) strength++;

        strengthIndicator.style.display = 'block';
        strengthIndicator.className = 'password-strength';

        if (strength < 2) {
            strengthIndicator.className += ' weak';
            strengthIndicator.textContent = 'Weak password';
        } else if (strength < 4) {
            strengthIndicator.className += ' medium';
            strengthIndicator.textContent = 'Medium strength';
        } else {
            strengthIndicator.className += ' strong';
            strengthIndicator.textContent = 'Strong password';
        }
    }

    async login(email, password) {
        const response = await fetch('/api/auth/login', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ email, password })
        });

        return await response.json();
    }

    async signup(signupData) {
        const response = await fetch('/api/auth/signup', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(signupData)
        });

        return await response.json();
    }

    async loginWithIdp(provider, idToken, subjectId) {
        const response = await fetch('/api/auth/login/idp', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                provider,
                idToken,
                subjectId
            })
        });

        return await response.json();
    }

    initiateGoogleAuth(mode) {
        // Redirect to Spring Security OAuth2 endpoint
        // Spring will handle the OAuth flow with Google
        window.location.href = '/oauth2/authorization/google';
    }

    isValidEmail(email) {
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return emailRegex.test(email);
    }

    setLoading(button, isLoading) {
        const btnText = button.querySelector('.btn-text');
        const btnLoading = button.querySelector('.btn-loading');

        if (isLoading) {
            btnText.style.display = 'none';
            btnLoading.style.display = 'block';
            button.disabled = true;
        } else {
            btnText.style.display = 'block';
            btnLoading.style.display = 'none';
            button.disabled = false;
        }
    }

    showError(message) {
        const errorElement = document.getElementById('formError');
        errorElement.textContent = message;
        errorElement.style.display = 'block';
    }

    showFieldError(fieldId, message) {
        const errorElement = document.getElementById(fieldId);
        if (errorElement) {
            errorElement.textContent = message;
        }
    }

    clearErrors() {
        const errorElements = document.querySelectorAll('.error-message');
        errorElements.forEach(element => {
            element.textContent = '';
        });

        const formError = document.getElementById('formError');
        if (formError) {
            formError.style.display = 'none';
        }
    }

    // Session management
    static getSessionToken() {
        return localStorage.getItem('sessionToken');
    }

    static getUserInfo() {
        const userInfo = localStorage.getItem('userInfo');
        return userInfo ? JSON.parse(userInfo) : null;
    }

    static async logout() {
        const sessionToken = this.getSessionToken();

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

        localStorage.removeItem('sessionToken');
        localStorage.removeItem('userInfo');
        window.location.href = '/html/login.html';
    }

    static async validateSession() {
        const sessionToken = this.getSessionToken();

        if (!sessionToken) {
            return false;
        }

        try {
            const response = await fetch('/api/auth/validate', {
                headers: {
                    'Authorization': `Bearer ${sessionToken}`
                }
            });

            if (response.ok) {
                const isValid = await response.json();
                return isValid;
            }
        } catch (error) {
            console.error('Session validation error:', error);
        }

        return false;
    }
}

// OAuth callback handler - extracts session info from URL parameters
function handleOAuthCallback() {
    const urlParams = new URLSearchParams(window.location.search);
    const sessionToken = urlParams.get('sessionToken');
    const userId = urlParams.get('userId');
    const email = urlParams.get('email');
    const firstName = urlParams.get('firstName');
    const lastName = urlParams.get('lastName');

    if (sessionToken && userId && email) {
        console.log('🔐 OAuth callback detected - storing session info');

        // Store session token
        localStorage.setItem('sessionToken', sessionToken);
        localStorage.setItem('userInfo', JSON.stringify({
            userId: parseInt(userId),
            email: email,
            firstName: firstName || '',
            lastName: lastName || ''
        }));

        console.log('✅ OAuth session stored successfully');
        console.log('✅ Session token:', sessionToken.substring(0, 10) + '...');

        // Clean up URL by removing query parameters
        const cleanUrl = window.location.origin + window.location.pathname;
        window.history.replaceState({}, document.title, cleanUrl);

        return true;
    }

    return false;
}

// Initialize auth manager when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    // First, check if this is an OAuth callback
    handleOAuthCallback();

    // Then initialize auth manager
    new AuthManager();
});

// Global logout function - wrapped in arrow function to preserve context
window.logout = () => AuthManager.logout();