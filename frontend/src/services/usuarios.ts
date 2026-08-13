import api from './api';
import type { NovoUsuario, Perfil, Usuario } from '../types';

export function listarUsuarios(perfil?: Perfil) {
  return api.get<Usuario[]>('/usuarios', { params: perfil ? { perfil } : {} }).then((res) => res.data);
}

export function criarUsuario(dados: NovoUsuario) {
  return api.post<Usuario>('/usuarios', dados).then((res) => res.data);
}

export function buscarMeuUsuario() {
  return api.get<Usuario>('/usuarios/me').then((res) => res.data);
}
