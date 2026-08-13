import { createContext, useCallback, useContext, useState, type ReactNode } from 'react';

type TipoToast = 'sucesso' | 'erro';

interface ToastItem {
  id: number;
  tipo: TipoToast;
  mensagem: string;
}

interface ToastContextValue {
  notificarSucesso: (mensagem: string) => void;
  notificarErro: (mensagem: string) => void;
}

const ToastContext = createContext<ToastContextValue | undefined>(undefined);

let proximoId = 1;

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<ToastItem[]>([]);

  const remover = useCallback((id: number) => {
    setToasts((atual) => atual.filter((t) => t.id !== id));
  }, []);

  const adicionar = useCallback(
    (tipo: TipoToast, mensagem: string) => {
      const id = proximoId++;
      setToasts((atual) => [...atual, { id, tipo, mensagem }]);
      setTimeout(() => remover(id), 4000);
    },
    [remover]
  );

  const value: ToastContextValue = {
    notificarSucesso: (mensagem) => adicionar('sucesso', mensagem),
    notificarErro: (mensagem) => adicionar('erro', mensagem),
  };

  return (
    <ToastContext.Provider value={value}>
      {children}
      <div className="fixed bottom-4 right-4 z-[999] flex flex-col gap-2 w-[min(360px,calc(100vw-2rem))]">
        {toasts.map((toast) => (
          <div
            key={toast.id}
            role="status"
            className={`rounded-card border px-4 py-3 text-sm shadow-lg backdrop-blur-sm animate-toast-in ${
              toast.tipo === 'sucesso'
                ? 'bg-surface border-accent/40 text-text'
                : 'bg-surface border-red-500/40 text-text'
            }`}
          >
            <span
              className={`mr-2 inline-block h-2 w-2 rounded-full ${
                toast.tipo === 'sucesso' ? 'bg-accent' : 'bg-red-400'
              }`}
            />
            {toast.mensagem}
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  );
}

export function useToast() {
  const ctx = useContext(ToastContext);
  if (!ctx) {
    throw new Error('useToast precisa ser usado dentro de um ToastProvider');
  }
  return ctx;
}
