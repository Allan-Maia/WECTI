import type { ReactNode } from 'react';
import type { RankingItem } from '../types';

interface Props {
  itens: RankingItem[];
  /** Destaca a linha do próprio aluno - numa lista de centenas, é o que
   *  ele procura primeiro. */
  destacarAlunoId?: string;
  /** Coluna extra à direita (na tela do admin, o botão de lançar pontos). */
  acao?: (item: RankingItem) => ReactNode;
  /** RGM só aparece para o admin, e mesmo assim só quando a API mandou. */
  mostrarRgm?: boolean;
}

/**
 * Medalha nas três primeiras posições. Empate divide a mesma medalha,
 * porque divide a mesma posição.
 *
 * Só entra com pontuação positiva: no começo do WECTI, quando quase
 * ninguém pontuou, o terceiro lugar pode estar negativo por no-show — e
 * premiar isso com um bronze faz a tela mentir sobre o que aconteceu.
 */
function Medalha({ posicao, pontos }: { posicao: number; pontos: number }) {
  const medalhas: Record<number, string> = { 1: '🥇', 2: '🥈', 3: '🥉' };
  const medalha = pontos > 0 ? medalhas[posicao] : undefined;
  return (
    <span className="inline-flex items-center gap-1.5 tabular-nums">
      {medalha && <span aria-hidden>{medalha}</span>}
      <span className={medalha ? 'font-bold text-text' : 'text-text-muted'}>{posicao}º</span>
    </span>
  );
}

export default function RankingTabela({ itens, destacarAlunoId, acao, mostrarRgm = false }: Props) {
  return (
    <div className="overflow-x-auto rounded-card border border-border bg-surface">
      <table className="w-full min-w-[640px] text-left text-sm">
        <thead>
          <tr className="border-b border-border text-text-muted">
            <th className="px-5 py-3 font-medium">#</th>
            <th className="px-5 py-3 font-medium">Aluno</th>
            {mostrarRgm && <th className="px-5 py-3 font-medium">RGM</th>}
            <th className="px-5 py-3 font-medium">Curso</th>
            <th className="px-5 py-3 text-right font-medium">Palestras</th>
            <th className="px-5 py-3 text-right font-medium">Gincanas</th>
            <th className="px-5 py-3 text-right font-medium">Total</th>
            {acao && <th className="px-5 py-3 font-medium" />}
          </tr>
        </thead>
        <tbody>
          {itens.map((item) => {
            const euMesmo = item.aluno_id === destacarAlunoId;
            return (
              <tr
                key={item.aluno_id}
                className={`border-b border-border last:border-0 ${euMesmo ? 'bg-accent/5' : ''}`}
              >
                <td className="px-5 py-3">
                  <Medalha posicao={item.posicao} pontos={item.pontos_total} />
                </td>
                <td className="px-5 py-3 text-text">
                  {item.aluno_nome}
                  {euMesmo && (
                    <span className="ml-2 rounded-full bg-accent/15 px-2 py-0.5 text-xs font-medium text-accent">
                      você
                    </span>
                  )}
                </td>
                {mostrarRgm && <td className="px-5 py-3 tabular-nums text-text-muted">{item.aluno_rgm ?? '-'}</td>}
                <td className="px-5 py-3 text-text-muted">{item.aluno_curso ?? '-'}</td>
                <td className="px-5 py-3 text-right tabular-nums text-text-muted">
                  {item.pontos_eventos}
                  <span className="ml-1 text-xs text-text-muted/70">
                    ({item.eventos_concluidos})
                  </span>
                </td>
                <td className="px-5 py-3 text-right tabular-nums text-text-muted">
                  {item.pontos_extras !== 0 ? item.pontos_extras : '-'}
                </td>
                <td className="px-5 py-3 text-right tabular-nums font-bold text-text">{item.pontos_total}</td>
                {acao && <td className="px-5 py-3 text-right">{acao(item)}</td>}
              </tr>
            );
          })}
        </tbody>
      </table>
      <p className="border-t border-border px-5 py-3 text-xs text-text-muted">
        Em "Palestras", o número entre parênteses é quantas o aluno concluiu com presença suficiente. Alunos
        empatados dividem a mesma posição.
      </p>
    </div>
  );
}
