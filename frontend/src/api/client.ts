import type { paths } from './schema';

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

// Factory allowing injection (e.g., for tests or future auth refresh logic)
export function createApi(getToken: () => string | null, opts: ApiClientOptions = {}) {
  return {
    auth: {
      login: (body: { username: string; password: string }) =>
        request<{ token: string }>('/api/auth/login', { method: 'POST', body: JSON.stringify(body) }, {}, opts),
      register: (body: { username: string; password: string; email?: string }) =>
        request('/api/auth/register', { method: 'POST', body: JSON.stringify(body) }, {}, opts),
      me: () => request<paths['/api/auth/me']['get']['responses']['200']['content']['*/*']>('/api/auth/me', { method: 'GET' }, { token: getToken() }, opts)
    },
    assets: {
      list: (page = 0, size = 20) => request(`/api/assets?page=${page}&size=${size}`, { method: 'GET' }, { token: getToken() }, opts)
    }
  };
}

// Default Api instance using a simple token getter from localStorage until context wiring
export const Api = createApi(() => localStorage.getItem('clims_token'));

export default Api;