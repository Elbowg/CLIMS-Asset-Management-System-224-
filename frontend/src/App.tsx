import React from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './features/auth/AuthContext';
import { LoginPage } from './features/auth/LoginPage';
import { DashboardPage } from './features/dashboard/DashboardPage';
import { AssetsPage } from './features/assets/AssetsPage';
import { NotFoundPage } from './features/misc/NotFoundPage';
import AppLayout from './layouts/AppLayout';

const PrivateRoute: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { token } = useAuth();
  if (!token) {
    return <Navigate to="/login" replace />;
  }
  return <>{children}</>;
};

const queryClient = new QueryClient();

export const App: React.FC = () => (
  // QueryClientProvider can be outermost. BrowserRouter must wrap AuthProvider so
  // AuthProvider can safely use navigation hooks (useNavigate).
  <QueryClientProvider client={queryClient}>
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/" element={<PrivateRoute><AppLayout><DashboardPage /></AppLayout></PrivateRoute>} />
          <Route path="/assets" element={<PrivateRoute><AppLayout><AssetsPage /></AppLayout></PrivateRoute>} />
          <Route path="*" element={<NotFoundPage />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  </QueryClientProvider>
);

export default App;
