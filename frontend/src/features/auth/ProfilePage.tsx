import React, { useState } from 'react';
import { useAuth } from './AuthContext';
import Api, { createApi } from '../../api/client';
import { useNavigate } from 'react-router-dom';
import { useToast } from '../../components/ToastContext';

const ProfilePage: React.FC = () => {
  const { logout, currentUser, login } = useAuth();
  const nav = useNavigate();
  const toast = useToast();
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [strength, setStrength] = useState(0);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState<string | null>(null);

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setMessage(null);
    if (newPassword !== confirm) {
      setMessage('New password and confirmation do not match');
      return;
    }
    setLoading(true);
    try {
      // Send both currentPassword and newPassword to server for verification
      const res = await Api.auth.changePassword({ currentPassword, newPassword });
      // replace stored refresh token if server returned a rotated token
      if ((res as any).refreshToken) {
        localStorage.setItem('clims_refresh', (res as any).refreshToken);
      }
      // Attempt to refresh access token using stored refresh token so user stays logged in
      const refresh = localStorage.getItem('clims_refresh');
      if (refresh) {
        try {
          const res = await createApi(() => null).auth.refresh({ refreshToken: refresh });
          login(res.token, refresh);
          toast.push('Password changed', 'success');
          return;
        } catch (e) {
          // fallthrough to logout
        }
      }
      // Clear token and force re-login if refresh not available or failed
      logout();
      nav('/login');
    } catch (err: any) {
      setMessage(err?.body?.message ?? err?.message ?? 'Failed to change password');
    } finally {
      setLoading(false);
    }
  };

  const calcStrength = (pwd: string) => {
    let score = 0;
    if (pwd.length >= 8) score++;
    if (/[A-Z]/.test(pwd)) score++;
    if (/[a-z]/.test(pwd)) score++;
    if (/[0-9]/.test(pwd)) score++;
    if (/[^A-Za-z0-9]/.test(pwd)) score++;
    setStrength(score);
  };

  // update strength as user types
  React.useEffect(() => { calcStrength(newPassword); }, [newPassword]);

  return (
    <div className="max-w-md mx-auto">
      <h2 className="text-xl font-semibold mb-4">Profile</h2>
      <div className="mb-6">
        <div><strong>Username:</strong> {currentUser?.username}</div>
        <div><strong>Email:</strong> {currentUser?.email}</div>
      </div>

      <form onSubmit={onSubmit} className="space-y-3 bg-white p-4 rounded shadow">
        <h3 className="font-medium">Change Password</h3>
        <div>
          <label htmlFor="currentPassword" className="block text-sm">Current password (optional)</label>
          <input id="currentPassword" name="currentPassword" type="password" value={currentPassword} onChange={e => setCurrentPassword(e.target.value)} className="w-full border px-2 py-1 rounded" />
        </div>
        <div>
          <label htmlFor="newPassword" className="block text-sm">New password</label>
          <input id="newPassword" name="newPassword" type="password" value={newPassword} onChange={e => setNewPassword(e.target.value)} className="w-full border px-2 py-1 rounded" />
          <div className="mt-2 h-2 bg-gray-200 rounded overflow-hidden">
            <div style={{ width: `${(strength/5)*100}%` }} className={`h-2 ${strength < 3 ? 'bg-red-500' : strength < 4 ? 'bg-yellow-400' : 'bg-green-500'}`}></div>
          </div>
          <div className="text-xs text-gray-600 mt-1">Strength: {strength}/5</div>
        </div>
        <div>
          <label htmlFor="confirmNewPassword" className="block text-sm">Confirm new password</label>
          <input id="confirmNewPassword" name="confirmNewPassword" type="password" value={confirm} onChange={e => setConfirm(e.target.value)} className="w-full border px-2 py-1 rounded" />
        </div>
        {message && <div className="text-sm text-red-600">{message}</div>}
        <div className="flex items-center gap-2">
          <button type="submit" disabled={loading} className="px-3 py-1 bg-blue-600 text-white rounded">{loading ? 'Saving...' : 'Change Password'}</button>
        </div>
      </form>
    </div>
  );
};

export default ProfilePage;
