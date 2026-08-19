import { useEffect, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import PageContainer from '../../components/PageContainer';
import Button from '../../components/Button';
import { LoadingBlock } from '../../components/LoadingSpinner';
import { confirmarCheckinSessao } from '../../services/checkins';
import { extrairMensagemErro } from '../../services/api';
import type { Checkin } from '../../types';

/**
 * Aluno chega aqui ao escanear com a câmera do celular o QR code
 * projetado pelo admin/professor (link embutido no PNG gerado por
 * GET /checkin-sessoes/{sessaoId}/qrcode). A confirmação em si acontece
 * automaticamente ao abrir a página, autenticado como aluno - ver
 * ProtectedRoute/LoginPage pro fluxo de login-e-volta caso o aluno ainda
 * não estivesse logado quando escaneou.
 */
export default function CheckinConfirmarPage() {
  const { sessaoId } = useParams<{ sessaoId: string }>();
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

    if (!sessaoId) {
      setErro('QR code inválido.');
      setCarregando(false);
      return;
    }
    confirmarCheckinSessao(sessaoId)
      .then((resultado) => {
        setCheckin(resultado);
        setErro(null);
      })
      .catch((e) => {
        setErro(extrairMensagemErro(e, 'Não foi possível confirmar sua presença.'));
        setCheckin(null);
      })
      .finally(() => setCarregando(false));
  }, [sessaoId]);

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
