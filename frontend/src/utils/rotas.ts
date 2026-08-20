import type { Perfil } from '../types';

/**
 * Rota "home" de cada perfil - usada depois do login e como destino de
 * redirecionamento quando alguém tenta acessar uma rota que não pode.
 * `perfil` só é null antes do login carregar; nesse caso cai em
 * /perfil, a única rota que não exige nenhum perfil específico.
 *
 * Importante NÃO simplificar pra um fallback tipo
 * "perfil !== 'ALUNO' ? admin : aluno" - isso já causou um loop infinito
 * de redirecionamento no passado, quando existia um terceiro perfil
 * (Professor) sem rota própria: ele caía em /admin/eventos, que exigia
 * ADMIN, e o ProtectedRoute redirecionava de volta pro mesmo lugar.
 */
export function homeDoPerfil(perfil: Perfil | null): string {
  if (perfil === 'ADMIN') return '/admin/eventos';
  if (perfil === 'ALUNO') return '/eventos';
  return '/perfil';
}
