import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { useAuth } from '../auth/AuthContext';
import { useToast } from '../../components/ToastContext';
import { createApi } from '../../api/client';

export const MaintenancePage: React.FC = () => {
  const { token } = useAuth();
  const api = createApi(() => token);
  const toast = useToast();
  const { data, isLoading, isError, error, refetch } = useQuery({
    queryKey: ['maintenance', token],
    queryFn: async () => api.maintenance.list(0, 50),
    enabled: !!token
  });

  const downloadCSV = async () => {
    try {
      const blob = await api.reports.maintenanceCsv({});
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = 'maintenance.csv';
      document.body.appendChild(a);
      a.click();
      a.remove();
      URL.revokeObjectURL(url);
    } catch (e: any) {
      console.error(e);
      toast.push(e?.message ?? 'Failed to download CSV', 'error');
    }
  };

  return (
    <div className="p-6">
      <div className="flex items-center justify-between mb-4">
        <h1 className="text-2xl font-semibold">Maintenance</h1>
        <div className="flex gap-2">
          <button onClick={() => refetch()} className="px-3 py-1 rounded bg-gray-200">Refresh</button>
          <button onClick={downloadCSV} className="px-3 py-1 rounded bg-blue-600 text-white">Download CSV</button>
        </div>
      </div>
      {isLoading && <div>Loading...</div>}
      {isError && <div className="text-red-600">{(error as Error).message}</div>}
      {data && (
        <div className="overflow-x-auto border rounded">
          <table className="min-w-full text-sm">
            <thead className="bg-gray-50">
              <tr>
                <th className="text-left px-3 py-2">ID</th>
                <th className="text-left px-3 py-2">Asset</th>
                <th className="text-left px-3 py-2">Description</th>
                <th className="text-left px-3 py-2">Status</th>
                <th className="text-left px-3 py-2">Scheduled</th>
              </tr>
            </thead>
            <tbody>
              {(data.content ?? []).map((m: any) => (
                <tr key={m.id} className="border-t">
                  <td className="px-3 py-2">{m.id}</td>
                  <td className="px-3 py-2">{m.assetTag ?? m.assetId}</td>
                  <td className="px-3 py-2">{m.description}</td>
                  <td className="px-3 py-2">{m.status}</td>
                  <td className="px-3 py-2">{m.scheduledDate ?? '-'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
};

export default MaintenancePage;
