// CLIMS Frontend Application
const app = {
  apiBase: 'http://127.0.0.1:8080',
  token: null,
  
  // Initialize the application
  init() {
    console.log('Initializing CLIMS Frontend...');
    this.token = localStorage.getItem('accessToken');
    this.setupEventListeners();
    this.checkAuthState();
  },
  
  // Setup event listeners
  setupEventListeners() {
    document.getElementById('login-form')?.addEventListener('submit', (e) => {
      e.preventDefault();
      this.login();
    });
    
    document.getElementById('logout-btn')?.addEventListener('click', () => {
      this.logout();
    });
    
    document.getElementById('call-hello-btn')?.addEventListener('click', () => {
      this.callHello();
    });
    
    // Tab navigation
    document.querySelectorAll('[data-bs-toggle="tab"]').forEach(tab => {
      tab.addEventListener('shown.bs.tab', (e) => {
        const targetId = e.target.getAttribute('data-bs-target');
        console.log('Switched to tab:', targetId);
      });
    });
  },
  
  // Check authentication state
  checkAuthState() {
    if (this.token) {
      this.showApp();
    } else {
      this.showLogin();
    }
  },
  
  // Show login section
  showLogin() {
    document.getElementById('login-section')?.classList.remove('d-none');
    document.getElementById('main-app')?.classList.add('d-none');
    document.getElementById('user-info')?.classList.add('d-none');
  },
  
  // Show main app
  showApp() {
    document.getElementById('login-section')?.classList.add('d-none');
    document.getElementById('main-app')?.classList.remove('d-none');
    document.getElementById('user-info')?.classList.remove('d-none');
    
    // Decode JWT to get username
    if (this.token) {
      try {
        const payload = JSON.parse(atob(this.token.split('.')[1]));
        document.getElementById('username-display').textContent = payload.sub || 'User';
      } catch (e) {
        console.error('Failed to decode token:', e);
      }
    }
  },
  
  // Login
  async login() {
    const username = document.getElementById('username').value;
    const password = document.getElementById('password').value;
    
    if (!username || !password) {
      this.showMessage('Please enter both username and password', 'warning');
      return;
    }
    
    const loginBtn = document.getElementById('login-btn');
    loginBtn.disabled = true;
    loginBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Logging in...';
    
    try {
      const response = await fetch(`${this.apiBase}/api/auth/login`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ username, password })
      });
      
      if (!response.ok) {
        const error = await response.json().catch(() => ({ message: 'Login failed' }));
        throw new Error(error.message || `HTTP ${response.status}`);
      }
      
      const data = await response.json();
      
      // Store token - try different field names for compatibility
      this.token = data.accessToken || data.access_token || data.token;
      
      if (!this.token) {
        throw new Error('No access token in response');
      }
      
      localStorage.setItem('accessToken', this.token);
      
      if (data.refreshToken) {
        localStorage.setItem('refreshToken', data.refreshToken);
      }
      
      this.showMessage('Login successful!', 'success');
      this.showApp();
      
      // Clear the form
      document.getElementById('login-form').reset();
      
    } catch (error) {
      console.error('Login error:', error);
      this.showMessage(`Login failed: ${error.message}`, 'danger');
    } finally {
      loginBtn.disabled = false;
      loginBtn.textContent = 'Login';
    }
  },
  
  // Logout
  logout() {
    this.token = null;
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    this.showMessage('Logged out successfully', 'info');
    this.showLogin();
  },
  
  // Call /api/hello endpoint
  async callHello() {
    const btn = document.getElementById('call-hello-btn');
    const resultDiv = document.getElementById('hello-result');
    
    btn.disabled = true;
    btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Calling...';
    resultDiv.innerHTML = '';
    
    try {
      const headers = {
        'Content-Type': 'application/json'
      };
      
      if (this.token) {
        headers['Authorization'] = `Bearer ${this.token}`;
      }
      
      const response = await fetch(`${this.apiBase}/api/hello`, {
        method: 'GET',
        headers: headers
      });
      
      const data = await response.text();
      
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${data}`);
      }
      
      resultDiv.innerHTML = `
        <div class="alert alert-success">
          <strong>Success!</strong> Status: ${response.status}<br>
          <strong>Response:</strong> ${data}
        </div>
      `;
      
    } catch (error) {
      console.error('API call error:', error);
      resultDiv.innerHTML = `
        <div class="alert alert-danger">
          <strong>Error!</strong> ${error.message}
        </div>
      `;
    } finally {
      btn.disabled = false;
      btn.textContent = 'Call /api/hello';
    }
  },
  
  // Show message toast
  showMessage(message, type = 'info') {
    const toastContainer = document.getElementById('toast-container');
    if (!toastContainer) return;
    
    const toastId = `toast-${Date.now()}`;
    const bgClass = `bg-${type}`;
    const textClass = type === 'warning' ? 'text-dark' : 'text-white';
    
    const toastHtml = `
      <div id="${toastId}" class="toast ${textClass} ${bgClass}" role="alert" aria-live="assertive" aria-atomic="true">
        <div class="toast-header ${bgClass} ${textClass}">
          <strong class="me-auto">CLIMS</strong>
          <button type="button" class="btn-close btn-close-white" data-bs-dismiss="toast"></button>
        </div>
        <div class="toast-body">
          ${message}
        </div>
      </div>
    `;
    
    toastContainer.insertAdjacentHTML('beforeend', toastHtml);
    
    const toastElement = document.getElementById(toastId);
    const toast = new bootstrap.Toast(toastElement, { delay: 3000 });
    toast.show();
    
    // Remove from DOM after hidden
    toastElement.addEventListener('hidden.bs.toast', () => {
      toastElement.remove();
    });
  }
};

// Initialize when DOM is ready
document.addEventListener('DOMContentLoaded', () => {
  app.init();
});
