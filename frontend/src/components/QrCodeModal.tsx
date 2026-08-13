import { useEffect, useState } from 'react';
import { baixarQrCode } from '../services/inscricoes';
import { LoadingBlock } from './LoadingSpinner';
import ErrorMessage from './ErrorMessage';
import { extrairMensagemErro } from '../services/api';

export default function QrCodeModal({ inscricaoId, onClose }: { inscricaoId: string; onClose: () => void }) {
  const [urlImagem, setUrlImagem] = useState<string | null>(null);
  const [erro, setErro] = useState<string | null>(null);

  useEffect(() => {
    let urlAtual: string | null = null;
    baixarQrCode(inscricaoId)
      .then((blob) => {
        urlAtual = URL.createObjectURL(blob);
        setUrlImagem(urlAtual);
      })
      .catch((e) => setErro(extrairMensagemErro(e, 'Não foi possível carregar o QR code.')));
    return () => {
      if (urlAtual) URL.revokeObjectURL(urlAtual);
    };
  }, [inscricaoId]);

  return (
    <div
      className="fixed inset-0 z-[200] flex items-center justify-center bg-black/80 p-4"
      onClick={onClose}
    >
      <div
        className="w-full max-w-xs rounded-card border border-border bg-surface p-6 text-center"
        onClick={(e) => e.stopPropagation()}
      >
        <h2 className="mb-4 text-sm font-semibold text-text">QR code da inscrição</h2>
        {!urlImagem && !erro && <LoadingBlock mensagem="Gerando QR code..." />}
        {erro && <ErrorMessage mensagem={erro} />}
        {urlImagem && <img src={urlImagem} alt="QR code da inscrição" className="mx-auto rounded-lg bg-white p-2" />}
        <button
          onClick={onClose}
          className="mt-5 w-full rounded-full border border-border py-2 text-sm text-text-muted transition hover:border-accent hover:text-accent"
        >
          Fechar
        </button>
      </div>
    </div>
  );
}
