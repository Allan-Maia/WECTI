import { useEffect, useState } from 'react';
import Button from '../../components/Button';
import FormField from '../../components/FormField';
import { formatarDataHora } from '../../utils/data';
import { listarPontosExtras, lancarPontosExtras, removerPontosExtras } from '../../services/pontuacao';
import { extrairMensagemErro } from '../../services/api';
import type { PontuacaoExtra, RankingItem } from '../../types';

interface Props {
  aluno: RankingItem;
  onFechar: () => void;
  /** Chamado depois de lançar ou remover, para a tela recarregar o ranking. */
  onAlterado: () => void;
}

/**
 * Lançamento manual de pontos num aluno — prêmio de gincana.
 *
 * O histórico aparece junto de propósito: sem ele, o admin não tem como
 * saber se já premiou aquela gincana e acaba lançando duas vezes. É
 * também de onde se desfaz um lançamento errado.
 */
export default function LancarPontosModal({ aluno, onFechar, onAlterado }: Props) {
  const [pontos, setPontos] = useState('');
  const [motivo, setMotivo] = useState('');
  const [salvando, setSalvando] = useState(false);
  const [erro, setErro] = useState<string | null>(null);

  const [historico, setHistorico] = useState<PontuacaoExtra[]>([]);
  const [carregandoHistorico, setCarregandoHistorico] = useState(true);

  const carregarHistorico = () => {
    setCarregandoHistorico(true);
    listarPontosExtras(aluno.aluno_id)
      .then(setHistorico)
      .catch(() => setHistorico([]))
      .finally(() => setCarregandoHistorico(false));
  };

  useEffect(carregarHistorico, [aluno.aluno_id]);

  const lancar = async (e: React.FormEvent) => {
    e.preventDefault();
    setErro(null);

    const valor = Number(pontos);
    if (!Number.isInteger(valor) || valor === 0) {
      setErro('Informe um número inteiro de pontos, diferente de zero.');
      return;
    }
    if (motivo.trim().length === 0) {
      setErro('Explique o motivo — é o que permite conferir o ranking depois.');
      return;
    }

    setSalvando(true);
    try {
      await lancarPontosExtras({ aluno_id: aluno.aluno_id, pontos: valor, motivo: motivo.trim() });
      setPontos('');
      setMotivo('');
      carregarHistorico();
      onAlterado();
    } catch (e2) {
      setErro(extrairMensagemErro(e2, 'Não foi possível lançar os pontos.'));
    } finally {
      setSalvando(false);
    }
  };

  const remover = async (id: string) => {
    try {
      await removerPontosExtras(id);
      carregarHistorico();
      onAlterado();
    } catch (e) {
      setErro(extrairMensagemErro(e, 'Não foi possível remover o lançamento.'));
    }
  };

  const totalExtras = historico.reduce((soma, h) => soma + h.pontos, 0);

  return (
    <div className="fixed inset-0 z-[200] flex items-center justify-center bg-black/80 p-4" onClick={onFechar}>
      <div
        onClick={(e) => e.stopPropagation()}
        className="flex max-h-[90vh] w-full max-w-lg flex-col gap-4 overflow-y-auto rounded-card border border-border bg-surface p-6"
      >
        <div>
          <h2 className="text-lg font-bold text-text">Lançar pontos</h2>
          <p className="text-sm text-text-muted">
            {aluno.aluno_nome}
            {aluno.aluno_rgm && <span className="ml-2 tabular-nums">RGM {aluno.aluno_rgm}</span>}
          </p>
        </div>

        <form onSubmit={lancar} className="flex flex-col gap-4 rounded-card border border-border p-4">
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-[120px_1fr]">
            <FormField
              label="Pontos"
              type="number"
              value={pontos}
              onChange={(e) => setPontos(e.target.value)}
              placeholder="Ex.: 50"
            />
            <FormField
              label="Motivo"
              value={motivo}
              onChange={(e) => setMotivo(e.target.value)}
              placeholder="Ex.: 1º lugar na gincana de lógica"
              maxLength={200}
            />
          </div>

          <p className="text-xs text-text-muted">
            Use um valor negativo para corrigir um lançamento a maior. O motivo fica visível para o aluno na
            tela de pontuação dele.
          </p>

          {erro && (
            <p className="rounded-lg border border-red-500/30 bg-red-500/5 px-3 py-2 text-sm text-red-400">
              {erro}
            </p>
          )}

          <Button type="submit" carregando={salvando}>
            Lançar pontos
          </Button>
        </form>

        <div className="flex flex-col gap-2">
          <div className="flex items-center justify-between">
            <h3 className="text-sm font-semibold text-text">Lançamentos deste período</h3>
            {historico.length > 0 && (
              <span className="text-sm font-bold text-accent">{totalExtras} pts</span>
            )}
          </div>

          {carregandoHistorico && <p className="text-sm text-text-muted">Carregando...</p>}

          {!carregandoHistorico && historico.length === 0 && (
            <p className="text-sm text-text-muted">Nenhum ponto extra lançado ainda para este aluno.</p>
          )}

          {historico.map((item) => (
            <div
              key={item.id}
              className="flex items-start justify-between gap-3 rounded-lg border border-border px-4 py-3"
            >
              <div className="min-w-0">
                <p className="text-sm text-text">
                  <span className={`font-bold ${item.pontos < 0 ? 'text-red-400' : 'text-accent'}`}>
                    {item.pontos > 0 ? `+${item.pontos}` : item.pontos}
                  </span>{' '}
                  {item.motivo}
                </p>
                <p className="text-xs text-text-muted">
                  {formatarDataHora(item.criado_em)} · por {item.criado_por_nome}
                </p>
              </div>
              <Button variante="danger" onClick={() => remover(item.id)} className="shrink-0 px-3 py-1.5 text-xs">
                Remover
              </Button>
            </div>
          ))}
        </div>

        <div className="flex justify-end">
          <Button variante="outline" onClick={onFechar}>
            Fechar
          </Button>
        </div>
      </div>
    </div>
  );
}
