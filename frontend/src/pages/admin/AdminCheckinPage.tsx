import { useState } from 'react';
import PageContainer from '../../components/PageContainer';
import Button from '../../components/Button';
import FormField from '../../components/FormField';
import { registrarEntrada, registrarSaida } from '../../services/checkins';
import { extrairMensagemErro } from '../../services/api';
import { useToast } from '../../context/ToastContext';
import { formatarDataHora } from '../../utils/data';
import type { Checkin } from '../../types';

/**
 * O contrato so tem POST /checkins (recebe qrcode_token, registra entrada)
 * e POST /checkins/{id}/checkout (recebe o id interno do checkin) - nao
 * existe um endpoint de busca/consulta por token. Por isso "buscar" aqui
 * e a propria tentativa de registrar entrada; o botao de saida so fica
 * disponivel para o checkin que acabou de ser criado nesta tela.
 */
export default function AdminCheckinPage() {
  const [token, setToken] = useState('');
  const [checkin, setCheckin] = useState<Checkin | null>(null);
  const [carregandoEntrada, setCarregandoEntrada] = useState(false);
  const [carregandoSaida, setCarregandoSaida] = useState(false);
  const [erro, setErro] = useState<string | null>(null);
  const { notificarSucesso, notificarErro } = useToast();

  const buscarERegistrarEntrada = async () => {
    if (!token.trim()) return;
    setErro(null);
    setCarregandoEntrada(true);
    try {
      const resultado = await registrarEntrada(token.trim());
      setCheckin(resultado);
      notificarSucesso('Entrada registrada com sucesso.');
    } catch (e) {
      setCheckin(null);
      setErro(extrairMensagemErro(e, 'QR code inválido ou check-in já realizado.'));
    } finally {
      setCarregandoEntrada(false);
    }
  };

  const registrarSaidaDoCheckin = async () => {
    if (!checkin) return;
    setCarregandoSaida(true);
    try {
      const atualizado = await registrarSaida(checkin.id);
      setCheckin(atualizado);
      notificarSucesso('Saída registrada com sucesso.');
    } catch (e) {
      notificarErro(extrairMensagemErro(e, 'Não foi possível registrar a saída.'));
    } finally {
      setCarregandoSaida(false);
    }
  };

  return (
    <PageContainer titulo="Check-in" descricao="Escaneie ou digite o token do QR code do aluno">
      <div className="flex flex-col gap-4 rounded-card border border-border bg-surface p-6">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-end">
          <div className="flex-1">
            <FormField
              label="Token do QR code"
              value={token}
              onChange={(e) => setToken(e.target.value)}
              placeholder="Cole ou escaneie o token aqui"
            />
          </div>
          <Button carregando={carregandoEntrada} onClick={buscarERegistrarEntrada} className="sm:mb-0">
            Registrar entrada
          </Button>
        </div>

        {erro && (
          <p className="rounded-lg border border-red-500/30 bg-red-500/5 px-3 py-2 text-sm text-red-400">{erro}</p>
        )}

        {checkin && (
          <div className="rounded-card border border-accent/30 bg-accent/5 p-5">
            <p className="text-sm text-text-muted">Entrada registrada em</p>
            <p className="mb-3 font-semibold text-text">{formatarDataHora(checkin.entrada)}</p>

            {checkin.saida ? (
              <>
                <p className="text-sm text-text-muted">Saída registrada em</p>
                <p className="font-semibold text-text">{formatarDataHora(checkin.saida)}</p>
                {checkin.percentual_presenca !== null && (
                  <p className="mt-2 text-sm text-text-muted">
                    Permanência: <span className="font-semibold text-accent">{checkin.percentual_presenca.toFixed(0)}%</span>
                  </p>
                )}
              </>
            ) : (
              <Button carregando={carregandoSaida} onClick={registrarSaidaDoCheckin}>
                Registrar saída
              </Button>
            )}
          </div>
        )}
      </div>
    </PageContainer>
  );
}
