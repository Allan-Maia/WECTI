import { defineConfig, type Plugin, type Connect } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

/**
 * Faz a raiz (`/`) abrir a landing do evento, e nao a casca do sistema.
 *
 * Em producao quem cuida disso e o `DirectoryIndex` do
 * frontend/public/.htaccess. O Vite nao le .htaccess, entao sem este
 * plugin `npm run dev` abriria a tela de login em `/` - diferente do que
 * o visitante ve no ar, que e justamente o tipo de divergencia que so
 * aparece depois do deploy.
 *
 * So a raiz exata e desviada. `/index.html` precisa continuar chegando
 * intacto: e para ele que o Vite redireciona todas as rotas do React
 * (/login, /eventos, /checkin/...) - reescrever esse caminho quebraria o
 * sistema inteiro.
 */
function landingNaRaiz(): Plugin {
  const desviarRaiz: Connect.NextHandleFunction = (req, _res, next) => {
    const caminho = (req.url ?? '').split('?')[0]
    if (caminho === '/') {
      req.url = '/home.html' + (req.url!.includes('?') ? req.url!.slice(req.url!.indexOf('?')) : '')
    }
    next()
  }

  return {
    name: 'wecti-landing-na-raiz',
    // A forma direta (sem retornar funcao) registra o middleware ANTES
    // dos internos do Vite - necessario para pegar `/` antes do fallback
    // de SPA servir o index.html.
    configureServer(server) {
      server.middlewares.use(desviarRaiz)
    },
    // `vite preview` serve o dist/ e tambem nao le .htaccess.
    configurePreviewServer(server) {
      server.middlewares.use(desviarRaiz)
    },
  }
}

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss(), landingNaRaiz()],
  server: {
    host: '0.0.0.0',
    port: 5173
  }
})
