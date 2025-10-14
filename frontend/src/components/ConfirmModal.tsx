import React, { useEffect, useRef } from 'react';

export const ConfirmModal: React.FC<{ open: boolean; title?: string; message?: string; onConfirm: () => void; onCancel: () => void }> = ({ open, title='Confirm', message='Are you sure?', onConfirm, onCancel }) => {
  const confirmRef = useRef<HTMLButtonElement | null>(null);
  const cancelRef = useRef<HTMLButtonElement | null>(null);
  const previouslyFocused = useRef<HTMLElement | null>(null);

  useEffect(() => {
    if (open) {
      previouslyFocused.current = document.activeElement as HTMLElement | null;
      // prevent background scroll
      const prevOverflow = document.body.style.overflow;
      document.body.style.overflow = 'hidden';

      const onKey = (e: KeyboardEvent) => {
        if (e.key === 'Escape') {
          e.preventDefault();
          onCancel();
        }
        if (e.key === 'Enter') {
          e.preventDefault();
          onConfirm();
        }
        if (e.key === 'Tab') {
          // simple focus trap between cancel and confirm
          const active = document.activeElement;
          if (e.shiftKey) {
            if (active === cancelRef.current) {
              e.preventDefault();
              confirmRef.current?.focus();
            }
          } else {
            if (active === confirmRef.current) {
              e.preventDefault();
              cancelRef.current?.focus();
            }
          }
        }
      };

      document.addEventListener('keydown', onKey);
      // focus confirm button for keyboard users
      setTimeout(() => confirmRef.current?.focus(), 0);
      return () => {
        document.removeEventListener('keydown', onKey);
        document.body.style.overflow = prevOverflow;
        // restore previous focus
        previouslyFocused.current?.focus();
      };
    }
    // if not open, restore any previous focus
    return undefined;
  }, [open, onCancel, onConfirm]);

  if (!open) return null;
  const onBackdropMouseDown = (e: React.MouseEvent) => {
    // close if user clicked the backdrop (outside dialog)
    if (e.target === e.currentTarget) onCancel();
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-40 flex items-center justify-center z-50" role="dialog" aria-modal="true" aria-labelledby="confirm-title" aria-describedby="confirm-desc" onMouseDown={onBackdropMouseDown}>
      <div className="bg-white rounded shadow p-6 w-96" onMouseDown={(e) => e.stopPropagation()}>
        <h3 id="confirm-title" className="font-semibold mb-2">{title}</h3>
        <p id="confirm-desc" className="mb-4 text-sm text-gray-700">{message}</p>
        <div className="flex justify-end gap-2">
          <button ref={cancelRef} onClick={onCancel} className="px-3 py-1 border rounded">Cancel</button>
          <button ref={confirmRef} onClick={onConfirm} className="px-3 py-1 bg-red-600 text-white rounded">Confirm</button>
        </div>
      </div>
    </div>
  );
};

export default ConfirmModal;
