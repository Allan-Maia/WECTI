import { useEffect, useMemo, useState } from 'react';
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
import { formatarDataHora, statusEvento, type StatusEvento } from '../../utils/data';
import EventoStatusBadge from '../../components/EventoStatusBadge';
import type { Evento, NovoEvento } from '../../types';

const TAMANHO_PAGINA = 10;

export default function AdminEventosPage() {
  // GET /eventos não é paginado no backend de propósito: além dessa
  // tela, o dropdown do Check-in e as páginas do aluno (certificados,
  // minhas inscrições, eventos abertos) também consomem essa mesma
  // lista e precisam dela completa, não de uma página - paginar aqui
  // é feito em cima da lista já carregada, sem mudar o contrato
  // compartilhado da API (diferente de Usuários, que só tem um
  // consumidor).
  const { eventos, carregando, erro, recarregar } = useEventos({});
  const { notificarSucesso, notificarErro } = useToast();
  const [modalAberto, setModalAberto] = useState(false);
  const [eventoEditando, setEventoEditando] = useState<Evento | null>(null);
  const [excluindoId, setExcluindoId] = useState<string | null>(null);

  const [busca, setBusca] = useState('');
  const [filtroStatus, setFiltroStatus] = useState<StatusEvento | ''>('');
  const [pagina, setPagina] = useState(0);

  useEffect(() => {
    setPagina(0);
  }, [busca, filtroStatus]);

  const eventosFiltrados = useMemo(() => {
    const termo = busca.trim().toLowerCase();
    return eventos.filter((evento) => {
      const bateBusca =
        !termo ||
        evento.titulo.toLowerCase().includes(termo) ||
        (evento.local ?? '').toLowerCase().includes(termo);
      const bateStatus = !filtroStatus || statusEvento(evento.data_hora_inicio, evento.data_hora_fim) === filtroStatus;
      return bateBusca && bateStatus;
    });
  }, [eventos, busca, filtroStatus]);

  const totalPaginas = Math.max(1, Math.ceil(eventosFiltrados.length / TAMANHO_PAGINA));
  const eventosPagina = eventosFiltrados.slice(pagina * TAMANHO_PAGINA, (pagina + 1) * TAMANHO_PAGINA);

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

  const semResultado = !carregando && !erro && eventosFiltrados.length === 0;
  const filtrosAtivos = Boolean(busca || filtroStatus);

  return (
    <PageContainer
      titulo="Eventos"
      descricao="Gerencie os eventos cadastrados"
      acao={<Button onClick={abrirNovo}>Novo evento</Button>}
    >
      <div className="mb-4 flex flex-col gap-3 sm:flex-row sm:items-center">
        <input
          type="search"
          value={busca}
          onChange={(e) => setBusca(e.target.value)}
          placeholder="Buscar por título ou local..."
          className="flex-1 rounded-lg border border-border bg-surface px-3.5 py-2.5 text-sm text-text placeholder:text-text-muted/60 outline-none focus:border-accent"
        />
        <select
          value={filtroStatus}
          onChange={(e) => setFiltroStatus(e.target.value as StatusEvento | '')}
          className="rounded-lg border border-border bg-surface px-3.5 py-2.5 text-sm text-text outline-none focus:border-accent sm:w-52"
        >
          <option value="">Todos os status</option>
          <option value="nao_iniciado">Não iniciado</option>
          <option value="em_andamento">Em andamento</option>
          <option value="finalizado">Finalizado</option>
        </select>
      </div>

      {carregando && <LoadingBlock mensagem="Carregando eventos..." />}
      {!carregando && erro && <ErrorMessage mensagem={erro} onTentarNovamente={recarregar} />}
      {semResultado && filtrosAtivos && (
        <EmptyState titulo="Nenhum evento encontrado" descricao="Tente ajustar a busca ou o filtro de status." />
      )}
      {semResultado && !filtrosAtivos && (
        <EmptyState titulo="Nenhum evento cadastrado" acao={<Button onClick={abrirNovo}>Criar o primeiro evento</Button>} />
      )}
      {!carregando && !erro && eventosPagina.length > 0 && (
        <>
          <div className="flex flex-col gap-3">
            {eventosPagina.map((evento) => (
              <div
                key={evento.id}
                className="flex flex-col gap-3 rounded-card border border-border bg-surface p-5 sm:flex-row sm:items-center sm:justify-between"
              >
                <div>
                  <div className="mb-1 flex flex-wrap items-center gap-2">
                    <h3 className="font-semibold text-text">{evento.titulo}</h3>
                    <EventoStatusBadge dataHoraInicio={evento.data_hora_inicio} dataHoraFim={evento.data_hora_fim} />
                  </div>
                  <p className="text-sm text-text-muted">
                    {formatarDataHora(evento.data_hora_inicio)} · {evento.pontos} pts
                    {evento.local ? ` · ${evento.local}` : ''}
                  </p>
                  {/* Ocupação. Sem limite não vira "X/∞": mostra só quantos
                      entraram, que é a informação que existe. */}
                  <p className="mt-1 text-sm">
                    <span aria-hidden>🎟️</span>{' '}
                    {evento.capacidade == null ? (
                      <span className="text-text-muted">
                        {evento.inscritos} inscrito{evento.inscritos === 1 ? '' : 's'} · sem limite de vagas
                      </span>
                    ) : (
                      <span className={evento.lotado ? 'font-medium text-amber-400' : 'text-text-muted'}>
                        {evento.inscritos} de {evento.capacidade} vagas
                        {evento.lotado ? ' · lotado' : ` · ${evento.vagas_restantes} restantes`}
                      </span>
                    )}
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

          <div className="mt-4 flex flex-col items-center justify-between gap-3 sm:flex-row">
            <p className="text-sm text-text-muted">
              {eventosFiltrados.length} evento{eventosFiltrados.length === 1 ? '' : 's'} - página {pagina + 1} de{' '}
              {totalPaginas}
            </p>
            <div className="flex gap-2">
              <Button variante="outline" disabled={pagina === 0} onClick={() => setPagina((p) => Math.max(0, p - 1))}>
                Anterior
              </Button>
              <Button
                variante="outline"
                disabled={pagina + 1 >= totalPaginas}
                onClick={() => setPagina((p) => p + 1)}
              >
                Próxima
              </Button>
            </div>
          </div>
        </>
      )}

      {modalAberto && (
        <EventoFormModal evento={eventoEditando} onSalvar={salvar} onFechar={() => setModalAberto(false)} />
      )}
    </PageContainer>
  );
}
