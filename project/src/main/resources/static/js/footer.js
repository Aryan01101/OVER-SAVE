// Footer Component
// Automatically adds footer to all pages

function initializeFooter() {
    const currentYear = new Date().getFullYear();

    const footerHTML = `
        <footer class="footer">
            <div class="footer-simple">
                <div class="footer-logo">
                    <span>💰</span>
                    <span>OVER-SAVE</span>
                </div>
                <p class="footer-tagline">Track smarter. Save better. Live wealthier.</p>
                <p class="footer-copyright">© ${currentYear} OVER-SAVE. All rights reserved.</p>
            </div>
        </footer>
    `;

    // Find the main content element
    const mainContent = document.querySelector('.main-content');

    if (mainContent) {
        // Create a temporary div to parse HTML
        const tempDiv = document.createElement('div');
        tempDiv.innerHTML = footerHTML;

        // Append the footer to main content
        mainContent.appendChild(tempDiv.firstElementChild);
    }
}

// Initialize footer when DOM is loaded
document.addEventListener('DOMContentLoaded', initializeFooter);
