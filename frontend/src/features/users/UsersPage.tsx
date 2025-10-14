import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { useAuth } from '../auth/AuthContext';
import { createApi } from '../../api/client';

type UserRow = {
  id?: number;
  username?: string;
  email?: string;
  role?: string;
  department?: string;
};

export const UsersPage: React.FC = () => {
  const { token } = useAuth();
  const api = createApi(() => token);
  const { data, isLoading, isError, error, refetch } = useQuery({
    queryKey: ['users', token],
    queryFn: async () => api.users.list(0, 50),
    enabled: !!token
  });

  return (
    <div className="p-6">
      <div className="flex items-center justify-between mb-4">
        <h1 className="text-2xl font-semibold">Users</h1>
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
                <th className="text-left px-3 py-2">Username</th>
                <th className="text-left px-3 py-2">Email</th>
                <th className="text-left px-3 py-2">Role</th>
                <th className="text-left px-3 py-2">Department</th>
              </tr>
            </thead>
            <tbody>
              {(data.content ?? []).map((u: UserRow) => (
                <tr key={u.id} className="border-t">
                  <td className="px-3 py-2">{u.id}</td>
                  <td className="px-3 py-2">{u.username}</td>
                  <td className="px-3 py-2">{u.email}</td>
                  <td className="px-3 py-2">{u.role}</td>
                  <td className="px-3 py-2">{u.department ?? '-'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
};

export default UsersPage;
