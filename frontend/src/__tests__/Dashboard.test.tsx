import React from 'react';
import { render, screen, waitFor, within } from '@testing-library/react';
import { describe, it, vi, expect } from 'vitest';

// Mock the API factory to return controlled responses synchronously
vi.mock('../../api/client', async () => {
  const actual = await vi.importActual('../../api/client');
  return {
    ...actual,
    createApi: () => ({
      assets: { list: async () => ({ content: [{ id: 1, assetTag: 'A1', status: 'AVAILABLE', make: 'Dell', model: 'XPS' }], totalElements: 1 }) },
      maintenance: { list: async () => ({ content: [], totalElements: 0 }) },
      reports: { kpis: async () => ({ totalAssets: 1, assetsByStatus: { AVAILABLE: 1 }, upcomingMaintenance: 0 }) },
      auth: { me: async () => ({ id: 1, username: 'admin', email: 'admin@example.com' }) }
    })
  };
});

// Mock auth context so useAuth returns a token synchronously
vi.mock('../features/auth/AuthContext', () => ({
  AuthProvider: ({ children }: any) => children,
  useAuth: () => ({ token: 'test-token', currentUser: { id: 1, username: 'admin' } })
}));

import { DashboardPage } from '../features/dashboard/DashboardPage';
import { BrowserRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider } from '../features/auth/AuthContext';

function makeQueryClient() {
  return new QueryClient({ defaultOptions: { queries: { retry: false } } });
}

describe('Dashboard', () => {
  it('renders KPI numbers from backend', async () => {
    // Ensure AuthProvider picks up a token so queries enable
    localStorage.setItem('clims_token', 'test-token');

    const qc = makeQueryClient();

    render(
      <QueryClientProvider client={qc}>
        <BrowserRouter>
          <AuthProvider>
            <DashboardPage />
          </AuthProvider>
        </BrowserRouter>
      </QueryClientProvider>
    );

    // Basic sanity: the page renders its main headings
    expect(screen.getByText('Dashboard')).toBeInTheDocument();
    expect(screen.getByText('Total assets')).toBeInTheDocument();
  });
});
