import React, { createContext, useContext, useState, useCallback, useRef } from 'react';

type Toast = { id: number; message: string; type?: 'info' | 'success' | 'error' };

const ToastContext = createContext<any>(null);

export const ToastProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [toasts, setToasts] = useState<Toast[]>([]);
  // timers tracked so we can pause/resume on hover
  const timers = useRef<Map<number, number>>(new Map());

  const remove = useCallback((id: number) => {
    // clear any timer
    const t = timers.current.get(id);
    if (t) window.clearTimeout(t);
    timers.current.delete(id);
    setToasts(s => s.filter(x => x.id !== id));
  }, []);

  const push = useCallback((message: string, type: Toast['type'] = 'info', durationMs?: number) => {
    const id = Date.now() + Math.floor(Math.random() * 1000);
    const t = { id, message, type };
    setToasts(s => [...s, t]);
    const defaultMs = durationMs ?? (type === 'error' ? 8000 : type === 'success' ? 4000 : 5000);
    const timer = window.setTimeout(() => remove(id), defaultMs);
    timers.current.set(id, timer);
  }, [remove]);

  const pause = (id: number) => {
    const t = timers.current.get(id);
    if (t) {
      window.clearTimeout(t);
      timers.current.delete(id);
    }
  };

  const resume = (id: number) => {
    if (!timers.current.has(id)) {
      const timer = window.setTimeout(() => remove(id), 3000);
      timers.current.set(id, timer);
    }
  };

  return (
    <ToastContext.Provider value={{ push, remove }}>
      {children}
      <div className="fixed bottom-4 right-4 flex flex-col gap-2 z-50" aria-live="polite" aria-atomic="true">
        {toasts.map(t => (
          <div key={t.id} onMouseEnter={() => pause(t.id)} onMouseLeave={() => resume(t.id)} onFocus={() => pause(t.id)} onBlur={() => resume(t.id)} tabIndex={0} className={`px-4 py-2 rounded shadow text-white flex items-start gap-3 max-w-xs transition-opacity duration-200 ease-out ${t.type === 'error' ? 'bg-red-600' : t.type === 'success' ? 'bg-green-600' : 'bg-gray-800'}`} role="status">
            <div className="flex-1">{t.message}</div>
            <button onClick={() => remove(t.id)} aria-label="Dismiss" className="text-white opacity-80 hover:opacity-100">×</button>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  );
};

export const useToast = () => {
  const ctx = useContext(ToastContext);
  if (!ctx) throw new Error('useToast must be used within ToastProvider');
  return ctx as { push: (m: string, t?: Toast['type']) => void; remove: (id: number) => void };
};

export default ToastProvider;
