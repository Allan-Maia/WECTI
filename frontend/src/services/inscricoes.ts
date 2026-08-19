import api from './api';
import type { Inscricao } from '../types';

export function inscreverEmEvento(eventoId: string) {
  return api.post<Inscricao>(`/eventos/${eventoId}/inscricoes`).then((res) => res.data);
}

export function listarInscritosDoEvento(eventoId: string) {
  return api.get<Inscricao[]>(`/eventos/${eventoId}/inscricoes`).then((res) => res.data);
}

export function listarMinhasInscricoes(status?: 'futuros' | 'historico') {
  return api.get<Inscricao[]>('/me/inscricoes', { params: status ? { status } : {} }).then((res) => res.data);
}

export function buscarInscricao(id: string) {
  return api.get<Inscricao>(`/inscricoes/${id}`).then((res) => res.data);
}

export function cancelarInscricao(id: string) {
  return api.delete<void>(`/inscricoes/${id}`).then((res) => res.data);
}

export function urlCertificado(inscricaoId: string) {
  return `${api.defaults.baseURL}/inscricoes/${inscricaoId}/certificado`;
}

/** Baixa o PDF do certificado autenticado (o navegador nao manda o header
 *  Authorization em <a href>, entao o download precisa passar pelo axios). */
export function baixarCertificado(inscricaoId: string) {
  return api
    .get(`/inscricoes/${inscricaoId}/certificado`, { responseType: 'blob' })
    .then((res) => res.data as Blob);
}
