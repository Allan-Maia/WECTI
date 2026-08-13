import { useCallback, useEffect, useState } from 'react';
import { listarMinhasInscricoes } from '../services/inscricoes';
import { extrairMensagemErro } from '../services/api';
import type { Inscricao } from '../types';

export function useInscricoes(status?: 'futuros' | 'historico') {
  const [inscricoes, setInscricoes] = useState<Inscricao[]>([]);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState<string | null>(null);

  const recarregar = useCallback(() => {
    setCarregando(true);
    setErro(null);
    listarMinhasInscricoes(status)
      .then(setInscricoes)
      .catch((e) => setErro(extrairMensagemErro(e, 'Nao foi possivel carregar as inscricoes.')))
      .finally(() => setCarregando(false));
  }, [status]);

  useEffect(() => {
    recarregar();
  }, [recarregar]);

  return { inscricoes, carregando, erro, recarregar };
}
