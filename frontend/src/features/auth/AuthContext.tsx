import React, { createContext, useContext, useState, useCallback, useEffect } from 'react';
import { createApi, Api as DefaultApi } from '../../api/client';
import { useNavigate } from 'react-router-dom';

interface AuthState {
  token: string | null;
  currentUser: User | null;
  login: (token: string, refresh?: string | null) => void;
  logout: () => void;
}

export type User = {
  id?: number;
  username?: string;
  email?: string;
  role?: string;
  department?: string;
};

const AuthContext = createContext<AuthState | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [token, setToken] = useState<string | null>(() => localStorage.getItem('clims_token'));
  const [refreshToken, setRefreshToken] = useState<string | null>(() => localStorage.getItem('clims_refresh'));
  const [currentUser, setCurrentUser] = useState<User | null>(null);

  const nav = useNavigate();

  // Create Api instance that will call logout on 401
  const api = createApi(() => token, { onUnauthorized: async () => {
    // attempt a refresh using stored refresh token
    try {
      if (refreshToken) {
        const res = await fetch('/api/auth/refresh', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ refreshToken }) });
        if (res.ok) {
          const body = await res.json();
          const newToken = body.token;
          setToken(newToken);
          localStorage.setItem('clims_token', newToken);
          return;
        }
      }
    } catch (e) {
      // ignore
    }
    setToken(null); localStorage.removeItem('clims_token'); setCurrentUser(null); localStorage.removeItem('clims_refresh'); setRefreshToken(null); nav('/login');
  } });

  // Fetch user info after login or when token changes
  useEffect(() => {
    if (token) {
      api.auth.me().then(setCurrentUser).catch(() => setCurrentUser(null));
    } else {
      setCurrentUser(null);
    }
  }, [token]);

  const login = useCallback((t: string, r: string | null = null) => {
    setToken(t);
    localStorage.setItem('clims_token', t);
    if (r) { setRefreshToken(r); localStorage.setItem('clims_refresh', r); }
  }, []);

  const logout = useCallback(() => {
    setToken(null);
    localStorage.removeItem('clims_token');
    setCurrentUser(null);
    localStorage.removeItem('clims_refresh');
    setRefreshToken(null);
    nav('/login');
  }, [nav]);

  return (
    <AuthContext.Provider value={{ token, currentUser, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
};
