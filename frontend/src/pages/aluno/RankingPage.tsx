import { useEffect, useState } from 'react';
import PageContainer from '../../components/PageContainer';
import { LoadingBlock } from '../../components/LoadingSpinner';
import ErrorMessage from '../../components/ErrorMessage';
import EmptyState from '../../components/EmptyState';
import RankingTabela from '../../components/RankingTabela';
import { buscarRanking } from '../../services/pontuacao';
import { extrairMensagemErro } from '../../services/api';
import { useAuth } from '../../context/AuthContext';
import type { Ranking } from '../../types';

/**
 * Classificação do período, como o aluno vê.
 *
 * Mostra nome e curso dos colegas, e mais nada — o RGM só vai para a
 * tela do admin (quem decide isso é o backend, pelo perfil do token).
 * A própria linha do aluno vem destacada: numa lista de centenas, achar
 * a si mesmo é a primeira coisa que ele quer fazer.
 */
export default function RankingPage() {
  const { usuario } = useAuth();
  const [ranking, setRanking] = useState<Ranking | null>(null);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState<string | null>(null);

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

  const minhaPosicao = ranking?.itens.find((i) => i.aluno_id === usuario?.id);

  return (
    <PageContainer
      titulo="Ranking"
      descricao="Classificação dos alunos por pontos no WECTI"
    >
      {carregando && <LoadingBlock mensagem="Carregando ranking..." />}
      {!carregando && erro && <ErrorMessage mensagem={erro} onTentarNovamente={carregar} />}

      {!carregando && !erro && ranking && ranking.itens.length === 0 && (
        <EmptyState
          titulo="O ranking ainda está vazio"
          descricao="Assim que as primeiras palestras forem concluídas, a classificação aparece aqui."
          icone="🏆"
        />
      )}

      {!carregando && !erro && ranking && ranking.itens.length > 0 && (
        <div className="flex flex-col gap-5">
          {minhaPosicao ? (
            <div className="flex flex-wrap items-center justify-between gap-4 rounded-card border border-accent/30 bg-accent/5 p-5">
              <div>
                <p className="text-sm text-text-muted">Sua posição</p>
                <p className="text-3xl font-bold text-text">
                  {minhaPosicao.posicao}
                  <span className="ml-1 text-base font-medium text-text-muted">
                    de {ranking.itens.length}
                  </span>
                </p>
              </div>
              <div className="text-right">
                <p className="text-sm text-text-muted">Seus pontos</p>
                <p className="text-3xl font-bold text-accent">{minhaPosicao.pontos_total}</p>
                {minhaPosicao.pontos_extras !== 0 && (
                  <p className="text-xs text-text-muted">
                    {minhaPosicao.pontos_eventos} de palestras + {minhaPosicao.pontos_extras} de gincanas
                  </p>
                )}
              </div>
            </div>
          ) : (
            <p className="rounded-card border border-border bg-surface p-5 text-sm text-text-muted">
              Você ainda não pontuou neste período. Inscreva-se numa palestra e confirme sua presença para
              entrar na disputa.
            </p>
          )}

          <RankingTabela itens={ranking.itens} destacarAlunoId={usuario?.id} />
        </div>
      )}
    </PageContainer>
  );
}
