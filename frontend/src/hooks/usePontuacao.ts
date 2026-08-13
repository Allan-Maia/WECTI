import { useCallback, useEffect, useState } from 'react';
import axios from 'axios';
import { buscarMinhaPontuacao } from '../services/pontuacao';
import { extrairMensagemErro } from '../services/api';
import type { PontuacaoPeriodo } from '../types';

export function usePontuacao(periodoId?: string) {
  const [pontuacao, setPontuacao] = useState<PontuacaoPeriodo | null>(null);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState<string | null>(null);
  // 404 aqui normalmente significa "nao ha periodo ativo cadastrado ainda" -
  // um estado normal, nao uma falha real (ver PontuacaoPage/HistoricoPage).
  const [semPeriodoAtivo, setSemPeriodoAtivo] = useState(false);

  const recarregar = useCallback(() => {
    setCarregando(true);
    setErro(null);
    setSemPeriodoAtivo(false);
    buscarMinhaPontuacao(periodoId)
      .then(setPontuacao)
      .catch((e) => {
        if (axios.isAxiosError(e) && e.response?.status === 404) {
          setSemPeriodoAtivo(true);
          return;
        }
        setErro(extrairMensagemErro(e, 'Nao foi possivel carregar a pontuacao.'));
      })
      .finally(() => setCarregando(false));
  }, [periodoId]);

  useEffect(() => {
    recarregar();
  }, [recarregar]);

  return { pontuacao, carregando, erro, semPeriodoAtivo, recarregar };
}
