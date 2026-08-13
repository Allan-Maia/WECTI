import { useState } from 'react';
import PageContainer from '../../components/PageContainer';
import Button from '../../components/Button';
import { LoadingBlock } from '../../components/LoadingSpinner';
import ErrorMessage from '../../components/ErrorMessage';
import EmptyState from '../../components/EmptyState';
import EventoFormModal from './EventoFormModal';
import { useEventos } from '../../hooks/useEventos';
import { atualizarEvento, cancelarEvento, criarEvento } from '../../services/eventos';
import { extrairMensagemErro } from '../../services/api';
import { useToast } from '../../context/ToastContext';
import { formatarDataHora } from '../../utils/data';
import type { Evento, NovoEvento } from '../../types';

export default function AdminEventosPage() {
  const { eventos, carregando, erro, recarregar } = useEventos({});
  const { notificarSucesso, notificarErro } = useToast();
  const [modalAberto, setModalAberto] = useState(false);
  const [eventoEditando, setEventoEditando] = useState<Evento | null>(null);
  const [excluindoId, setExcluindoId] = useState<string | null>(null);

  const abrirNovo = () => {
    setEventoEditando(null);
    setModalAberto(true);
  };

  const abrirEdicao = (evento: Evento) => {
    setEventoEditando(evento);
    setModalAberto(true);
  };

  const salvar = async (dados: NovoEvento) => {
    try {
      if (eventoEditando) {
        await atualizarEvento(eventoEditando.id, dados);
        notificarSucesso('Evento atualizado.');
      } else {
        await criarEvento(dados);
        notificarSucesso('Evento criado.');
      }
      setModalAberto(false);
      recarregar();
    } catch (e) {
      throw new Error(extrairMensagemErro(e, 'Não foi possível salvar o evento.'));
    }
  };

  const excluir = async (evento: Evento) => {
    if (!window.confirm(`Cancelar o evento "${evento.titulo}"?`)) return;
    setExcluindoId(evento.id);
    try {
      await cancelarEvento(evento.id);
      notificarSucesso('Evento cancelado.');
      recarregar();
    } catch (e) {
      notificarErro(extrairMensagemErro(e, 'Não foi possível cancelar - o evento já possui inscrições.'));
    } finally {
      setExcluindoId(null);
    }
  };

  return (
    <PageContainer
      titulo="Eventos"
      descricao="Gerencie os eventos cadastrados"
      acao={<Button onClick={abrirNovo}>Novo evento</Button>}
    >
      {carregando && <LoadingBlock mensagem="Carregando eventos..." />}
      {!carregando && erro && <ErrorMessage mensagem={erro} onTentarNovamente={recarregar} />}
      {!carregando && !erro && eventos.length === 0 && (
        <EmptyState titulo="Nenhum evento cadastrado" acao={<Button onClick={abrirNovo}>Criar o primeiro evento</Button>} />
      )}
      {!carregando && !erro && eventos.length > 0 && (
        <div className="flex flex-col gap-3">
          {eventos.map((evento) => (
            <div
              key={evento.id}
              className="flex flex-col gap-3 rounded-card border border-border bg-surface p-5 sm:flex-row sm:items-center sm:justify-between"
            >
              <div>
                <h3 className="font-semibold text-text">{evento.titulo}</h3>
                <p className="text-sm text-text-muted">
                  {formatarDataHora(evento.data_hora_inicio)} · {evento.pontos} pts
                  {evento.local ? ` · ${evento.local}` : ''}
                </p>
              </div>
              <div className="flex gap-2">
                <Button variante="outline" onClick={() => abrirEdicao(evento)}>
                  Editar
                </Button>
                <Button variante="danger" carregando={excluindoId === evento.id} onClick={() => excluir(evento)}>
                  Cancelar
                </Button>
              </div>
            </div>
          ))}
        </div>
      )}

      {modalAberto && (
        <EventoFormModal evento={eventoEditando} onSalvar={salvar} onFechar={() => setModalAberto(false)} />
      )}
    </PageContainer>
  );
}
