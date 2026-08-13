import axios from 'axios';

export const TOKEN_KEY = 'wecti_token';
export const USUARIO_KEY = 'wecti_usuario';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080',
});

// Request: injeta o Bearer token em toda chamada, se existir.
api.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Response: 401 (token ausente/expirado/invalido) -> limpa sessao e volta
// pro login. Evita loop infinito se o proprio /auth/login devolver 401
// (credenciais invalidas) - nesse caso so deixa o erro subir normalmente.
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const isLoginRequest = error.config?.url?.includes('/auth/login');
    if (error.response?.status === 401 && !isLoginRequest) {
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(USUARIO_KEY);
      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);

export default api;

/** Extrai a mensagem de erro do padrao ErroResponse do backend, com fallback. */
export function extrairMensagemErro(error: unknown, fallback = 'Ocorreu um erro. Tente novamente.'): string {
  if (axios.isAxiosError(error)) {
    const mensagem = error.response?.data?.mensagem;
    if (typeof mensagem === 'string' && mensagem.length > 0) {
      return mensagem;
    }
  }
  return fallback;
}
