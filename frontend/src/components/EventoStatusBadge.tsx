import { statusEvento } from '../utils/data';

const ESTILOS = {
  finalizado: 'bg-white/5 text-text-muted border-border',
  em_andamento: 'bg-accent/10 text-accent border-accent/30',
  nao_iniciado: 'bg-blue-500/10 text-blue-400 border-blue-500/30',
};

const LABELS = {
  finalizado: 'Finalizado',
  em_andamento: 'Em andamento',
  nao_iniciado: 'Não iniciado',
};

interface Props {
  dataHoraInicio: string;
  dataHoraFim: string;
}

/** Badge de status do evento (Finalizado/Em andamento/Não iniciado) -
 *  calculado no frontend a partir das datas, sem campo novo no banco. */
export default function EventoStatusBadge({ dataHoraInicio, dataHoraFim }: Props) {
  const status = statusEvento(dataHoraInicio, dataHoraFim);
  return (
    <span
      className={`inline-flex shrink-0 items-center rounded-full border px-3 py-1 text-xs font-semibold ${ESTILOS[status]}`}
    >
      {LABELS[status]}
    </span>
  );
}
