// Sidebar toggle functionality
function toggleSidebar() {
    const sidebar = document.querySelector('.sidebar');
    const mainContent = document.querySelector('.main-content');
    const toggleIcon = document.getElementById('toggle-icon');

    if (sidebar.classList.contains('collapsed')) {
        sidebar.classList.remove('collapsed');
        mainContent.style.marginLeft = '280px';
        toggleIcon.textContent = '☰'; // hamburger menu
        localStorage.setItem('sidebarCollapsed', 'false');
    } else {
        sidebar.classList.add('collapsed');
        mainContent.style.marginLeft = '60px';
        toggleIcon.textContent = '☰'; // hamburger menu
        localStorage.setItem('sidebarCollapsed', 'true');
    }
}

// Initialize sidebar state on page load
document.addEventListener('DOMContentLoaded', function() {
    const sidebar = document.querySelector('.sidebar');
    const mainContent = document.querySelector('.main-content');
    const toggleIcon = document.getElementById('toggle-icon');
    const isCollapsed = localStorage.getItem('sidebarCollapsed') === 'true';

    if (isCollapsed) {
        sidebar.classList.add('collapsed');
        mainContent.style.marginLeft = '60px';
        toggleIcon.textContent = '☰';
    } else {
        mainContent.style.marginLeft = '280px';
        toggleIcon.textContent = '☰';
    }
});

// Keyboard shortcut for sidebar toggle (Ctrl/Cmd + B)
document.addEventListener('keydown', function(e) {
    if ((e.ctrlKey || e.metaKey) && e.key === 'b') {
        e.preventDefault();
        toggleSidebar();
    }
});