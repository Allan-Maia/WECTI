// Tipos espelhando exatamente os DTOs JSON do backend (docs/openapi.yaml).
// O backend serializa em snake_case (spring.jackson.property-naming-strategy:
// SNAKE_CASE), por isso os campos aqui usam snake_case, sem camada de
// mapeamento intermediaria.

/**
 * O contrato (docs/openapi.yaml) documenta o enum Perfil em minusculas
 * (admin/professor/aluno), mas o backend serializa o enum Java como veio
 * (maiusculas: ADMIN/PROFESSOR/ALUNO) - e isso que a API realmente
 * devolve e espera em NovoUsuario.perfil. Usamos maiusculas aqui para
 * bater com o comportamento real.
 */
export type Perfil = 'ADMIN' | 'PROFESSOR' | 'ALUNO';

export interface Usuario {
  id: string;
  nome: string;
  email: string;
  perfil: Perfil;
  rgm: string | null;
}

export interface NovoUsuario {
  nome: string;
  email: string;
  perfil: Perfil;
  rgm?: string | null;
}

export interface Periodo {
  id: string;
  nome: string;
  data_inicio: string;
  data_fim: string;
}

export interface NovoPeriodo {
  nome: string;
  data_inicio: string;
  data_fim: string;
}

export interface Palestrante {
  id: string;
  nome: string;
  bio: string | null;
}

export interface NovoPalestrante {
  nome: string;
  bio?: string;
}

export interface Evento {
  id: string;
  titulo: string;
  descricao: string | null;
  periodo_id: string;
  local: string | null;
  data_hora_inicio: string;
  data_hora_fim: string;
  pontos: number;
  palestrantes: Palestrante[];
}

export interface NovoEvento {
  titulo: string;
  descricao?: string;
  periodo_id: string;
  local?: string;
  data_hora_inicio: string;
  data_hora_fim: string;
  pontos: number;
  palestrante_ids?: string[];
}

export type InscricaoStatus = 'ativa' | 'cancelada';

export interface Checkin {
  id: string;
  inscricao_id: string;
  entrada: string;
  saida: string | null;
  percentual_presenca: number | null;
}

export interface Inscricao {
  id: string;
  aluno_id: string;
  evento_id: string;
  status: InscricaoStatus;
  criada_em: string;
  cancelada_em: string | null;
  checkin: Checkin | null;
  certificado_disponivel: boolean;
}

export type EventoPontuacaoStatus = 'concluido' | 'no_show' | 'cancelado';

export interface EventoPontuacaoItem {
  evento_id: string;
  titulo: string;
  pontos: number;
  status: EventoPontuacaoStatus;
}

export interface PontuacaoPeriodo {
  periodo_id: string;
  periodo_nome: string;
  pontos_total: number;
  eventos: EventoPontuacaoItem[];
}

export interface LoginRequest {
  email: string;
  senha: string;
}

export interface LoginResponse {
  token: string;
  usuario: Usuario;
}

export interface ErroResponse {
  timestamp: string;
  status: number;
  erro: string;
  mensagem: string;
  campo?: string | null;
}

/** Claims decodificadas do JWT (ver JwtService no backend). */
export interface JwtPayload {
  sub: string;
  perfil: Perfil;
  iat: number;
  exp: number;
}
