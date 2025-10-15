import React from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './features/auth/AuthContext';
import { ToastProvider } from './components/ToastContext';
import { LoginPage } from './features/auth/LoginPage';
import { DashboardPage } from './features/dashboard/DashboardPage';
import { AssetsPage } from './features/assets/AssetsPage';
import { UsersPage } from './features/users/UsersPage';
import { DepartmentsPage } from './features/departments/DepartmentsPage';
import { MaintenancePage } from './features/maintenance/MaintenancePage';
import { LocationsPage } from './features/locations/LocationsPage';
import { VendorsPage } from './features/vendors/VendorsPage';
import { AssetDetailPage } from './features/assets/AssetDetailPage';
import { AssetFormPage } from './features/assets/AssetFormPage';
import { NotFoundPage } from './features/misc/NotFoundPage';
import ProfilePage from './features/auth/ProfilePage';
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
      <ToastProvider>
        <AuthProvider>
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route path="/" element={<PrivateRoute><AppLayout><DashboardPage /></AppLayout></PrivateRoute>} />
            <Route path="/assets" element={<PrivateRoute><AppLayout><AssetsPage /></AppLayout></PrivateRoute>} />
            <Route path="/users" element={<PrivateRoute><AppLayout><UsersPage /></AppLayout></PrivateRoute>} />
            <Route path="/profile" element={<PrivateRoute><AppLayout><ProfilePage /></AppLayout></PrivateRoute>} />
            <Route path="/departments" element={<PrivateRoute><AppLayout><DepartmentsPage /></AppLayout></PrivateRoute>} />
            <Route path="/maintenance" element={<PrivateRoute><AppLayout><MaintenancePage /></AppLayout></PrivateRoute>} />
            <Route path="/locations" element={<PrivateRoute><AppLayout><LocationsPage /></AppLayout></PrivateRoute>} />
            <Route path="/vendors" element={<PrivateRoute><AppLayout><VendorsPage /></AppLayout></PrivateRoute>} />
            <Route path="/assets/:id" element={<PrivateRoute><AppLayout><AssetDetailPage /></AppLayout></PrivateRoute>} />
            <Route path="/assets/new" element={<PrivateRoute><AppLayout><AssetFormPage /></AppLayout></PrivateRoute>} />
            <Route path="/assets/:id/edit" element={<PrivateRoute><AppLayout><AssetFormPage /></AppLayout></PrivateRoute>} />
            <Route path="*" element={<NotFoundPage />} />
          </Routes>
          </AuthProvider>
      </ToastProvider>
    </BrowserRouter>
  </QueryClientProvider>
);

export default App;
