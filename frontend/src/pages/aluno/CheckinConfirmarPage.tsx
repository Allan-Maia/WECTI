import { useEffect, useRef, useState } from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import PageContainer from '../../components/PageContainer';
import Button from '../../components/Button';
import { LoadingBlock } from '../../components/LoadingSpinner';
import { confirmarCheckinSessao } from '../../services/checkins';
import { extrairMensagemErro } from '../../services/api';
import type { Checkin } from '../../types';

/**
 * Aluno chega aqui ao escanear com a câmera do celular o QR code
 * projetado pelo admin (link embutido no PNG devolvido por
 * GET /checkin-sessoes/{sessaoId}/qrcode). A confirmação em si acontece
 * automaticamente ao abrir a página, autenticado como aluno - ver
 * ProtectedRoute/LoginPage pro fluxo de login-e-volta caso o aluno ainda
 * não estivesse logado quando escaneou (a query string é preservada, o
 * que importa muito aqui: o código está nela).
 *
 * O `c` da URL é o código rotativo daquela janela de tempo. É o que
 * impede que o link, repassado no grupo, sirva pra quem não está na
 * sala - alguns minutos depois ele não vale mais. Se o aluno demorar
 * (por exemplo, precisou fazer login no meio), a API recusa e ele
 * escaneia de novo o QR que está na tela - dessa vez já logado, é
 * instantâneo.
 */
export default function CheckinConfirmarPage() {
  const { sessaoId } = useParams<{ sessaoId: string }>();
  const [searchParams] = useSearchParams();
  const codigo = searchParams.get('c');
  const navigate = useNavigate();
  const [carregando, setCarregando] = useState(true);
  const [checkin, setCheckin] = useState<Checkin | null>(null);
  const [erro, setErro] = useState<string | null>(null);
  // React roda os efeitos duas vezes em dev (StrictMode) - sem essa
  // trava, a confirmação seria disparada duas vezes e a segunda sempre
  // voltaria com "já realizado", mesmo em telas que deram certo.
  const jaConfirmouRef = useRef(false);

  useEffect(() => {
    if (jaConfirmouRef.current) return;
    jaConfirmouRef.current = true;

    if (!sessaoId || !codigo) {
      // Sem o `c` na URL o link é de um QR antigo (ou foi digitado à
      // mão) - não dá pra confirmar nada com ele.
      setErro('QR code inválido. Escaneie o que está na tela agora.');
      setCarregando(false);
      return;
    }
    confirmarCheckinSessao(sessaoId, codigo)
      .then((resultado) => {
        setCheckin(resultado);
        setErro(null);
      })
      .catch((e) => {
        setErro(extrairMensagemErro(e, 'Não foi possível confirmar sua presença.'));
        setCheckin(null);
      })
      .finally(() => setCarregando(false));
  }, [sessaoId, codigo]);

  return (
    <PageContainer titulo="Confirmação de presença">
      <div className="mx-auto flex max-w-md flex-col items-center gap-4 rounded-card border border-border bg-surface p-8 text-center">
        {carregando && <LoadingBlock mensagem="Confirmando sua presença..." />}

        {!carregando && checkin && (
          <>
            <span className="text-4xl">✅</span>
            <h2 className="text-lg font-bold text-text">Presença confirmada!</h2>
            <p className="text-sm text-text-muted">
              {checkin.saida
                ? 'Seu check-out foi registrado. Obrigado por participar!'
                : 'Seu check-in foi registrado. Não esqueça de escanear o QR code de saída antes de ir embora, pra garantir seus pontos.'}
            </p>
          </>
        )}

        {!carregando && erro && (
          <>
            <span className="text-4xl">⚠️</span>
            <h2 className="text-lg font-bold text-text">Não foi possível confirmar</h2>
            <p className="text-sm text-red-400">{erro}</p>
            <p className="text-xs text-text-muted">
              O QR da tela muda de tempos em tempos. Se você demorou entre escanear e chegar aqui, é só
              apontar a câmera de novo - agora que já está logado, vai direto.
            </p>
          </>
        )}

        {!carregando && (
          <Button onClick={() => navigate('/eventos')} className="mt-2">
            Ir para Eventos
          </Button>
        )}
      </div>
    </PageContainer>
  );
}
