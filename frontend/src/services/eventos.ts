import api from './api';
import type { Evento, NovoEvento } from '../types';

export interface FiltroEventos {
  /**
   * `futuros` — ainda aceitam inscrição (não começaram).
   * `em_cartaz` — ainda não terminaram, incluindo o que está acontecendo
   *   agora. É o que a tela do aluno usa: sem isso a palestra sumia da
   *   lista no instante em que começava.
   * `encerrados` — já terminaram.
   */
  status?: 'futuros' | 'em_cartaz' | 'encerrados';
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
