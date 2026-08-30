import { useEffect, useMemo, useState } from 'react';
import PageContainer from '../../components/PageContainer';
import Button from '../../components/Button';
import { LoadingBlock } from '../../components/LoadingSpinner';
import ErrorMessage from '../../components/ErrorMessage';
import EmptyState from '../../components/EmptyState';
import RankingTabela from '../../components/RankingTabela';
import LancarPontosModal from './LancarPontosModal';
import { buscarRanking } from '../../services/pontuacao';
import { extrairMensagemErro } from '../../services/api';
import type { Ranking, RankingItem } from '../../types';

/**
 * Ranking na visão do admin: mesma classificação que o aluno vê, mais o
 * RGM (para não lançar pontos no aluno errado — homônimos existem) e a
 * ação de lançar pontos de gincana.
 */
export default function AdminRankingPage() {
  const [ranking, setRanking] = useState<Ranking | null>(null);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState<string | null>(null);
  const [busca, setBusca] = useState('');
  const [alunoSelecionado, setAlunoSelecionado] = useState<RankingItem | null>(null);

  const carregar = () => {
    setCarregando(true);
    buscarRanking()
      .then((dados) => {
        setRanking(dados);
        setErro(null);
      })
      .catch((e) => {
        setRanking(null);
        setErro(extrairMensagemErro(e, 'Não foi possível carregar o ranking.'));
      })
      .finally(() => setCarregando(false));
  };

  useEffect(carregar, []);

  // Filtra em memória: a lista inteira já veio, e com centenas de alunos
  // achar um nome específico rolando a tela é inviável.
  const itensFiltrados = useMemo(() => {
    if (!ranking) return [];
    const termo = busca.trim().toLowerCase();
    if (!termo) return ranking.itens;
    return ranking.itens.filter(
      (i) =>
        i.aluno_nome.toLowerCase().includes(termo) ||
        (i.aluno_rgm ?? '').includes(termo) ||
        (i.aluno_curso ?? '').toLowerCase().includes(termo),
    );
  }, [ranking, busca]);

  return (
    <PageContainer
      titulo="Ranking"
      descricao={
        ranking
          ? `Classificação do período ${ranking.periodo_nome} - lance aqui os pontos de gincana`
          : 'Classificação por pontos'
      }
    >
      {carregando && <LoadingBlock mensagem="Carregando ranking..." />}
      {!carregando && erro && <ErrorMessage mensagem={erro} onTentarNovamente={carregar} />}

      {!carregando && !erro && ranking && ranking.itens.length === 0 && (
        <EmptyState
          titulo="O ranking ainda está vazio"
          descricao="Assim que houver inscrições e palestras concluídas, a classificação aparece aqui."
          icone="🏆"
        />
      )}

      {!carregando && !erro && ranking && ranking.itens.length > 0 && (
        <div className="flex flex-col gap-4">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <input
              type="search"
              value={busca}
              onChange={(e) => setBusca(e.target.value)}
              placeholder="Buscar por nome, RGM ou curso"
              className="w-full max-w-sm rounded-lg border border-border bg-surface px-3.5 py-2.5 text-sm text-text placeholder:text-text-muted/60 outline-none focus:border-accent"
            />
            <span className="text-sm text-text-muted">
              {itensFiltrados.length} de {ranking.itens.length} aluno(s)
            </span>
          </div>

          {itensFiltrados.length === 0 ? (
            <EmptyState titulo="Nenhum aluno encontrado" descricao="Tente outro nome, RGM ou curso." icone="🔍" />
          ) : (
            <RankingTabela
              itens={itensFiltrados}
              mostrarRgm
              acao={(item) => (
                <Button
                  variante="outline"
                  onClick={() => setAlunoSelecionado(item)}
                  className="px-3 py-1.5 text-xs"
                >
                  Lançar pontos
                </Button>
              )}
            />
          )}
        </div>
      )}

      {alunoSelecionado && (
        <LancarPontosModal
          aluno={alunoSelecionado}
          onFechar={() => setAlunoSelecionado(null)}
          onAlterado={carregar}
        />
      )}
    </PageContainer>
  );
}
