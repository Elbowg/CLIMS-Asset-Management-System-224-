import React from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '../auth/AuthContext';
import { createApi } from '../../api/client';
import type { components } from '../../api/schema';
import { useToast } from '../../components/ToastContext';

export const AssetFormPage: React.FC = () => {
  const { id } = useParams();
  const isEdit = !!id && id !== 'new';
  const { token } = useAuth();
  const { currentUser } = useAuth();
  const api = createApi(() => token);
  const nav = useNavigate();
  const toast = useToast();
  const qc = useQueryClient();

  const { data: lookups } = useQuery({
    queryKey: ['lookups', token],
    queryFn: async () => {
      const [departments, locations, vendors] = await Promise.all([
        api.lookups.departments(),
        api.lookups.locations(),
        api.lookups.vendors()
      ]);
      return { departments, locations, vendors } as { departments: components['schemas']['Department'][]; locations: components['schemas']['Location'][]; vendors: components['schemas']['Vendor'][] };
    },
    enabled: !!token
  });

  const { data: asset, isLoading } = useQuery({
    queryKey: ['asset', id, token],
    queryFn: async () => api.assets.getById(Number(id)),
    enabled: !!id && !!token && isEdit
  });

  const [form, setForm] = React.useState({
    serialNumber: '',
    make: '',
    model: '',
    purchaseDate: '',
    warrantyExpiryDate: '',
    locationId: undefined as number | undefined,
    vendorId: undefined as number | undefined,
    departmentId: undefined as number | undefined
  });

  React.useEffect(() => {
    if (asset) {
      // asset comes from the API and matches components['schemas']['AssetResponse'] shape
      const a = asset as components['schemas']['AssetResponse'] & Partial<components['schemas']['CreateAssetRequest']>;
      setForm({
        serialNumber: a.serialNumber ?? '',
        make: a.make ?? '',
        model: a.model ?? '',
        purchaseDate: (a.purchaseDate as string) ?? '',
        warrantyExpiryDate: (a.warrantyExpiryDate as string) ?? '',
        locationId: (a.locationId as number) ?? undefined,
        vendorId: (a.vendorId as number) ?? undefined,
        departmentId: (a.departmentId as number) ?? undefined
      });
    }
  }, [asset]);

  // If current user is a MANAGER, force departmentId to their department and disable changing it
  React.useEffect(() => {
    if (currentUser?.role === 'MANAGER' && currentUser.department) {
      // find department id from lookups if available
      const dept = lookups?.departments?.find((d: any) => d.name === currentUser.department);
      if (dept) setForm(f => ({ ...f, departmentId: dept.id }));
    }
  }, [currentUser, lookups]);

  // If creating new, prefill purchaseDate to today
  React.useEffect(() => {
    if (!isEdit) {
      const today = new Date().toISOString().slice(0,10);
      setForm(f => ({ ...f, purchaseDate: f.purchaseDate || today }));
    }
  }, [isEdit]);

  const updateField = (k: string, v: any) => setForm(f => ({ ...f, [k]: v }));

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    // basic validation
    if (!form.serialNumber || !form.make || !form.model || !form.purchaseDate) {
      toast.push('Please fill required fields (serial, make, model, purchase date)', 'error');
      return;
    }
    try {
        if (isEdit && id) {
        await api.assets.update(Number(id), {
          make: form.make || undefined,
          model: form.model || undefined,
          warrantyExpiryDate: form.warrantyExpiryDate || undefined,
          locationId: form.locationId,
          departmentId: form.departmentId
        });
        // update cached asset detail
        qc.setQueryData(['asset', Number(id), token], (prev: components['schemas']['AssetResponse'] | undefined) => ({ ...(prev ?? {}), make: form.make, model: form.model, warrantyExpiryDate: form.warrantyExpiryDate, locationId: form.locationId } as components['schemas']['AssetResponse']));
        // update any assets list caches that start with ['assets']
  qc.setQueriesData({ queryKey: ['assets'] }, (old: any) => {
          if (!old) return old;
          // paged response
          if (Array.isArray(old)) {
            return old.map((a: any) => a.id === Number(id) ? { ...a, make: form.make, model: form.model } : a);
          }
          if (old.content && Array.isArray(old.content)) {
            return { ...old, content: old.content.map((a: any) => a.id === Number(id) ? { ...a, make: form.make, model: form.model } : a) };
          }
          return old;
        });
        toast.push('Asset updated', 'success');
        nav(`/assets/${id}`);
      } else {
        const created = await api.assets.create({
          serialNumber: form.serialNumber,
          make: form.make,
          model: form.model,
          purchaseDate: form.purchaseDate,
          warrantyExpiryDate: form.warrantyExpiryDate || undefined,
          locationId: form.locationId,
          vendorId: form.vendorId,
          departmentId: form.departmentId
        });
        // optimistically insert into any cached assets lists
  qc.setQueriesData({ queryKey: ['assets'] }, (old: any) => {
          if (!old) return old;
          if (Array.isArray(old)) {
            return [created, ...old];
          }
          if (old.content && Array.isArray(old.content)) {
            return { ...old, content: [created, ...(old.content || [])], totalElements: ((old.totalElements || 0) + 1) };
          }
          return old;
        });
        // set asset detail cache directly
        qc.setQueryData(['asset', created.id, token], created);
        toast.push('Asset created', 'success');
        nav(`/assets/${created.id ?? ''}`);
      }
    } catch (err: any) {
      console.error(err);
      toast.push(err?.message ?? 'Failed to save asset', 'error');
    }
  };

  return (
    <div className="p-6">
      <h1 className="text-2xl font-semibold mb-4">{isEdit ? 'Edit Asset' : 'New Asset'}</h1>
      {isLoading && <div>Loading...</div>}
  <form onSubmit={onSubmit} className="max-w-lg">
        <div className="mb-3">
          <label className="block text-sm mb-1">Serial Number <span className="text-red-500">*</span></label>
          <input value={form.serialNumber} onChange={e => updateField('serialNumber', e.target.value)} className="w-full border px-2 py-1" />
        </div>
        <div className="mb-3">
          <label className="block text-sm mb-1">Make <span className="text-red-500">*</span></label>
          <input value={form.make} onChange={e => updateField('make', e.target.value)} className="w-full border px-2 py-1" />
        </div>
        <div className="mb-3">
          <label className="block text-sm mb-1">Model <span className="text-red-500">*</span></label>
          <input value={form.model} onChange={e => updateField('model', e.target.value)} className="w-full border px-2 py-1" />
        </div>
        <div className="mb-3">
          <label className="block text-sm mb-1">Purchase Date <span className="text-red-500">*</span></label>
          <input type="date" value={form.purchaseDate} onChange={e => updateField('purchaseDate', e.target.value)} className="w-full border px-2 py-1" />
        </div>
        <div className="mb-3">
          <label className="block text-sm mb-1">Warranty Expiry</label>
          <input type="date" value={form.warrantyExpiryDate} onChange={e => updateField('warrantyExpiryDate', e.target.value)} className="w-full border px-2 py-1" />
        </div>
        <div className="mb-3">
          <label className="block text-sm mb-1">Location</label>
          <select className="w-full border px-2 py-1" value={form.locationId ?? ''} onChange={e => updateField('locationId', e.target.value ? Number(e.target.value) : undefined)}>
            <option value="">-- select --</option>
            {(lookups?.locations ?? []).map((l: any) => <option value={l.id} key={l.id}>{l.name}</option>)}
          </select>
        </div>
        <div className="mb-3">
          <label className="block text-sm mb-1">Department</label>
          <select className="w-full border px-2 py-1" value={form.departmentId ?? ''} onChange={e => updateField('departmentId', e.target.value ? Number(e.target.value) : undefined)} disabled={currentUser?.role === 'MANAGER'}>
            <option value="">-- select --</option>
            {(lookups?.departments ?? []).map((d: any) => <option value={d.id} key={d.id}>{d.name}</option>)}
          </select>
          {currentUser?.role === 'MANAGER' && (
            <div className="text-xs text-gray-500 mt-1">Your department is preselected and cannot be changed.</div>
          )}
        </div>
        <div className="mb-3">
          <label className="block text-sm mb-1">Vendor</label>
          <select className="w-full border px-2 py-1" value={form.vendorId ?? ''} onChange={e => updateField('vendorId', e.target.value ? Number(e.target.value) : undefined)}>
            <option value="">-- select --</option>
            {(lookups?.vendors ?? []).map((v: any) => <option value={v.id} key={v.id}>{v.name}</option>)}
          </select>
        </div>
        <div className="flex gap-2">
          <button type="submit" disabled={!form.serialNumber || !form.make || !form.model || !form.purchaseDate} className="px-3 py-1 bg-blue-600 text-white rounded disabled:opacity-50">Save</button>
          <button type="button" onClick={() => nav('/assets')} className="px-3 py-1 border rounded">Cancel</button>
        </div>
      </form>
    </div>
  );
};

export default AssetFormPage;
