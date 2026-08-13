import { useCallback, useEffect, useState } from 'react';
import PageContainer from '../../components/PageContainer';
import Button from '../../components/Button';
import { LoadingBlock } from '../../components/LoadingSpinner';
import ErrorMessage from '../../components/ErrorMessage';
import EmptyState from '../../components/EmptyState';
import NovoUsuarioModal from './NovoUsuarioModal';
import { criarUsuario, listarUsuarios } from '../../services/usuarios';
import { extrairMensagemErro } from '../../services/api';
import { useToast } from '../../context/ToastContext';
import type { NovoUsuario, Usuario } from '../../types';

export default function AdminUsuariosPage() {
  const [usuarios, setUsuarios] = useState<Usuario[]>([]);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState<string | null>(null);
  const [modalAberto, setModalAberto] = useState(false);
  const { notificarSucesso } = useToast();

  const carregar = useCallback(() => {
    setCarregando(true);
    setErro(null);
    listarUsuarios('ALUNO')
      .then(setUsuarios)
      .catch((e) => setErro(extrairMensagemErro(e, 'Não foi possível carregar os alunos.')))
      .finally(() => setCarregando(false));
  }, []);

  useEffect(() => {
    carregar();
  }, [carregar]);

  const salvar = async (dados: NovoUsuario) => {
    try {
      await criarUsuario(dados);
      notificarSucesso('Usuário criado.');
      setModalAberto(false);
      carregar();
    } catch (e) {
      throw new Error(extrairMensagemErro(e, 'Não foi possível criar o usuário.'));
    }
  };

  return (
    <PageContainer
      titulo="Usuários"
      descricao="Alunos cadastrados no sistema"
      acao={<Button onClick={() => setModalAberto(true)}>Novo usuário</Button>}
    >
      {carregando && <LoadingBlock mensagem="Carregando usuários..." />}
      {!carregando && erro && <ErrorMessage mensagem={erro} onTentarNovamente={carregar} />}
      {!carregando && !erro && usuarios.length === 0 && (
        <EmptyState titulo="Nenhum aluno cadastrado" acao={<Button onClick={() => setModalAberto(true)}>Cadastrar aluno</Button>} />
      )}
      {!carregando && !erro && usuarios.length > 0 && (
        <div className="overflow-x-auto rounded-card border border-border bg-surface">
          <table className="w-full min-w-[480px] text-left text-sm">
            <thead>
              <tr className="border-b border-border text-text-muted">
                <th className="px-5 py-3 font-medium">Nome</th>
                <th className="px-5 py-3 font-medium">Email</th>
                <th className="px-5 py-3 font-medium">RGM</th>
              </tr>
            </thead>
            <tbody>
              {usuarios.map((usuario) => (
                <tr key={usuario.id} className="border-b border-border last:border-0">
                  <td className="px-5 py-3 text-text">{usuario.nome}</td>
                  <td className="px-5 py-3 text-text-muted">{usuario.email}</td>
                  <td className="px-5 py-3 text-text-muted">{usuario.rgm ?? '-'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {modalAberto && <NovoUsuarioModal onSalvar={salvar} onFechar={() => setModalAberto(false)} />}
    </PageContainer>
  );
}
