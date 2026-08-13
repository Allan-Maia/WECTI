import { useCallback, useEffect, useState } from 'react';
import { listarEventos, type FiltroEventos } from '../services/eventos';
import { extrairMensagemErro } from '../services/api';
import type { Evento } from '../types';

export function useEventos(filtro: FiltroEventos = {}) {
  const [eventos, setEventos] = useState<Evento[]>([]);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState<string | null>(null);

  const periodoId = filtro.periodo_id;
  const status = filtro.status;

  const recarregar = useCallback(() => {
    setCarregando(true);
    setErro(null);
    listarEventos({ periodo_id: periodoId, status })
      .then(setEventos)
      .catch((e) => setErro(extrairMensagemErro(e, 'Nao foi possivel carregar os eventos.')))
      .finally(() => setCarregando(false));
  }, [periodoId, status]);

  useEffect(() => {
    recarregar();
  }, [recarregar]);

  return { eventos, carregando, erro, recarregar };
}
