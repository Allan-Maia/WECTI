import api from './api';
import type { Checkin } from '../types';

export function registrarEntrada(qrcodeToken: string) {
  return api.post<Checkin>('/checkins', { qrcode_token: qrcodeToken }).then((res) => res.data);
}

export function registrarSaida(checkinId: string) {
  return api.post<Checkin>(`/checkins/${checkinId}/checkout`).then((res) => res.data);
}
