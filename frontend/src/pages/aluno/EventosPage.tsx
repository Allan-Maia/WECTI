import { useState } from 'react';
import PageContainer from '../../components/PageContainer';
import EventoCard from '../../components/EventoCard';
import Button from '../../components/Button';
import { LoadingBlock } from '../../components/LoadingSpinner';
import ErrorMessage from '../../components/ErrorMessage';
import EmptyState from '../../components/EmptyState';
import { useEventos } from '../../hooks/useEventos';
import { useInscricoes } from '../../hooks/useInscricoes';
import { inscreverEmEvento } from '../../services/inscricoes';
import { extrairMensagemErro } from '../../services/api';
import { useToast } from '../../context/ToastContext';

export default function EventosPage() {
  const { eventos, carregando, erro, recarregar } = useEventos({ status: 'futuros' });
  const { inscricoes, recarregar: recarregarInscricoes } = useInscricoes();
  const { notificarSucesso, notificarErro } = useToast();
  const [inscrevendoId, setInscrevendoId] = useState<string | null>(null);

  const jaInscrito = (eventoId: string) =>
    inscricoes.some((i) => i.evento_id === eventoId && i.status === 'ativa');

  const inscrever = async (eventoId: string) => {
    setInscrevendoId(eventoId);
    try {
      await inscreverEmEvento(eventoId);
      notificarSucesso('Inscrição realizada! Sua presença é confirmada no dia, lendo o QR code do evento.');
      recarregarInscricoes();
      // Recarrega os eventos também: a vaga que o aluno acabou de ocupar
      // precisa sumir do contador antes que ele (ou outro) olhe de novo.
      recarregar();
    } catch (erroInscricao) {
      notificarErro(extrairMensagemErro(erroInscricao, 'Não foi possível se inscrever.'));
    } finally {
      setInscrevendoId(null);
    }
  };

  return (
    <PageContainer titulo="Eventos" descricao="Palestras e eventos abertos para inscrição">
      {carregando && <LoadingBlock mensagem="Carregando eventos..." />}
      {!carregando && erro && <ErrorMessage mensagem={erro} onTentarNovamente={recarregar} />}
      {!carregando && !erro && eventos.length === 0 && (
        <EmptyState titulo="Nenhum evento disponível" descricao="Ainda não há eventos futuros abertos para inscrição." icone="🗓️" />
      )}
      {!carregando && !erro && eventos.length > 0 && (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {eventos.map((evento) => (
            <EventoCard
              key={evento.id}
              evento={evento}
              acao={
                jaInscrito(evento.id) ? (
                  <Button variante="outline" disabled className="w-full">
                    Inscrito
                  </Button>
                ) : evento.lotado ? (
                  // Continua visível, e não escondido: o aluno precisa
                  // saber que o evento existe e encheu - some da tela e
                  // ele acha que houve erro.
                  <Button variante="outline" disabled className="w-full">
                    Vagas esgotadas
                  </Button>
                ) : (
                  <Button
                    carregando={inscrevendoId === evento.id}
                    onClick={() => inscrever(evento.id)}
                    className="w-full"
                  >
                    Inscrever-se
                  </Button>
                )
              }
            />
          ))}
        </div>
      )}
    </PageContainer>
  );
}
