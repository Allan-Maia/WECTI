import type { ReactNode } from 'react';

interface Props {
  titulo: string;
  descricao?: string;
  acao?: ReactNode;
  children: ReactNode;
}

export default function PageContainer({ titulo, descricao, acao, children }: Props) {
  return (
    <div className="mx-auto w-full max-w-6xl px-4 pb-16 pt-28 sm:px-8">
      <div className="mb-8 flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-text sm:text-3xl">{titulo}</h1>
          {descricao && <p className="mt-1 text-sm text-text-muted">{descricao}</p>}
        </div>
        {acao}
      </div>
      {children}
    </div>
  );
}
