import api from './api';
import type { NovoPeriodo, Periodo } from '../types';

export function listarPeriodos() {
  return api.get<Periodo[]>('/periodos').then((res) => res.data);
}

export function criarPeriodo(dados: NovoPeriodo) {
  return api.post<Periodo>('/periodos', dados).then((res) => res.data);
}
