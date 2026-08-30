import type { ReactNode } from 'react';
import type { Evento } from '../types';
import { formatarDataHora } from '../utils/data';
import EventoStatusBadge from './EventoStatusBadge';

interface Props {
  evento: Evento;
  /** Slot de acao (botao "Inscrever-se", editar/excluir, etc.) - mantem o
   *  card reutilizavel entre a area do aluno e a area de admin. */
  acao?: ReactNode;
}

/**
 * Ocupação das vagas. Evento sem limite não mostra nada - "23 inscritos"
 * sozinho não ajuda o aluno a decidir e só polui o card.
 *
 * Fica em destaque quando aperta (últimas vagas ou lotado), porque é
 * exatamente aí que a informação muda o comportamento de quem lê.
 */
function VagasDoEvento({ evento }: { evento: Evento }) {
  if (evento.capacidade == null || evento.vagas_restantes == null) return null;

  const restantes = evento.vagas_restantes;
  const cor = restantes === 0 ? 'text-red-400' : restantes <= 10 ? 'text-amber-400' : 'text-text-muted';

  return (
    <span className={`flex items-center gap-2 ${cor}`}>
      <span aria-hidden>🎟️</span>
      {restantes === 0 ? (
        <span className="font-medium">Vagas esgotadas ({evento.capacidade} lugares)</span>
      ) : (
        <>
          <span className={restantes <= 10 ? 'font-medium' : ''}>
            {restantes} {restantes === 1 ? 'vaga restante' : 'vagas restantes'}
          </span>
          <span className="text-text-muted/70">
            de {evento.capacidade}
          </span>
        </>
      )}
    </span>
  );
}

export default function EventoCard({ evento, acao }: Props) {
  return (
    <div className="group flex flex-col gap-3 rounded-card border border-border bg-surface p-5 transition-colors duration-200 hover:border-accent">
      <div className="flex items-start justify-between gap-3">
        <h3 className="text-base font-semibold leading-snug text-text">{evento.titulo}</h3>
        <span className="shrink-0 rounded-full border border-accent/30 bg-accent/10 px-3 py-1 text-xs font-bold text-accent">
          {evento.pontos} pts
        </span>
      </div>

      <EventoStatusBadge dataHoraInicio={evento.data_hora_inicio} dataHoraFim={evento.data_hora_fim} />

      {evento.descricao && <p className="line-clamp-2 text-sm text-text-muted">{evento.descricao}</p>}

      <div className="flex flex-col gap-1.5 text-sm text-text-muted">
        <span className="flex items-center gap-2">
          <span aria-hidden>🗓️</span> {formatarDataHora(evento.data_hora_inicio)}
        </span>
        {evento.local && (
          <span className="flex items-center gap-2">
            <span aria-hidden>📍</span> {evento.local}
          </span>
        )}
        {evento.palestrantes.length > 0 && (
          <span className="flex items-center gap-2">
            <span aria-hidden>🎤</span> {evento.palestrantes.map((p) => p.nome).join(', ')}
          </span>
        )}
        <VagasDoEvento evento={evento} />
      </div>

      {acao && <div className="mt-2 flex gap-2">{acao}</div>}
    </div>
  );
}
