import api from './api';
import type { PontuacaoExtra, PontuacaoPeriodo, Ranking } from '../types';

export function buscarMinhaPontuacao(periodoId?: string) {
  return api
    .get<PontuacaoPeriodo>('/me/pontuacao', { params: periodoId ? { periodo_id: periodoId } : {} })
    .then((res) => res.data);
}

export function buscarPontuacaoDoAluno(alunoId: string, periodoId: string) {
  return api.get<PontuacaoPeriodo>(`/pontuacao/aluno/${alunoId}/periodo/${periodoId}`).then((res) => res.data);
}

/** Classificação do período. Uma rota só para os dois perfis - o backend
 *  decide o que devolver pelo perfil do token (o RGM dos colegas só vai
 *  para o admin). */
export function buscarRanking(periodoId?: string) {
  return api
    .get<Ranking>('/ranking', { params: periodoId ? { periodo_id: periodoId } : {} })
    .then((res) => res.data);
}

/** Lança pontos de gincana num aluno (só admin). */
export function lancarPontosExtras(dados: {
  aluno_id: string;
  pontos: number;
  motivo: string;
  periodo_id?: string;
}) {
  return api.post<PontuacaoExtra>('/pontuacao-extra', dados).then((res) => res.data);
}

/** Histórico de lançamentos de um aluno - o admin confere antes de
 *  premiar de novo, para não contar a mesma gincana duas vezes. */
export function listarPontosExtras(alunoId: string, periodoId?: string) {
  return api
    .get<PontuacaoExtra[]>('/pontuacao-extra', {
      params: { aluno_id: alunoId, ...(periodoId ? { periodo_id: periodoId } : {}) },
    })
    .then((res) => res.data);
}

/** Desfaz um lançamento (lançou no aluno errado, valor errado...). */
export function removerPontosExtras(id: string) {
  return api.delete(`/pontuacao-extra/${id}`).then(() => undefined);
}
