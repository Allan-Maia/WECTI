import { useCallback, useEffect, useState } from 'react';
import { buscarMinhaPontuacao } from '../services/pontuacao';
import { extrairMensagemErro } from '../services/api';
import type { Pontuacao } from '../types';

/**
 * Pontuação do aluno logado.
 *
 * Antes este hook tinha um estado extra, `semPeriodoAtivo`, para tratar o
 * 404 de "nenhum período ativo" como situação normal em vez de erro. Esse
 * caso deixou de existir junto com o conceito de período: a pontuação
 * agora é simplesmente a do aluno no WECTI, e a API sempre responde -
 * mesmo que com zero pontos.
 */
export function usePontuacao() {
  const [pontuacao, setPontuacao] = useState<Pontuacao | null>(null);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState<string | null>(null);

  const recarregar = useCallback(() => {
    setCarregando(true);
    setErro(null);
    buscarMinhaPontuacao()
      .then(setPontuacao)
      .catch((e) => setErro(extrairMensagemErro(e, 'Nao foi possivel carregar a pontuacao.')))
      .finally(() => setCarregando(false));
  }, []);

  useEffect(() => {
    recarregar();
  }, [recarregar]);

  return { pontuacao, carregando, erro, recarregar };
}
