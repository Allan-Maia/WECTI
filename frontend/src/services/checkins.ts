import api from './api';
import type { Checkin, SessaoCheckin, TipoSessaoCheckin } from '../types';

/** Admin/professor gera uma sessão de check-in (entrada) ou check-out
 *  (saída) pro evento - o QR resultante fica válido por 6 horas. */
export function criarSessaoCheckin(eventoId: string, tipo: TipoSessaoCheckin) {
  return api.post<SessaoCheckin>(`/eventos/${eventoId}/checkin-sessoes`, { tipo }).then((res) => res.data);
}

/** Baixa o PNG do QR code autenticado - um <img src> direto nao manda o
 *  header Authorization, entao precisa passar pelo axios e virar blob. */
export function baixarQrCodeSessao(sessaoId: string) {
  return api
    .get(`/checkin-sessoes/${sessaoId}/qrcode`, { responseType: 'blob' })
    .then((res) => res.data as Blob);
}

/** Chamado pelo aluno ao escanear o QR com a câmera do celular. */
export function confirmarCheckinSessao(sessaoId: string) {
  return api.post<Checkin>(`/checkin-sessoes/${sessaoId}/confirmar`).then((res) => res.data);
}
