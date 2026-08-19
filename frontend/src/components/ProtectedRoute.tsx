import type { ReactNode } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
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
  const location = useLocation();

  if (!autenticado) {
    // Guarda a rota original (ex.: link do QR code de check-in) pra
    // LoginPage devolver o usuario pra ca depois do login, em vez de
    // sempre mandar pra home do perfil.
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  if (perfis && perfil && !perfis.includes(perfil)) {
    const home = perfil === 'ALUNO' ? '/eventos' : '/admin/eventos';
    return <Navigate to={home} replace />;
  }

  return <>{children}</>;
}
