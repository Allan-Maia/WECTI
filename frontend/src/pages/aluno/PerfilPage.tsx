import { useEffect, useState } from 'react';
import PageContainer from '../../components/PageContainer';
import { LoadingBlock } from '../../components/LoadingSpinner';
import ErrorMessage from '../../components/ErrorMessage';
import { buscarMeuUsuario } from '../../services/usuarios';
import { extrairMensagemErro } from '../../services/api';
import type { Usuario } from '../../types';

const LABEL_PERFIL: Record<string, string> = {
  ADMIN: 'Administrador',
  PROFESSOR: 'Professor',
  ALUNO: 'Aluno',
};

export default function PerfilPage() {
  const [usuario, setUsuario] = useState<Usuario | null>(null);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState<string | null>(null);

  useEffect(() => {
    buscarMeuUsuario()
      .then(setUsuario)
      .catch((e) => setErro(extrairMensagemErro(e, 'Não foi possível carregar seu perfil.')))
      .finally(() => setCarregando(false));
  }, []);

  return (
    <PageContainer titulo="Meu perfil">
      {carregando && <LoadingBlock mensagem="Carregando perfil..." />}
      {!carregando && erro && <ErrorMessage mensagem={erro} />}
      {!carregando && usuario && (
        <div className="max-w-md rounded-card border border-border bg-surface p-6">
          <dl className="flex flex-col gap-4">
            <div>
              <dt className="text-xs uppercase tracking-wider text-text-muted">Nome</dt>
              <dd className="mt-1 text-text">{usuario.nome}</dd>
            </div>
            <div>
              <dt className="text-xs uppercase tracking-wider text-text-muted">Email</dt>
              <dd className="mt-1 text-text">{usuario.email}</dd>
            </div>
            <div>
              <dt className="text-xs uppercase tracking-wider text-text-muted">RGM</dt>
              <dd className="mt-1 text-text">{usuario.rgm ?? '-'}</dd>
            </div>
            <div>
              <dt className="text-xs uppercase tracking-wider text-text-muted">Perfil</dt>
              <dd className="mt-1 text-text">{LABEL_PERFIL[usuario.perfil] ?? usuario.perfil}</dd>
            </div>
          </dl>
        </div>
      )}
    </PageContainer>
  );
}
