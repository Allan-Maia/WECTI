import type { ReactNode } from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import type { Perfil } from '../types';

interface Props {
  children: ReactNode;
  /** Se informado, restringe a rota a esses perfis - quem nao bate e
   *  redirecionado pra home do proprio perfil. */
  perfis?: Perfil[];
}

export default function ProtectedRoute({ children, perfis }: Props) {
  const { autenticado, perfil } = useAuth();

  if (!autenticado) {
    return <Navigate to="/login" replace />;
  }

  if (perfis && perfil && !perfis.includes(perfil)) {
    const home = perfil === 'ALUNO' ? '/eventos' : '/admin/eventos';
    return <Navigate to={home} replace />;
  }

  return <>{children}</>;
}
