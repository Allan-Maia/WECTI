interface Props {
  tamanho?: 'sm' | 'md' | 'lg';
  className?: string;
}

const TAMANHOS: Record<NonNullable<Props['tamanho']>, string> = {
  sm: 'h-4 w-4 border-2',
  md: 'h-8 w-8 border-2',
  lg: 'h-12 w-12 border-[3px]',
};

export default function LoadingSpinner({ tamanho = 'md', className = '' }: Props) {
  return (
    <span
      role="status"
      aria-label="Carregando"
      className={`inline-block ${TAMANHOS[tamanho]} animate-spin rounded-full border-accent/25 border-t-accent ${className}`}
    />
  );
}

export function LoadingBlock({ mensagem = 'Carregando...' }: { mensagem?: string }) {
  return (
    <div className="flex flex-col items-center justify-center gap-3 py-16 text-text-muted">
      <LoadingSpinner tamanho="lg" />
      <p className="text-sm">{mensagem}</p>
    </div>
  );
}
