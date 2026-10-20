import api from './api';
import type { CertificadoValidacao } from '../types';

/**
 * Validacao publica de certificado - endpoint aberto, sem login (quem
 * confere costuma ser recrutador/outra instituicao, sem conta aqui).
 *
 * O interceptor do axios que desloga em 401 nao atrapalha: esse endpoint
 * responde 404 quando o codigo nao existe, nunca 401.
 */
export function validarCertificado(codigo: string) {
  return api.get<CertificadoValidacao>(`/validar/${encodeURIComponent(codigo)}`).then((res) => res.data);
}
