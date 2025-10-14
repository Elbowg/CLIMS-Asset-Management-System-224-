import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { useAuth } from '../auth/AuthContext';
import Api, { request } from '../../api/client';

type Asset = {
  id: number;
  name: string;
  serialNumber?: string;
  status?: string;
};

type Page<T> = {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  page: number;
};

export const AssetsPage: React.FC = () => {
  const { token } = useAuth();
  const [page, setPage] = React.useState(0);
  const [size, setSize] = React.useState(10);

  const { data, isLoading, isError, error, refetch } = useQuery<Page<Asset> | Asset[]>({
    queryKey: ['assets', token, page, size],
    queryFn: async () => request<Page<Asset> | Asset[]>(`/api/assets?page=${page}&size=${size}`, { method: 'GET' }, { token }),
    enabled: !!token
  });

  const isPage = (v: any): v is Page<Asset> => v && typeof v === 'object' && 'content' in v;

  return (
    <div className="p-6">
      <div className="flex items-center justify-between mb-4">
        <h1 className="text-2xl font-semibold">Assets</h1>
        <div className="flex items-center gap-2">
          <button onClick={() => refetch()} className="px-3 py-1 rounded bg-gray-200 hover:bg-gray-300">Refresh</button>
          <a href="/api/assets/export/csv" className="px-3 py-1 rounded bg-blue-600 text-white hover:bg-blue-700">Export CSV</a>
          <a href="/assets/new" className="px-3 py-1 rounded bg-green-600 text-white hover:bg-green-700">New Asset</a>
        </div>
      </div>
      {isLoading && <div>Loading...</div>}
      {isError && <div className="text-red-600 text-sm">{(error as Error).message}</div>}
      {data && (
        <div className="overflow-x-auto border rounded">
          <table className="min-w-full text-sm">
            <thead className="bg-gray-50">
              <tr>
                <th className="text-left px-3 py-2">ID</th>
                <th className="text-left px-3 py-2">Name</th>
                <th className="text-left px-3 py-2">Serial</th>
                <th className="text-left px-3 py-2">Status</th>
              </tr>
            </thead>
            <tbody>
              {(isPage(data) ? data.content ?? [] : data ?? []).map((a: Asset) => (
                <tr key={a.id} className="border-t">
                  <td className="px-3 py-2">{a.id}</td>
                  <td className="px-3 py-2">{a.name}</td>
                  <td className="px-3 py-2">{a.serialNumber ?? '-'}</td>
                  <td className="px-3 py-2">{a.status ?? '-'}</td>
                </tr>
              ))}
              {((isPage(data) ? (data.content?.length ?? 0) : (data as Asset[])?.length ?? 0) === 0) && (
                <tr>
                  <td className="px-3 py-4 text-center text-gray-500" colSpan={4}>No assets</td>
                </tr>
              )}
            </tbody>
          </table>
          <div className="flex items-center justify-between p-3">
            <div className="flex items-center gap-2 text-sm">
              <button onClick={() => setPage(p => Math.max(0, p - 1))} className="px-2 py-1 border rounded">Prev</button>
              <span>Page {(isPage(data) ? (data.page ?? page) : page) + 1} of {isPage(data) ? (data.totalPages ?? 1) : 1}</span>
              <button onClick={() => setPage(p => p + 1)} className="px-2 py-1 border rounded">Next</button>
            </div>
            <div className="flex items-center gap-2 text-sm">
              <label className="text-sm">Page size</label>
              <select value={size} onChange={e => { setSize(Number(e.target.value)); setPage(0); }} className="border px-2 py-1">
                <option value={5}>5</option>
                <option value={10}>10</option>
                <option value={20}>20</option>
              </select>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
