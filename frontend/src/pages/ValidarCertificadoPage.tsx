import { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import Logo from '../components/Logo';
import Button from '../components/Button';
import FormField from '../components/FormField';
import { LoadingBlock } from '../components/LoadingSpinner';
import { validarCertificado } from '../services/certificados';
import { extrairMensagemErro } from '../services/api';
import { formatarData, formatarDataHora } from '../utils/data';
import type { CertificadoValidacao } from '../types';

/**
 * Pagina PUBLICA de validacao de certificado (sem login).
 *
 * Atende dois caminhos:
 *  - /validar/{codigo} - vindo do QR code impresso no certificado; ja
 *    consulta sozinha ao abrir.
 *  - /validar - vindo de quem prefere digitar o codigo a mao.
 *
 * Nao exibe RGM: quem valida pode ser qualquer pessoa com o codigo em
 * maos (recrutador, outra instituicao), e nome/evento/data/carga horaria
 * ja bastam pra conferir autenticidade.
 */
export default function ValidarCertificadoPage() {
  const { codigo: codigoDaUrl } = useParams<{ codigo: string }>();
  const navigate = useNavigate();

  const [codigoDigitado, setCodigoDigitado] = useState(codigoDaUrl ?? '');
  const [certificado, setCertificado] = useState<CertificadoValidacao | null>(null);
  const [erro, setErro] = useState<string | null>(null);
  const [carregando, setCarregando] = useState(false);

  const consultar = useCallback((codigo: string) => {
    setCarregando(true);
    setErro(null);
    setCertificado(null);
    validarCertificado(codigo)
      .then(setCertificado)
      .catch((e) =>
        setErro(extrairMensagemErro(e, 'Não foi possível validar este código. Confira se digitou corretamente.')),
      )
      .finally(() => setCarregando(false));
  }, []);

  useEffect(() => {
    if (codigoDaUrl) {
      setCodigoDigitado(codigoDaUrl);
      consultar(codigoDaUrl);
    }
  }, [codigoDaUrl, consultar]);

  const enviar = (e: React.FormEvent) => {
    e.preventDefault();
    const limpo = codigoDigitado.trim();
    if (!limpo) return;
    // Navega em vez de consultar direto pra a URL ficar compartilhavel
    // (o efeito acima dispara a consulta).
    navigate(`/validar/${encodeURIComponent(limpo)}`);
  };

  return (
    <div className="relative flex min-h-screen items-center justify-center overflow-hidden bg-bg px-4 py-12">
      <div
        className="pointer-events-none absolute inset-0"
        style={{
          background:
            'radial-gradient(ellipse 80% 60% at 60% 40%, rgba(0,212,176,0.10) 0%, transparent 70%), radial-gradient(ellipse 60% 50% at 20% 70%, rgba(0,168,138,0.10) 0%, transparent 60%)',
        }}
      />

      <div className="relative z-10 w-full max-w-md">
        <div className="mb-8 flex justify-center">
          <Logo />
        </div>

        <div className="flex flex-col gap-4 rounded-card border border-border bg-surface p-8">
          <div className="mb-2 text-center">
            <h1 className="text-lg font-bold text-text">Validar certificado</h1>
            <p className="mt-1 text-sm text-text-muted">
              Informe o código impresso no certificado para conferir sua autenticidade
            </p>
          </div>

          <form onSubmit={enviar} className="flex flex-col gap-4">
            <FormField
              label="Código do certificado"
              placeholder="WCT-2026-A7F3K2"
              value={codigoDigitado}
              onChange={(e) => setCodigoDigitado(e.target.value)}
            />
            <Button type="submit" carregando={carregando} className="w-full">
              Validar
            </Button>
          </form>

          {carregando && <LoadingBlock mensagem="Consultando..." />}

          {!carregando && certificado && (
            <div className="flex flex-col gap-3 rounded-card border border-accent/30 bg-accent/5 p-5">
              <div className="flex items-center gap-2">
                <span className="text-xl" aria-hidden>
                  ✅
                </span>
                <p className="font-semibold text-accent">Certificado válido</p>
              </div>

              <dl className="flex flex-col gap-2.5 text-sm">
                <div>
                  <dt className="text-xs uppercase tracking-wider text-text-muted">Aluno</dt>
                  <dd className="font-semibold text-text">{certificado.aluno_nome}</dd>
                </div>
                <div>
                  <dt className="text-xs uppercase tracking-wider text-text-muted">Evento</dt>
                  <dd className="text-text">{certificado.evento_titulo}</dd>
                </div>
                <div className="flex gap-8">
                  <div>
                    <dt className="text-xs uppercase tracking-wider text-text-muted">Realização</dt>
                    <dd className="text-text">{formatarData(certificado.data_realizacao)}</dd>
                  </div>
                  <div>
                    <dt className="text-xs uppercase tracking-wider text-text-muted">Carga horária</dt>
                    <dd className="text-text">{certificado.carga_horaria}</dd>
                  </div>
                </div>
                <div>
                  <dt className="text-xs uppercase tracking-wider text-text-muted">Emitido em</dt>
                  <dd className="text-text">{formatarDataHora(certificado.emitido_em)}</dd>
                </div>
                <div>
                  <dt className="text-xs uppercase tracking-wider text-text-muted">Código</dt>
                  <dd className="font-mono text-text">{certificado.codigo}</dd>
                </div>
              </dl>
            </div>
          )}

          {!carregando && erro && (
            <div className="flex flex-col gap-2 rounded-lg border border-red-500/30 bg-red-500/5 px-4 py-3">
              <div className="flex items-center gap-2">
                <span aria-hidden>⚠️</span>
                <p className="text-sm font-semibold text-red-400">Certificado não encontrado</p>
              </div>
              <p className="text-sm text-red-400">{erro}</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
