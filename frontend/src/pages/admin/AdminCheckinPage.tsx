import { useEffect, useState } from 'react';
import PageContainer from '../../components/PageContainer';
import Button from '../../components/Button';
import { LoadingBlock } from '../../components/LoadingSpinner';
import EmptyState from '../../components/EmptyState';
import { useEventos } from '../../hooks/useEventos';
import { criarSessaoCheckin, baixarQrCodeSessao, listarCheckinsDoEvento } from '../../services/checkins';
import { extrairMensagemErro } from '../../services/api';
import { useToast } from '../../context/ToastContext';
import { formatarDataHora } from '../../utils/data';
import type { EventoCheckin, SessaoCheckin, TipoSessaoCheckin } from '../../types';

/**
 * Admin/professor escolhe o evento e o tipo de sessão (entrada ou saída)
 * e gera um QR code pra projetar na tela. Cada aluno inscrito escaneia
 * com a câmera do próprio celular (Android ou iOS, sem app nenhum) - a
 * câmera abre a URL embutida no QR direto no navegador, que confirma a
 * presença dele autenticado como aluno. Ver CheckinSessaoController e
 * docs/openapi.yaml.
 */
export default function AdminCheckinPage() {
  const { eventos, carregando: carregandoEventos } = useEventos({});
  const { notificarErro } = useToast();

  const [eventoId, setEventoId] = useState('');
  const [tipo, setTipo] = useState<TipoSessaoCheckin>('ENTRADA');
  const [sessao, setSessao] = useState<SessaoCheckin | null>(null);
  const [qrCodeUrl, setQrCodeUrl] = useState<string | null>(null);
  const [gerando, setGerando] = useState(false);

  const [participantes, setParticipantes] = useState<EventoCheckin[]>([]);
  const [carregandoParticipantes, setCarregandoParticipantes] = useState(false);

  useEffect(() => {
    return () => {
      if (qrCodeUrl) URL.revokeObjectURL(qrCodeUrl);
    };
  }, [qrCodeUrl]);

  // Lista de presença - recarrega sempre que o evento selecionado muda
  // (e também depois de gerar um QR, via recarregarParticipantes abaixo,
  // já que confirmações podem já ter acontecido enquanto essa tela ficava
  // aberta com o QR anterior).
  useEffect(() => {
    if (!eventoId) {
      setParticipantes([]);
      return;
    }
    setCarregandoParticipantes(true);
    listarCheckinsDoEvento(eventoId)
      .then(setParticipantes)
      .catch(() => setParticipantes([]))
      .finally(() => setCarregandoParticipantes(false));
  }, [eventoId]);

  const recarregarParticipantes = () => {
    if (!eventoId) return;
    listarCheckinsDoEvento(eventoId)
      .then(setParticipantes)
      .catch(() => {});
  };

  /** Esconde o QR gerado anteriormente assim que o evento ou o tipo
   *  mudam - sem isso, o QR (e o link embutido nele) continuavam sendo
   *  os do evento/tipo antigo, só o texto acima mudava pra refletir a
   *  nova seleção, dando a falsa impressão de que era o QR certo. */
  const limparQrCode = () => {
    setSessao(null);
    if (qrCodeUrl) URL.revokeObjectURL(qrCodeUrl);
    setQrCodeUrl(null);
  };

  const selecionarEvento = (novoEventoId: string) => {
    setEventoId(novoEventoId);
    limparQrCode();
  };

  const selecionarTipo = (novoTipo: TipoSessaoCheckin) => {
    setTipo(novoTipo);
    limparQrCode();
  };

  const gerarQrCode = async () => {
    if (!eventoId) {
      notificarErro('Escolha um evento primeiro.');
      return;
    }
    setGerando(true);
    limparQrCode();
    try {
      const novaSessao = await criarSessaoCheckin(eventoId, tipo);
      const blob = await baixarQrCodeSessao(novaSessao.id);
      setSessao(novaSessao);
      setQrCodeUrl(URL.createObjectURL(blob));
    } catch (e) {
      notificarErro(extrairMensagemErro(e, 'Não foi possível gerar o QR code.'));
    } finally {
      setGerando(false);
    }
  };

  const eventoSelecionado = eventos.find((e) => e.id === eventoId);

  return (
    <PageContainer
      titulo="Check-in"
      descricao="Gere o QR code de entrada ou saída e projete na tela - cada aluno confirma a própria presença escaneando com a câmera do celular"
    >
      <div className="flex flex-col gap-4 rounded-card border border-border bg-surface p-6">
        {carregandoEventos ? (
          <LoadingBlock mensagem="Carregando eventos..." />
        ) : (
          <div className="flex flex-col gap-3 sm:flex-row sm:items-end">
            <label className="flex flex-1 flex-col gap-1.5 text-sm">
              <span className="font-medium text-text">Evento</span>
              <select
                className="rounded-lg border border-border bg-surface px-3.5 py-2.5 text-text outline-none focus:border-accent"
                value={eventoId}
                onChange={(e) => selecionarEvento(e.target.value)}
              >
                <option value="">Selecione um evento</option>
                {eventos.map((evento) => (
                  <option key={evento.id} value={evento.id}>
                    {evento.titulo}
                  </option>
                ))}
              </select>
            </label>

            <label className="flex flex-col gap-1.5 text-sm">
              <span className="font-medium text-text">Tipo</span>
              <select
                className="rounded-lg border border-border bg-surface px-3.5 py-2.5 text-text outline-none focus:border-accent"
                value={tipo}
                onChange={(e) => selecionarTipo(e.target.value as TipoSessaoCheckin)}
              >
                <option value="ENTRADA">Entrada</option>
                <option value="SAIDA">Saída</option>
              </select>
            </label>

            <Button carregando={gerando} onClick={gerarQrCode} className="sm:mb-0">
              Gerar QR code
            </Button>
          </div>
        )}

        {sessao && qrCodeUrl && (
          <div className="flex flex-col items-center gap-3 rounded-card border border-accent/30 bg-accent/5 p-8">
            <p className="text-sm text-text-muted">
              QR de <span className="font-semibold text-text">{tipo === 'ENTRADA' ? 'entrada' : 'saída'}</span> para
            </p>
            <p className="text-lg font-bold text-text">{eventoSelecionado?.titulo}</p>
            <img src={qrCodeUrl} alt="QR code de check-in" className="h-72 w-72 rounded-lg bg-white p-3" />
            <p className="text-xs text-text-muted">
              Válido até {new Date(sessao.expira_em).toLocaleString('pt-BR')} - peça pro aluno escanear com a
              câmera do próprio celular
            </p>
          </div>
        )}
      </div>

      {eventoId && (
        <div className="mt-6 flex flex-col gap-4 rounded-card border border-border bg-surface p-6">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <h2 className="text-lg font-bold text-text">Participantes do Evento</h2>
              <p className="text-sm text-text-muted">
                {carregandoParticipantes ? 'Carregando...' : `${participantes.length} participante${participantes.length === 1 ? '' : 's'} presente${participantes.length === 1 ? '' : 's'}.`}
              </p>
            </div>
            <Button variante="outline" onClick={recarregarParticipantes} disabled={carregandoParticipantes}>
              Atualizar
            </Button>
          </div>

          {carregandoParticipantes && <LoadingBlock mensagem="Carregando participantes..." />}

          {!carregandoParticipantes && participantes.length === 0 && (
            <EmptyState titulo="Nenhum check-in ainda" descricao="Assim que um aluno confirmar a entrada, ele aparece aqui." />
          )}

          {!carregandoParticipantes && participantes.length > 0 && (
            <div className="overflow-x-auto rounded-card border border-border">
              <table className="w-full min-w-[560px] text-left text-sm">
                <thead>
                  <tr className="border-b border-border text-text-muted">
                    <th className="px-5 py-3 font-medium">Aluno</th>
                    <th className="px-5 py-3 font-medium">RGM</th>
                    <th className="px-5 py-3 font-medium">Entrada</th>
                    <th className="px-5 py-3 font-medium">Saída</th>
                  </tr>
                </thead>
                <tbody>
                  {participantes.map((p) => (
                    <tr key={p.inscricao_id} className="border-b border-border last:border-0">
                      <td className="px-5 py-3 text-text">{p.aluno_nome}</td>
                      <td className="px-5 py-3 text-text-muted">{p.aluno_rgm ?? '-'}</td>
                      <td className="px-5 py-3 text-text-muted">{formatarDataHora(p.entrada)}</td>
                      <td className="px-5 py-3 text-text-muted">{p.saida ? formatarDataHora(p.saida) : '-'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}
    </PageContainer>
  );
}
