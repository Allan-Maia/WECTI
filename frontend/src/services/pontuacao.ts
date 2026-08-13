import api from './api';
import type { PontuacaoPeriodo } from '../types';

export function buscarMinhaPontuacao(periodoId?: string) {
  return api
    .get<PontuacaoPeriodo>('/me/pontuacao', { params: periodoId ? { periodo_id: periodoId } : {} })
    .then((res) => res.data);
}

export function buscarPontuacaoDoAluno(alunoId: string, periodoId: string) {
  return api.get<PontuacaoPeriodo>(`/pontuacao/aluno/${alunoId}/periodo/${periodoId}`).then((res) => res.data);
}
