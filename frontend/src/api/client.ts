import type { paths, components } from './schema';

const API_BASE = import.meta.env.VITE_API_BASE || '';

export type FetcherOptions = {
  token?: string | null;
  signal?: AbortSignal;
};

export type ApiClientOptions = {
  onUnauthorized?: () => void;
};

export class ApiError extends Error {
  status: number;
  body?: unknown;
  constructor(message: string, status: number, body?: unknown) {
    super(message);
    this.status = status;
    this.body = body;
  }
}

async function parseJsonSafe(res: Response) {
  try {
    return await res.json();
  } catch {
    return undefined;
  }
}

export async function request<T>(path: string, init: RequestInit = {}, opts: FetcherOptions = {}, clientOpts: ApiClientOptions = {}): Promise<T> {
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(init.headers as Record<string, string> || {})
  };
  if (opts.token) headers['Authorization'] = `Bearer ${opts.token}`;
  const res = await fetch(`${API_BASE}${path}`, { ...init, headers, signal: opts.signal });
  if (!res.ok) {
    if (res.status === 401 && clientOpts.onUnauthorized) {
      try { clientOpts.onUnauthorized(); } catch { /* ignore */ }
    }
    const body = await parseJsonSafe(res) ?? (await res.text().catch(() => undefined));
    const message = `HTTP ${res.status} ${res.statusText}`;
    throw new ApiError(message, res.status, body);
  }
  if (res.status === 204) return undefined as unknown as T;
  return parseJsonSafe(res) as Promise<T>;
}

// Helper to fetch binary responses (csv/pdf) and return a Blob
export async function fetchBlob(path: string, init: RequestInit = {}, opts: FetcherOptions = {}, clientOpts: ApiClientOptions = {}): Promise<Blob> {
  const headers: Record<string, string> = {
    ...(init.headers as Record<string, string> || {})
  };
  if (opts.token) headers['Authorization'] = `Bearer ${opts.token}`;
  const res = await fetch(`${API_BASE}${path}`, { ...init, headers, signal: opts.signal });
  if (!res.ok) {
    if (res.status === 401 && clientOpts.onUnauthorized) {
      try { clientOpts.onUnauthorized(); } catch { /* ignore */ }
    }
    const text = await res.text().catch(() => undefined);
    const message = `HTTP ${res.status} ${res.statusText}`;
    throw new ApiError(message, res.status, text);
  }
  return res.blob();
}

// Factory allowing injection (e.g., for tests or future auth refresh logic)
export function createApi(getToken: () => string | null, opts: ApiClientOptions = {}) {
  return {
    auth: {
      login: (body: { username: string; password: string }) =>
        request<{ token: string }>('/api/auth/login', { method: 'POST', body: JSON.stringify(body) }, {}, opts),
      register: (body: { username: string; password: string; email?: string }) =>
        request('/api/auth/register', { method: 'POST', body: JSON.stringify(body) }, {}, opts),
      me: () => request<paths['/api/auth/me']['get']['responses']['200']['content']['*/*']>('/api/auth/me', { method: 'GET' }, { token: getToken() }, opts),
      changePassword: (body: components['schemas']['ChangePasswordRequest']) => request('/api/auth/change-password', { method: 'POST', body: JSON.stringify(body) }, { token: getToken() }, opts),
      refresh: (body: { refreshToken: string }) => request<{ token: string }>('/api/auth/refresh', { method: 'POST', body: JSON.stringify(body) }, {}, opts),
      logout: (body: { refreshToken: string }) => request('/api/auth/logout', { method: 'POST', body: JSON.stringify(body) }, {}, opts)
    },
    users: {
      list: (page = 0, size = 20) => request<components['schemas']['PageResponseUserResponse']>(`/api/users?page=${page}&size=${size}`, { method: 'GET' }, { token: getToken() }, opts),
      getById: (id: number) => request<components['schemas']['UserResponse']>(`/api/users/${id}`, { method: 'GET' }, { token: getToken() }, opts)
      , resetPassword: (id: number, body: components['schemas']['ResetPasswordRequest']) => request('/api/users/' + id + '/reset-password', { method: 'POST', body: JSON.stringify(body) }, { token: getToken() }, opts)
    },
    assets: {
      list: (page = 0, size = 20, filters: Record<string, any> = {}) => {
        const qs = new URLSearchParams({ page: String(page), size: String(size), ...Object.fromEntries(Object.entries(filters).filter(([k,v]) => v != null).map(([k,v]) => [k, String(v)])) });
        return request<components['schemas']['PageResponseAssetResponse']>(`/api/assets?${qs.toString()}`, { method: 'GET' }, { token: getToken() }, opts);
      },
      getById: (id: number) => request<components['schemas']['AssetResponse']>(`/api/assets/${id}`, { method: 'GET' }, { token: getToken() }, opts),
      create: (body: components['schemas']['CreateAssetRequest']) => request<components['schemas']['AssetResponse']>('/api/assets', { method: 'POST', body: JSON.stringify(body) }, { token: getToken() }, opts),
      update: (id: number, body: components['schemas']['UpdateAssetRequest']) => request<components['schemas']['AssetResponse']>(`/api/assets/${id}`, { method: 'PUT', body: JSON.stringify(body) }, { token: getToken() }, opts),
      assign: (id: number, body: components['schemas']['AssignAssetRequest']) => request<components['schemas']['AssetResponse']>(`/api/assets/${id}/assign`, { method: 'POST', body: JSON.stringify(body) }, { token: getToken() }, opts),
      dispose: (id: number) => request<components['schemas']['AssetResponse']>(`/api/assets/${id}/dispose`, { method: 'POST' }, { token: getToken() }, opts)
    }
    , lookups: {
      departments: () => request<components['schemas']['Department'][]>('/api/lookups/departments', { method: 'GET' }, { token: getToken() }, opts),
      locations: () => request<components['schemas']['Location'][]>('/api/lookups/locations', { method: 'GET' }, { token: getToken() }, opts),
      vendors: () => request<components['schemas']['Vendor'][]>('/api/lookups/vendors', { method: 'GET' }, { token: getToken() }, opts)
    }
    , maintenance: {
      list: (page = 0, size = 20) => request<components['schemas']['PageResponseMaintenanceResponse']>(`/api/maintenance?page=${page}&size=${size}`, { method: 'GET' }, { token: getToken() }, opts),
      getById: (id: number) => request<components['schemas']['MaintenanceResponse']>(`/api/maintenance/${id}`, { method: 'GET' }, { token: getToken() }, opts),
      schedule: (body: components['schemas']['CreateRequest']) => request<components['schemas']['MaintenanceResponse']>('/api/maintenance', { method: 'POST', body: JSON.stringify(body) }, { token: getToken() }, opts),
      updateStatus: (id: number, body: components['schemas']['UpdateStatusRequest']) => request<components['schemas']['MaintenanceResponse']>(`/api/maintenance/${id}/status`, { method: 'PATCH', body: JSON.stringify(body) }, { token: getToken() }, opts)
    },
    reports: {
      inventoryCsv: (filter: components['schemas']['InventoryFilter']) => fetchBlob('/api/reports/inventory/csv', { method: 'POST', body: JSON.stringify(filter), headers: { 'Content-Type': 'application/json' } }, { token: getToken() }, opts),
      maintenanceCsv: (filter: components['schemas']['MaintenanceFilter']) => fetchBlob('/api/reports/maintenance/csv', { method: 'POST', body: JSON.stringify(filter), headers: { 'Content-Type': 'application/json' } }, { token: getToken() }, opts),
      inventoryPdf: (filter: components['schemas']['InventoryFilter']) => fetchBlob('/api/reports/inventory/pdf', { method: 'POST', body: JSON.stringify(filter), headers: { 'Content-Type': 'application/json' } }, { token: getToken() }, opts),
      maintenancePdf: (filter: components['schemas']['MaintenanceFilter']) => fetchBlob('/api/reports/maintenance/pdf', { method: 'POST', body: JSON.stringify(filter), headers: { 'Content-Type': 'application/json' } }, { token: getToken() }, opts)
      , kpis: () => request<components['schemas']['KpiResponse']>('/api/reports/kpis', { method: 'GET' }, { token: getToken() }, opts)
    }
  };
}

// Default Api instance using a simple token getter from localStorage until context wiring
export const Api = createApi(() => localStorage.getItem('clims_token'));

export default Api;