import React from 'react';

export const DashboardPage: React.FC = () => {
  return (
    <div className="p-6">
      <h1 className="text-2xl font-semibold mb-4">Dashboard</h1>
      <p className="text-gray-600">Welcome to CLIMS. This will show KPIs (assets by status, upcoming maintenance, etc.).</p>
    </div>
  );
};
