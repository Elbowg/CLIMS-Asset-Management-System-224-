/**
 * CLIMS Frontend Application
 * Handles authentication, JWT token management, and API calls
 */

// Configuration
const config = {
  // Try to auto-detect API base URL, default to localhost:8080
  apiBase: window.location.protocol === 'file:' 
    ? 'http://localhost:8080' 
    : `${window.location.protocol}//${window.location.hostname}:8080`,
  tokenKey: 'clims_access_token',
  refreshTokenKey: 'clims_refresh_token',
  userInfoKey: 'clims_user_info'
};

// State management
const state = {
  accessToken: null,
  refreshToken: null,
  userInfo: null
};

// Utility functions
const showToast = (title, message, type = 'info') => {
  const toastEl = document.getElementById('liveToast');
  const toastTitle = document.getElementById('toastTitle');
  const toastBody = document.getElementById('toastBody');
  
  toastTitle.textContent = title;
  toastBody.textContent = message;
  
  // Remove old type classes and add new one
  toastEl.classList.remove('success', 'error', 'warning', 'info');
  toastEl.classList.add(type);
  
  const toast = new bootstrap.Toast(toastEl);
  toast.show();
};

const setLoading = (element, isLoading) => {
  if (isLoading) {
    element.classList.add('loading');
    element.disabled = true;
  } else {
    element.classList.remove('loading');
    element.disabled = false;
  }
};

// Authentication functions
const saveTokens = (accessToken, refreshToken, userInfo) => {
  state.accessToken = accessToken;
  state.refreshToken = refreshToken;
  state.userInfo = userInfo;
  
  localStorage.setItem(config.tokenKey, accessToken);
  localStorage.setItem(config.refreshTokenKey, refreshToken);
  localStorage.setItem(config.userInfoKey, JSON.stringify(userInfo));
};

const loadTokens = () => {
  state.accessToken = localStorage.getItem(config.tokenKey);
  state.refreshToken = localStorage.getItem(config.refreshTokenKey);
  const userInfoStr = localStorage.getItem(config.userInfoKey);
  state.userInfo = userInfoStr ? JSON.parse(userInfoStr) : null;
  
  return state.accessToken !== null;
};

const clearTokens = () => {
  state.accessToken = null;
  state.refreshToken = null;
  state.userInfo = null;
  
  localStorage.removeItem(config.tokenKey);
  localStorage.removeItem(config.refreshTokenKey);
  localStorage.removeItem(config.userInfoKey);
};

const isAuthenticated = () => {
  return state.accessToken !== null;
};

// API call wrapper
const apiCall = async (endpoint, options = {}) => {
  const url = `${config.apiBase}${endpoint}`;
  const headers = {
    'Content-Type': 'application/json',
    ...options.headers
  };
  
  // Add authorization header if authenticated
  if (isAuthenticated() && !options.skipAuth) {
    headers['Authorization'] = `Bearer ${state.accessToken}`;
  }
  
  try {
    const response = await fetch(url, {
      ...options,
      headers
    });
    
    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      throw new Error(errorData.message || `HTTP ${response.status}: ${response.statusText}`);
    }
    
    // Handle empty responses
    const contentType = response.headers.get('content-type');
    if (contentType && contentType.includes('application/json')) {
      return await response.json();
    } else {
      return await response.text();
    }
  } catch (error) {
    console.error('API call failed:', error);
    throw error;
  }
};

// Login function
const login = async (username, password) => {
  try {
    const response = await apiCall('/api/auth/login', {
      method: 'POST',
      skipAuth: true,
      body: JSON.stringify({ username, password })
    });
    
    // Handle different response formats
    const accessToken = response.accessToken || response.access_token;
    const refreshToken = response.refreshToken || response.refresh_token;
    
    if (!accessToken) {
      throw new Error('Invalid response: missing access token');
    }
    
    saveTokens(accessToken, refreshToken, { username });
    return true;
  } catch (error) {
    console.error('Login failed:', error);
    throw error;
  }
};

// Logout function
const logout = () => {
  clearTokens();
  updateUI();
  showToast('Logged Out', 'You have been logged out successfully', 'info');
};

// UI update functions
const updateUI = () => {
  const loginCard = document.getElementById('loginCard');
  const appContent = document.getElementById('appContent');
  const userInfo = document.getElementById('userInfo');
  const logoutBtn = document.getElementById('logoutBtn');
  
  if (isAuthenticated()) {
    loginCard.style.display = 'none';
    appContent.style.display = 'block';
    userInfo.textContent = `Signed in as: ${state.userInfo?.username || 'User'}`;
    logoutBtn.style.display = 'inline-block';
  } else {
    loginCard.style.display = 'block';
    appContent.style.display = 'none';
    userInfo.textContent = '';
    logoutBtn.style.display = 'none';
  }
};

// Event handlers
const handleLogin = async (event) => {
  event.preventDefault();
  
  const form = event.target;
  const submitBtn = form.querySelector('button[type="submit"]');
  const username = form.username.value;
  const password = form.password.value;
  
  setLoading(submitBtn, true);
  
  try {
    await login(username, password);
    showToast('Success', 'Logged in successfully!', 'success');
    updateUI();
    form.reset();
  } catch (error) {
    showToast('Login Failed', error.message, 'error');
  } finally {
    setLoading(submitBtn, false);
  }
};

const handleTestHello = async () => {
  const btn = document.getElementById('testHelloBtn');
  const responseDiv = document.getElementById('helloResponse');
  
  setLoading(btn, true);
  responseDiv.style.display = 'none';
  
  try {
    const response = await apiCall('/api/hello', {
      method: 'GET'
    });
    
    responseDiv.className = 'alert alert-success';
    responseDiv.textContent = `✅ Success: ${response}`;
    responseDiv.style.display = 'block';
    showToast('API Test', 'Successfully called /api/hello', 'success');
  } catch (error) {
    responseDiv.className = 'alert alert-danger';
    responseDiv.textContent = `❌ Error: ${error.message}`;
    responseDiv.style.display = 'block';
    showToast('API Test Failed', error.message, 'error');
  } finally {
    setLoading(btn, false);
  }
};

// Initialize application
const init = () => {
  console.log('CLIMS Frontend initializing...');
  console.log('API Base URL:', config.apiBase);
  
  // Load saved tokens
  loadTokens();
  updateUI();
  
  // Setup event listeners
  const loginForm = document.getElementById('loginForm');
  if (loginForm) {
    loginForm.addEventListener('submit', handleLogin);
  }
  
  const testHelloBtn = document.getElementById('testHelloBtn');
  if (testHelloBtn) {
    testHelloBtn.addEventListener('click', handleTestHello);
  }
  
  const logoutBtn = document.getElementById('logoutBtn');
  if (logoutBtn) {
    logoutBtn.addEventListener('click', logout);
  }
  
  console.log('CLIMS Frontend initialized');
};

// Run initialization when DOM is ready
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', init);
} else {
  init();
}
