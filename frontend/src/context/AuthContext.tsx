import { createContext, useContext, useMemo, useState, type ReactNode } from 'react';
import { jwtDecode } from 'jwt-decode';
import { login as loginRequest } from '../services/auth';
import { TOKEN_KEY, USUARIO_KEY } from '../services/api';
import type { JwtPayload, LoginRequest, Perfil, Usuario } from '../types';

interface AuthContextValue {
  usuario: Usuario | null;
  perfil: Perfil | null;
  autenticado: boolean;
  entrar: (dados: LoginRequest) => Promise<Usuario>;
  sair: () => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

function lerUsuarioSalvo(): Usuario | null {
  const bruto = localStorage.getItem(USUARIO_KEY);
  if (!bruto) return null;
  try {
    return JSON.parse(bruto) as Usuario;
  } catch {
    return null;
  }
}

/** Perfil extraido do payload do JWT (claim "perfil"), como pedido - serve
 *  de fonte da verdade independente do objeto "usuario" guardado, caso um
 *  dia fiquem dessincronizados. */
function perfilDoToken(token: string | null): Perfil | null {
  if (!token) return null;
  try {
    const payload = jwtDecode<JwtPayload>(token);
    return payload.perfil;
  } catch {
    return null;
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [usuario, setUsuario] = useState<Usuario | null>(() => lerUsuarioSalvo());
  const [token, setToken] = useState<string | null>(() => localStorage.getItem(TOKEN_KEY));

  const entrar = async (dados: LoginRequest) => {
    const resposta = await loginRequest(dados);
    localStorage.setItem(TOKEN_KEY, resposta.token);
    localStorage.setItem(USUARIO_KEY, JSON.stringify(resposta.usuario));
    setToken(resposta.token);
    setUsuario(resposta.usuario);
    return resposta.usuario;
  };

  const sair = () => {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USUARIO_KEY);
    setToken(null);
    setUsuario(null);
  };

  const perfil = useMemo(() => usuario?.perfil ?? perfilDoToken(token), [usuario, token]);

  const value: AuthContextValue = {
    usuario,
    perfil,
    autenticado: Boolean(token),
    entrar,
    sair,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth precisa ser usado dentro de um AuthProvider');
  }
  return ctx;
}
