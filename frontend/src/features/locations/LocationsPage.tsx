import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { useAuth } from '../auth/AuthContext';
import { createApi } from '../../api/client';

export const LocationsPage: React.FC = () => {
  const { token } = useAuth();
  const api = createApi(() => token);
  const { data, isLoading, isError, error, refetch } = useQuery({
    queryKey: ['locations', token],
    queryFn: async () => api.lookups.locations(),
    enabled: !!token
  });

  return (
    <div className="p-6">
      <div className="flex items-center justify-between mb-4">
        <h1 className="text-2xl font-semibold">Locations</h1>
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
                <th className="text-left px-3 py-2">Building</th>
                <th className="text-left px-3 py-2">Room</th>
              </tr>
            </thead>
            <tbody>
              {(data ?? []).map((l: any) => (
                <tr key={l.id} className="border-t">
                  <td className="px-3 py-2">{l.id}</td>
                  <td className="px-3 py-2">{l.name}</td>
                  <td className="px-3 py-2">{l.building ?? '-'}</td>
                  <td className="px-3 py-2">{l.room ?? '-'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
};

export default LocationsPage;
