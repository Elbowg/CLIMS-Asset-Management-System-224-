import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { useAuth } from '../auth/AuthContext';
import { createApi } from '../../api/client';

export const VendorsPage: React.FC = () => {
  const { token } = useAuth();
  const api = createApi(() => token);
  const { data, isLoading, isError, error, refetch } = useQuery({
    queryKey: ['vendors', token],
    queryFn: async () => api.lookups.vendors(),
    enabled: !!token
  });

  return (
    <div className="p-6">
      <div className="flex items-center justify-between mb-4">
        <h1 className="text-2xl font-semibold">Vendors</h1>
        <div>
          <button onClick={() => refetch()} className="px-3 py-1 rounded bg-gray-200">Refresh</button>
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
                <th className="text-left px-3 py-2">Name</th>
                <th className="text-left px-3 py-2">Email</th>
                <th className="text-left px-3 py-2">Contact</th>
              </tr>
            </thead>
            <tbody>
              {(data ?? []).map((v: any) => (
                <tr key={v.id} className="border-t">
                  <td className="px-3 py-2">{v.id}</td>
                  <td className="px-3 py-2">{v.name}</td>
                  <td className="px-3 py-2">{v.email ?? '-'}</td>
                  <td className="px-3 py-2">{v.contactNumber ?? '-'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
};

export default VendorsPage;
