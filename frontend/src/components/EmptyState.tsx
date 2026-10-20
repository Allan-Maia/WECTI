interface Props {
  titulo: string;
  descricao?: string;
  icone?: string;
  acao?: React.ReactNode;
}

export default function EmptyState({ titulo, descricao, icone = '🗂️', acao }: Props) {
  return (
    <div className="flex flex-col items-center gap-2 rounded-card border border-border bg-surface px-6 py-16 text-center">
      <span className="mb-2 text-3xl opacity-60">{icone}</span>
      <p className="font-semibold text-text">{titulo}</p>
      {descricao && <p className="max-w-sm text-sm text-text-muted">{descricao}</p>}
      {acao && <div className="mt-3">{acao}</div>}
    </div>
  );
}
