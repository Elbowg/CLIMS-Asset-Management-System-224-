import React, { createContext, useContext, useState, useCallback, useEffect } from 'react';
import { createApi, Api as DefaultApi } from '../../api/client';
import { useNavigate } from 'react-router-dom';

interface AuthState {
  token: string | null;
  currentUser: User | null;
  login: (token: string) => void;
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
  const [currentUser, setCurrentUser] = useState<User | null>(null);

  const nav = useNavigate();

  // Create Api instance that will call logout on 401
  const api = createApi(() => token, { onUnauthorized: () => {
    setToken(null); localStorage.removeItem('clims_token'); setCurrentUser(null); nav('/login');
  } });

  // Fetch user info after login or when token changes
  useEffect(() => {
    if (token) {
      api.auth.me().then(setCurrentUser).catch(() => setCurrentUser(null));
    } else {
      setCurrentUser(null);
    }
  }, [token]);

  const login = useCallback((t: string) => {
    setToken(t);
    localStorage.setItem('clims_token', t);
  }, []);

  const logout = useCallback(() => {
    setToken(null);
    localStorage.removeItem('clims_token');
    setCurrentUser(null);
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
