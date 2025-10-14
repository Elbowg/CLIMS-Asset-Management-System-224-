import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../features/auth/AuthContext';

export const AppLayout: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { logout, currentUser } = useAuth();
  const nav = useNavigate();
  const [menuOpen, setMenuOpen] = useState(false);

  const onLogout = () => {
    logout();
    nav('/login');
  };

  return (
    <div className="min-h-screen flex flex-col">
      <header className="bg-gray-800 text-white">
        <div className="mx-auto max-w-6xl px-4 py-3 flex items-center justify-between">
          <div className="flex items-center gap-6">
            <Link to="/" className="font-semibold">CLIMS</Link>
            <nav className="flex items-center gap-4 text-sm">
              <Link className="hover:underline" to="/">Dashboard</Link>
              <Link className="hover:underline" to="/assets">Assets</Link>
            </nav>
          </div>
          <div className="relative">
            <button
              className="flex items-center gap-2 px-3 py-1 rounded bg-gray-700 hover:bg-gray-600 text-sm"
              onClick={() => setMenuOpen(v => !v)}
            >
              <span className="font-semibold">{currentUser?.username ?? 'User'}</span>
              {currentUser?.email && <span className="text-xs text-gray-300">({currentUser.email})</span>}
              <svg className="w-4 h-4 ml-1" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" /></svg>
            </button>
            {menuOpen && (
              <div className="absolute right-0 mt-2 w-40 bg-white text-gray-800 rounded shadow z-10">
                <div className="px-4 py-2 border-b text-xs text-gray-500">{currentUser?.role ?? ''}</div>
                <button
                  onClick={onLogout}
                  className="w-full text-left px-4 py-2 hover:bg-gray-100 text-sm"
                >Logout</button>
              </div>
            )}
          </div>
        </div>
      </header>
      <main className="flex-1 mx-auto max-w-6xl w-full px-4 py-6">
        {children}
      </main>
    </div>
  );
};

export default AppLayout;
