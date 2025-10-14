import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { useAuth } from '../auth/AuthContext';
import { createApi } from '../../api/client';
import { Link } from 'react-router-dom';

export const DashboardPage: React.FC = () => {
  const { token } = useAuth();
  const api = createApi(() => token);

  const assetsQ = useQuery({
    queryKey: ['dashboard', 'assets', token],
    queryFn: async () => api.assets.list(0, 100),
    enabled: !!token
  });

  const maintenanceQ = useQuery({
    queryKey: ['dashboard', 'maintenance', token],
    queryFn: async () => api.maintenance.list(0, 5),
    enabled: !!token
  });

  const assets = assetsQ.data?.content ?? [];
  const totalAssets = assetsQ.data?.totalElements ?? (Array.isArray(assets) ? assets.length : 0);
  const byStatus = (assets || []).reduce((acc: Record<string, number>, a: any) => { acc[a.status ?? 'UNKNOWN'] = (acc[a.status ?? 'UNKNOWN'] || 0) + 1; return acc; }, {});

  return (
    <div className="p-6">
      <h1 className="text-2xl font-semibold mb-4">Dashboard</h1>
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
        <div className="p-4 rounded-lg shadow-sm bg-white flex items-center gap-4">
          <div className="p-3 rounded-full bg-blue-50">
            <svg className="w-6 h-6 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 7h18M3 12h18M3 17h18" /></svg>
          </div>
          <div>
            <div className="text-sm text-gray-500">Total assets</div>
            <div className="text-2xl font-bold">{assetsQ.isLoading ? <span className="inline-block w-20 h-6 bg-gray-200 rounded animate-pulse" /> : totalAssets}</div>
            <Link to="/assets" className="text-sm text-blue-600 mt-2 inline-block">View assets</Link>
          </div>
        </div>

        <div className="p-4 rounded-lg shadow-sm bg-white flex items-center gap-4">
          <div className="p-3 rounded-full bg-yellow-50">
            <svg className="w-6 h-6 text-yellow-600" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M12 20c4.418 0 8-3.582 8-8s-3.582-8-8-8-8 3.582-8 8 3.582 8 8 8z" /></svg>
          </div>
          <div>
            <div className="text-sm text-gray-500">Upcoming maintenance</div>
            <div className="text-2xl font-bold">{maintenanceQ.isLoading ? <span className="inline-block w-10 h-6 bg-gray-200 rounded animate-pulse" /> : (maintenanceQ.data?.totalElements ?? (maintenanceQ.data?.content?.length ?? 0))}</div>
            <Link to="/maintenance" className="text-sm text-blue-600 mt-2 inline-block">View maintenance</Link>
          </div>
        </div>

        <div className="p-4 rounded-lg shadow-sm bg-white">
          <div className="text-sm text-gray-500">Assets by status</div>
          <div className="mt-2 grid grid-cols-2 gap-2 text-sm">
            {assetsQ.isLoading ? (
              Array.from({ length: 3 }).map((_, i) => <div key={i} className="h-5 bg-gray-200 rounded animate-pulse" />)
            ) : (
              Object.entries(byStatus).map(([k,v]) => (
                <div key={k} className="flex items-center justify-between px-2 py-1 bg-gray-50 rounded">
                  <div className="text-xs text-gray-700">{k}</div>
                  <div className="font-medium">{v}</div>
                </div>
              ))
            )}
            {!assetsQ.isLoading && Object.keys(byStatus).length === 0 && <div className="text-gray-500">No data</div>}
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div className="border rounded-lg p-4 bg-white">
          <h2 className="font-semibold mb-3">Recent assets</h2>
          <div className="space-y-3">
            {assetsQ.isLoading ? (
              Array.from({ length: 5 }).map((_, idx) => <div key={idx} className="h-12 bg-gray-100 rounded animate-pulse" />)
            ) : (
              (assets || []).slice(0,5).map((a: any) => (
                <div key={a.id} className="flex items-center justify-between">
                  <div>
                    <div className="font-medium">{a.assetTag ?? `Asset ${a.id}`}</div>
                    <div className="text-gray-500 text-xs">{a.make ?? '-'} {a.model ?? ''}</div>
                  </div>
                  <Link to={`/assets/${a.id}`} className="text-blue-600 text-sm">Open</Link>
                </div>
              ))
            )}
            {!assetsQ.isLoading && (assets || []).length === 0 && <div className="text-gray-500">No assets</div>}
          </div>
        </div>

        <div className="border rounded-lg p-4 bg-white">
          <h2 className="font-semibold mb-3">Upcoming maintenance</h2>
          <div className="space-y-3">
            {maintenanceQ.isLoading ? (
              Array.from({ length: 5 }).map((_, idx) => <div key={idx} className="h-12 bg-gray-100 rounded animate-pulse" />)
            ) : (
              (maintenanceQ.data?.content ?? []).map((m: any) => (
                <div key={m.id} className="flex items-center justify-between">
                  <div>
                    <div className="font-medium">{m.assetTag ?? `Asset ${m.assetId}`}</div>
                    <div className="text-gray-500 text-xs">{m.description ?? '-'}</div>
                  </div>
                  <Link to={`/assets/${m.assetId}`} className="text-blue-600 text-sm">Open</Link>
                </div>
              ))
            )}
            {!maintenanceQ.isLoading && (maintenanceQ.data?.content ?? []).length === 0 && <div className="text-gray-500">No upcoming maintenance</div>}
          </div>
        </div>
      </div>
    </div>
  );
};
