import axios from 'axios';

export const TOKEN_KEY = 'wecti_token';
export const USUARIO_KEY = 'wecti_usuario';

/**
 * Sem VITE_API_URL definido (dev local), usa o mesmo host que serviu a
 * página, só trocando a porta pra 8080 - em vez de "localhost" fixo.
 * "localhost" só resolve pra "essa mesma máquina": funciona testando no
 * próprio notebook, mas quebra ao abrir o site de outro aparelho na rede
 * (ex.: celular acessando http://<ip-do-notebook>:5173) - lá,
 * "localhost:8080" aponta pro celular, que não tem o backend rodando.
 * Em produção, VITE_API_URL vem de .env.production e sempre tem
 * prioridade sobre isso.
 */
function baseURLPadrao(): string {
  return `${window.location.protocol}//${window.location.hostname}:8080`;
}

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || baseURLPadrao(),
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
