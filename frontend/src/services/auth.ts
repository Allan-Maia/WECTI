import api from './api';
import type { CadastroAlunoRequest, LoginRequest, LoginResponse, RedefinirSenhaRequest } from '../types';

export function login(dados: LoginRequest) {
  return api.post<LoginResponse>('/auth/login', dados).then((res) => res.data);
}

/** Cadastro publico (sempre ALUNO - ver CadastroAlunoRequest) - devolve
 *  token igual ao login, pra entrar direto no sistema. */
export function registrar(dados: CadastroAlunoRequest) {
  return api.post<LoginResponse>('/auth/registrar', dados).then((res) => res.data);
}

/** "Esqueci minha senha" - identidade confirmada com email + RGM/CPF, sem
 *  link por email. Devolve token igual ao login. */
export function redefinirSenha(dados: RedefinirSenhaRequest) {
  return api.post<LoginResponse>('/auth/redefinir-senha', dados).then((res) => res.data);
}
