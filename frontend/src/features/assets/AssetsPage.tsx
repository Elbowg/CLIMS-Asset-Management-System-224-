import React from 'react';
import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { useAuth } from '../auth/AuthContext';
import Api, { request } from '../../api/client';

type Asset = {
  id: number;
  assetId?: string; // e.g. LAP-001
  assetTag?: string;
  serialNumber?: string;
  make?: string;
  type?: string;
  brand?: string;
  model?: string;
  department?: string;
  assignedTo?: string;
  status?: string;
  purchaseDate?: string; // ISO date
  location?: string;
  vendor?: string;
};

type Page<T> = {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  page: number;
};

const statusColor = (s?: string) => {
  switch ((s || '').toLowerCase()) {
    case 'available': return 'bg-green-100 text-green-800';
    case 'assigned': return 'bg-sky-100 text-sky-800';
    case 'under_repair': return 'bg-amber-100 text-amber-800';
    case 'retired': return 'bg-red-100 text-red-800';
    default: return 'bg-gray-100 text-gray-800';
  }
};

export const AssetsPage: React.FC = () => {
  const { token, currentUser } = useAuth();
  const [page, setPage] = React.useState(0);
  const [size, setSize] = React.useState(10);
  const [query, setQuery] = React.useState('');
  const [statusFilter, setStatusFilter] = React.useState('');
  const [departmentFilter, setDepartmentFilter] = React.useState('');

  const { data, isLoading, isError, error, refetch } = useQuery<Page<Asset> | Asset[]>({
    queryKey: ['assets', token, page, size, query, statusFilter, departmentFilter],
    queryFn: async () => {
      const params = new URLSearchParams();
      params.set('page', String(page));
      params.set('size', String(size));
      if (query) params.set('q', query);
      if (statusFilter) params.set('status', statusFilter);
      if (departmentFilter) params.set('department', departmentFilter);
      return request<Page<Asset> | Asset[]>(`/api/assets?${params.toString()}`, { method: 'GET' }, { token });
    },
    enabled: !!token
  });

  const isPage = (v: any): v is Page<Asset> => v && typeof v === 'object' && 'content' in v;

  const assets = isPage(data) ? data.content ?? [] : (data ?? []) as Asset[];
  const [deletingId, setDeletingId] = React.useState<number | null>(null);

  const canDeleteAsset = (a: Asset) => {
    const role = currentUser?.role ?? '';
    if (role === 'ADMIN' || role === 'IT_STAFF') return true;
    if (role === 'MANAGER') return !!(currentUser?.department && a.department && currentUser.department === a.department);
    return false;
  };

  const handleDelete = async (id: number) => {
    if (!token) return;
    const ok = window.confirm('Delete this asset? This action cannot be undone.');
    if (!ok) return;
    try {
      setDeletingId(id);
      await request(`/api/assets/${id}`, { method: 'DELETE' }, { token });
      // refresh list after delete
      refetch();
    } catch (e) {
      // minimal error feedback
      console.error('Delete failed', e);
      alert('Failed to delete asset');
    } finally {
      setDeletingId(null);
    }
  };

  return (
    <div className="p-6">
      <div className="mb-4">
        <h1 className="text-2xl font-semibold mb-3">Assets</h1>

        <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-3">
          <div className="flex items-center gap-2 w-full md:w-2/3">
            <div className="relative w-full">
              <input
                aria-label="Search assets"
                type="text"
                placeholder="Search by Asset ID, User, Type, or Brand..."
                value={query}
                onChange={e => setQuery(e.target.value)}
                className="w-full pl-10 pr-4 py-3 border rounded-lg shadow-sm focus:outline-none focus:ring-2 focus:ring-sky-200"
              />
              <div className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400">
                <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-4.35-4.35m0 0A7.5 7.5 0 103.5 3.5a7.5 7.5 0 0013.15 13.15z" />
                </svg>
              </div>
            </div>

            <div className="flex items-center gap-2">
              <button onClick={() => { setQuery(''); setStatusFilter(''); setDepartmentFilter(''); refetch(); }} className="px-3 py-2 border rounded bg-white hover:bg-gray-50">Clear</button>
              <button aria-label="Run search" onClick={() => refetch()} className="px-3 py-2 rounded bg-gray-800 text-white hover:bg-gray-700">Search</button>
            </div>
          </div>

          <div className="flex items-center gap-2 w-full md:w-auto">
            <select aria-label="Filter by status" value={statusFilter} onChange={e => setStatusFilter(e.target.value)} className="border rounded-lg px-3 py-2 bg-white">
              <option value="">All Statuses</option>
              <option value="AVAILABLE">Available</option>
              <option value="ASSIGNED">Assigned</option>
              <option value="UNDER_REPAIR">Under Repair</option>
              <option value="RETIRED">Retired</option>
            </select>
            <select aria-label="Filter by department" value={departmentFilter} onChange={e => setDepartmentFilter(e.target.value)} className="border rounded-lg px-3 py-2 bg-white">
              <option value="">All Departments</option>
              <option value="IT">IT</option>
              <option value="Finance">Finance</option>
              <option value="HR">HR</option>
              <option value="Marketing">Marketing</option>
            </select>
            {((currentUser?.role === 'ADMIN') || (currentUser?.role === 'IT_STAFF') || (currentUser?.role === 'MANAGER')) ? (
              <Link to="/assets/new" className="ml-3 inline-flex items-center gap-2 px-3 py-2 bg-sky-600 text-white rounded hover:bg-sky-700">
              <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
              </svg>
              Add asset
              </Link>
            ) : (
              <span className="ml-3 inline-flex items-center gap-2 px-3 py-2 bg-gray-200 text-gray-500 rounded" title="You don't have permission to create assets">Add asset</span>
            )}
          </div>
        </div>
      </div>

      {isLoading && <div>Loading...</div>}
      {isError && <div className="text-red-600 text-sm">{(error as Error).message}</div>}

      <div className="overflow-x-auto border rounded-lg bg-white shadow-sm">
        <table className="min-w-full text-sm">
          <thead className="bg-gray-50">
            <tr>
              <th className="text-left px-6 py-4">Asset ID</th>
              <th className="text-left px-6 py-4">Type</th>
              <th className="text-left px-6 py-4">Brand</th>
              <th className="text-left px-6 py-4">Model</th>
              <th className="text-left px-6 py-4">Department</th>
              <th className="text-left px-6 py-4">Assigned To</th>
              <th className="text-left px-6 py-4">Status</th>
              <th className="text-left px-6 py-4">Purchase Date</th>
              <th className="text-right px-6 py-4">Actions</th>
            </tr>
          </thead>
          <tbody>
            {assets.map((a) => (
              <tr key={a.id} className="border-t hover:bg-gray-50">
                <td className="px-6 py-4 font-medium">{a.assetId ?? `#${a.id}`}</td>
                <td className="px-6 py-4">{a.type ?? '-'}</td>
                <td className="px-6 py-4">{a.make ?? a.brand ?? '-'}</td>
                <td className="px-6 py-4">{a.model ?? '-'}</td>
                <td className="px-6 py-4">{a.department ?? '-'}</td>
                <td className="px-6 py-4">{a.assignedTo ?? '-'}</td>
                <td className="px-6 py-4">
                  <span className={`inline-flex items-center px-3 py-1 rounded-full text-xs font-semibold ${statusColor(a.status)}`} aria-label={`Status ${a.status}`}>
                    {a.status ?? 'Unknown'}
                  </span>
                </td>
                <td className="px-6 py-4">{a.purchaseDate ? new Date(a.purchaseDate).toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' }) : '-'}</td>
                <td className="px-6 py-4 text-right">
                  {((currentUser?.role === 'ADMIN' || currentUser?.role === 'IT_STAFF') || (currentUser?.role === 'MANAGER' && currentUser.department && a.department && currentUser.department === a.department)) ? (
                    <a href={`/assets/${a.id}/edit`} className="inline-block mr-3 text-gray-600 hover:text-gray-900" title="Edit">
                      <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
                        <path d="M17.414 2.586a2 2 0 00-2.828 0L7 10.172V13h2.828l7.586-7.586a2 2 0 000-2.828z" />
                        <path d="M3 17a1 1 0 011-1h12v2H4a1 1 0 01-1-1z" />
                      </svg>
                    </a>
                  ) : (
                    <span className="inline-block mr-3 text-gray-300" title="You don't have permission to edit this asset">
                      <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
                        <path d="M17.414 2.586a2 2 0 00-2.828 0L7 10.172V13h2.828l7.586-7.586a2 2 0 000-2.828z" />
                        <path d="M3 17a1 1 0 011-1h12v2H4a1 1 0 01-1-1z" />
                      </svg>
                    </span>
                  )}
                  <button
                    className={`inline-block ${canDeleteAsset(a) ? 'text-red-600 hover:text-red-800' : 'text-gray-400'} disabled:opacity-50`}
                    title={canDeleteAsset(a) ? 'Delete' : "You don't have permission to delete this asset"}
                    onClick={() => handleDelete(a.id)}
                    disabled={deletingId === a.id || !canDeleteAsset(a)}
                  >
                    <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
                      <path fillRule="evenodd" d="M6 2a1 1 0 00-1 1v1H3.5A1.5 1.5 0 002 5.5V6h16v-.5A1.5 1.5 0 0016.5 4H15V3a1 1 0 00-1-1H6zm2 6a1 1 0 012 0v6a1 1 0 11-2 0V8zm4 0a1 1 0 10-2 0v6a1 1 0 102 0V8z" clipRule="evenodd" />
                    </svg>
                  </button>
                </td>
              </tr>
            ))}

            {assets.length === 0 && (
              <tr>
                <td className="px-6 py-8 text-center text-gray-500" colSpan={9}>No assets</td>
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
            <select aria-label="Page size" value={size} onChange={e => { setSize(Number(e.target.value)); setPage(0); }} className="border px-2 py-1">
              <option value={5}>5</option>
              <option value={10}>10</option>
              <option value={20}>20</option>
            </select>
          </div>
        </div>
      </div>
    </div>
  );
};
