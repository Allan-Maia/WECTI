import api from './api';
import type { NovoPalestrante, Palestrante } from '../types';

export function listarPalestrantes() {
  return api.get<Palestrante[]>('/palestrantes').then((res) => res.data);
}

export function criarPalestrante(dados: NovoPalestrante) {
  return api.post<Palestrante>('/palestrantes', dados).then((res) => res.data);
}
