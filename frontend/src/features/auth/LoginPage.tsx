import React, { useState } from 'react';
import { useAuth } from './AuthContext';
import { createApi } from '../../api/client';
import { useNavigate } from 'react-router-dom';

export const LoginPage: React.FC = () => {
  const { login } = useAuth();
  const api = createApi(() => null);
  const nav = useNavigate();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    try {
      const res = await api.auth.login({ username, password });
      login(res.token);
      nav('/');
    } catch (err: any) {
      // ApiError includes body which may have { error: '...' }
      const body = err?.body;
      if (body && typeof body === 'object' && 'error' in body) setError((body as any).error as string);
      else setError(err.message || 'Login failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center p-4">
      <form onSubmit={submit} className="w-full max-w-sm space-y-4 bg-white p-6 rounded shadow">
        <h1 className="text-xl font-semibold">Sign in</h1>
        {error && <div className="text-sm text-red-600">{error}</div>}
        <div>
          <label className="block text-sm font-medium mb-1">Username</label>
          <input value={username} onChange={e => setUsername(e.target.value)} className="w-full rounded border px-2 py-1" />
        </div>
        <div>
          <label className="block text-sm font-medium mb-1">Password</label>
          <input type="password" value={password} onChange={e => setPassword(e.target.value)} className="w-full rounded border px-2 py-1" />
        </div>
        <button disabled={loading} className="w-full bg-brand text-white py-2 rounded hover:bg-brand-dark disabled:opacity-50">
          {loading ? 'Signing in...' : 'Sign In'}
        </button>
      </form>
    </div>
  );
};
