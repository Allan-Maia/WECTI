interface Props {
  mensagem: string;
  onTentarNovamente?: () => void;
}

export default function ErrorMessage({ mensagem, onTentarNovamente }: Props) {
  return (
    <div className="flex flex-col items-center gap-3 rounded-card border border-red-500/30 bg-surface px-6 py-10 text-center">
      <span className="text-2xl">⚠️</span>
      <p className="text-sm text-text-muted">{mensagem}</p>
      {onTentarNovamente && (
        <button
          onClick={onTentarNovamente}
          className="mt-1 rounded-full border border-accent px-4 py-1.5 text-sm font-medium text-accent transition hover:bg-accent/10"
        >
          Tentar novamente
        </button>
      )}
    </div>
  );
}
