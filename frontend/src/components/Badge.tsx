const ESTILOS: Record<string, string> = {
  ativa: 'bg-accent/10 text-accent border-accent/30',
  concluido: 'bg-accent/10 text-accent border-accent/30',
  cancelada: 'bg-white/5 text-text-muted border-border',
  cancelado: 'bg-white/5 text-text-muted border-border',
  no_show: 'bg-red-500/10 text-red-400 border-red-500/30',
};

const LABELS: Record<string, string> = {
  ativa: 'Ativa',
  concluido: 'Concluído',
  cancelada: 'Cancelada',
  cancelado: 'Cancelado',
  no_show: 'Não compareceu',
};

export default function Badge({ status }: { status: string }) {
  return (
    <span
      className={`inline-flex items-center rounded-full border px-3 py-1 text-xs font-semibold ${
        ESTILOS[status] ?? 'border-border bg-white/5 text-text-muted'
      }`}
    >
      {LABELS[status] ?? status}
    </span>
  );
}
