import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import ProfilePage from '../features/auth/ProfilePage';
import { AuthProvider } from '../features/auth/AuthContext';
import { ToastProvider } from '../components/ToastContext';
import { Api } from '../api/client';
import { BrowserRouter } from 'react-router-dom';
import { vi, describe, it, expect } from 'vitest';

describe('ProfilePage', () => {
  it('submits change password and logs out', async () => {
  const changePassword = vi.fn(() => Promise.resolve());
  const refresh = vi.fn(() => Promise.resolve({ token: 'newtoken' }));
  // override Api.auth with a typed mock for the functions we need
  Api.auth = {
    ...Api.auth,
    changePassword: changePassword as unknown as typeof Api.auth.changePassword,
    refresh: refresh as unknown as typeof Api.auth.refresh
  } as typeof Api.auth;

    render(
      <BrowserRouter>
        <ToastProvider>
          <AuthProvider>
            <ProfilePage />
          </AuthProvider>
        </ToastProvider>
      </BrowserRouter>
    );

  const newPass = screen.getByLabelText('New password');
  const confirm = screen.getByLabelText('Confirm new password');
    const submit = screen.getByRole('button', { name: /Change Password/i });

  fireEvent.change(newPass, { target: { value: 'Abc12345!' } });
  fireEvent.change(confirm, { target: { value: 'Abc12345!' } });
  const current = screen.getByLabelText('Current password (optional)');
  fireEvent.change(current, { target: { value: 'OldPass1!' } });
    fireEvent.click(submit);

  await waitFor(() => expect(changePassword).toHaveBeenCalledWith({ currentPassword: 'OldPass1!', newPassword: 'Abc12345!' }));
  });
});
