// Tipos espelhando exatamente os DTOs JSON do backend (docs/openapi.yaml).
// O backend serializa em snake_case (spring.jackson.property-naming-strategy:
// SNAKE_CASE), por isso os campos aqui usam snake_case, sem camada de
// mapeamento intermediaria.

/**
 * So ADMIN e ALUNO - confirmado com o stakeholder do projeto que esta
 * versao nao precisa de um perfil PROFESSOR separado. O contrato
 * (docs/openapi.yaml) documenta o enum Perfil em minusculas, mas o
 * backend serializa o enum Java como veio (maiusculas) - e isso que a
 * API realmente devolve e espera em NovoUsuario.perfil.
 */
export type Perfil = 'ADMIN' | 'ALUNO';

export interface Usuario {
  id: string;
  nome: string;
  email: string;
  perfil: Perfil;
  rgm: string | null;
  cpf: string | null;
  // Curso do aluno (ex.: "Ciencia da Computacao") - so informativo,
  // confirmado com o professor que nao afeta pontuacao/elegibilidade.
  curso: string | null;
}

export interface NovoUsuario {
  nome: string;
  email: string;
  perfil: Perfil;
  rgm?: string | null;
  cpf?: string | null;
  curso?: string | null;
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
  // Sem periodo_id aqui de proposito: o backend descobre sozinho o
  // Periodo (semestre) a partir da data do evento - ver EventoService.
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

/** Linha do relatório de presença (GET /eventos/{id}/checkins) - "Participantes do Evento". */
export interface EventoCheckin {
  inscricao_id: string;
  aluno_id: string;
  aluno_nome: string;
  aluno_rgm: string | null;
  entrada: string;
  saida: string | null;
  percentual_presenca: number | null;
}

// Igual ao Perfil: o enum Java (TipoSessaoCheckin.ENTRADA/.SAIDA) e
// serializado como veio, em maiusculas - a naming strategy SNAKE_CASE
// so afeta nomes de campo, nao valores de enum.
export type TipoSessaoCheckin = 'ENTRADA' | 'SAIDA';

/** Sessão de QR code de check-in/check-out gerada pelo admin/professor
 *  pra um evento - projetada na tela, o aluno confirma a própria presença
 *  escaneando com a câmera do celular (ver CheckinSessaoController). */
export interface SessaoCheckin {
  id: string;
  evento_id: string;
  tipo: TipoSessaoCheckin;
  criada_em: string;
  expira_em: string;
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

// Cadastro publico ("Primeiro acesso? Crie sua conta") - sem campo
// perfil de proposito: sempre vira ALUNO no backend, nunca escolhido
// pelo cliente (ver CadastroAlunoRequest no backend).
export interface CadastroAlunoRequest {
  nome: string;
  email: string;
  senha: string;
  rgm: string;
  curso?: string;
}

// "Esqueci minha senha" - identidade confirmada com RGM (aluno) ou CPF
// (professor), sem link por email (ver RedefinirSenhaRequest no backend).
export interface RedefinirSenhaRequest {
  email: string;
  identificador: string;
  nova_senha: string;
}

// Envelope de paginacao (ver PaginaResponse no backend) - usado nas
// listagens que crescem muito, ex.: GET /usuarios.
export interface Pagina<T> {
  conteudo: T[];
  pagina: number;
  tamanho: number;
  total_elementos: number;
  total_paginas: number;
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
