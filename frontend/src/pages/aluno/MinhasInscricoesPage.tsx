import { useMemo, useState } from 'react';
import PageContainer from '../../components/PageContainer';
import Badge from '../../components/Badge';
import Button from '../../components/Button';
import { LoadingBlock } from '../../components/LoadingSpinner';
import ErrorMessage from '../../components/ErrorMessage';
import EmptyState from '../../components/EmptyState';
import { useInscricoes } from '../../hooks/useInscricoes';
import { useEventos } from '../../hooks/useEventos';
import { cancelarInscricao } from '../../services/inscricoes';
import { extrairMensagemErro } from '../../services/api';
import { useToast } from '../../context/ToastContext';
import { formatarDataHora } from '../../utils/data';

export default function MinhasInscricoesPage() {
  const { inscricoes, carregando, erro, recarregar } = useInscricoes('futuros');
  const { eventos } = useEventos({});
  const { notificarSucesso, notificarErro } = useToast();
  const [cancelandoId, setCancelandoId] = useState<string | null>(null);

  const eventosPorId = useMemo(() => new Map(eventos.map((e) => [e.id, e])), [eventos]);

  const cancelar = async (inscricaoId: string) => {
    if (!window.confirm('Cancelar esta inscrição?')) return;
    setCancelandoId(inscricaoId);
    try {
      await cancelarInscricao(inscricaoId);
      notificarSucesso('Inscrição cancelada.');
      recarregar();
    } catch (e) {
      notificarErro(extrairMensagemErro(e, 'Não foi possível cancelar - o prazo pode ter passado.'));
    } finally {
      setCancelandoId(null);
    }
  };

  return (
    <PageContainer titulo="Minhas inscrições" descricao="Seus próximos eventos">
      {carregando && <LoadingBlock mensagem="Carregando inscrições..." />}
      {!carregando && erro && <ErrorMessage mensagem={erro} onTentarNovamente={recarregar} />}
      {!carregando && !erro && inscricoes.length === 0 && (
        <EmptyState titulo="Nenhuma inscrição" descricao="Você ainda não se inscreveu em nenhum evento futuro." icone="📋" />
      )}
      {!carregando && !erro && inscricoes.length > 0 && (
        <div className="flex flex-col gap-3">
          {inscricoes.map((inscricao) => {
            const evento = eventosPorId.get(inscricao.evento_id);
            return (
              <div
                key={inscricao.id}
                className="flex flex-col gap-3 rounded-card border border-border bg-surface p-5 sm:flex-row sm:items-center sm:justify-between"
              >
                <div>
                  <div className="mb-1 flex items-center gap-2">
                    <h3 className="font-semibold text-text">{evento?.titulo ?? 'Evento'}</h3>
                    <Badge status={inscricao.status} />
                  </div>
                  <p className="text-sm text-text-muted">
                    {evento ? formatarDataHora(evento.data_hora_inicio) : ''}
                    {evento?.local ? ` · ${evento.local}` : ''}
                  </p>
                  <p className="mt-1 text-xs text-text-muted">
                    {inscricao.checkin?.saida
                      ? 'Check-in e check-out confirmados'
                      : inscricao.checkin?.entrada
                        ? 'Check-in confirmado - falta o check-out'
                        : 'Presença ainda não confirmada - escaneie o QR code na entrada do evento'}
                  </p>
                </div>
                <div className="flex gap-2">
                  {inscricao.status === 'ativa' && (
                    <Button
                      variante="danger"
                      carregando={cancelandoId === inscricao.id}
                      onClick={() => cancelar(inscricao.id)}
                    >
                      Cancelar
                    </Button>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}
    </PageContainer>
  );
}
