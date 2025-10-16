import React from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { useAuth } from '../auth/AuthContext';
import { createApi } from '../../api/client';
import { useToast } from '../../components/ToastContext';
import ConfirmModal from '../../components/ConfirmModal';

export const AssetDetailPage: React.FC = () => {
  const { id } = useParams();
  const { token } = useAuth();
  const api = createApi(() => token);
  const nav = useNavigate();

  const { data, isLoading } = useQuery({
    queryKey: ['asset', id, token],
    queryFn: async () => api.assets.getById(Number(id)),
    enabled: !!id && !!token
  });

  const toast = useToast();
  const [confirmOpen, setConfirmOpen] = React.useState(false);
  const { currentUser } = useAuth();

  const canDispose = (a: any) => {
    const role = currentUser?.role ?? '';
    if (role === 'ADMIN' || role === 'IT_STAFF' || role === 'FINANCE') return true;
    if (role === 'MANAGER') return !!(currentUser?.department && a?.department && currentUser.department === a.department);
    return false;
  };
  const onDispose = async () => {
    if (!id) return;
    try {
      await api.assets.dispose(Number(id));
      toast.push('Asset disposed', 'success');
      nav('/assets');
    } catch (e: any) {
      toast.push(e?.message ?? 'Failed to dispose', 'error');
    }
  };

  return (
    <div className="p-6">
      {isLoading && <div>Loading...</div>}
      {data && (
        <div>
          <h1 className="text-2xl font-semibold">{data.assetTag ?? `Asset ${data.id}`}</h1>
          <div className="mt-4 grid grid-cols-2 gap-4">
            <div className="border p-3">
              <div><strong>Serial:</strong> {data.serialNumber ?? '-'}</div>
              <div><strong>Make:</strong> {data.make ?? '-'}</div>
              <div><strong>Model:</strong> {data.model ?? '-'}</div>
              <div><strong>Status:</strong> {data.status ?? '-'}</div>
            </div>
            <div className="border p-3">
              <div><strong>Assigned To:</strong> {data.assignedTo ?? '-'}</div>
              <div><strong>Location:</strong> {data.location ?? '-'}</div>
              <div><strong>Vendor:</strong> {data.vendor ?? '-'}</div>
            </div>
          </div>
          <div className="mt-4 flex gap-2">
            {canDispose(data) ? (
              <button onClick={() => setConfirmOpen(true)} className="px-3 py-1 bg-red-600 text-white rounded">Dispose</button>
            ) : (
              <button disabled className="px-3 py-1 bg-red-200 text-white rounded opacity-60" title="You don't have permission to dispose this asset">Dispose</button>
            )}
          </div>
          <ConfirmModal open={confirmOpen} title="Dispose Asset" message={`Dispose asset ${data.assetTag ?? data.id}?`} onConfirm={() => { setConfirmOpen(false); onDispose(); }} onCancel={() => setConfirmOpen(false)} />
        </div>
      )}
    </div>
  );
};

export default AssetDetailPage;
