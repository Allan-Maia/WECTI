import api from './api';
import type { Evento, NovoEvento } from '../types';

export interface FiltroEventos {
  status?: 'futuros' | 'encerrados';
}

export function listarEventos(filtro: FiltroEventos = {}) {
  return api.get<Evento[]>('/eventos', { params: filtro }).then((res) => res.data);
}

export function buscarEvento(id: string) {
  return api.get<Evento>(`/eventos/${id}`).then((res) => res.data);
}

export function criarEvento(dados: NovoEvento) {
  return api.post<Evento>('/eventos', dados).then((res) => res.data);
}

export function atualizarEvento(id: string, dados: NovoEvento) {
  return api.put<Evento>(`/eventos/${id}`, dados).then((res) => res.data);
}

export function cancelarEvento(id: string) {
  return api.delete<void>(`/eventos/${id}`).then((res) => res.data);
}
