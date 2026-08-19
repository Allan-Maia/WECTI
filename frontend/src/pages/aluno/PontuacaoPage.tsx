import PageContainer from '../../components/PageContainer';
import Badge from '../../components/Badge';
import { LoadingBlock } from '../../components/LoadingSpinner';
import ErrorMessage from '../../components/ErrorMessage';
import EmptyState from '../../components/EmptyState';
import { usePontuacao } from '../../hooks/usePontuacao';

export default function PontuacaoPage() {
  const { pontuacao, carregando, erro, semPeriodoAtivo, recarregar } = usePontuacao();

  return (
    <PageContainer titulo="Pontuação" descricao="Sua pontuação no período atual">
      {carregando && <LoadingBlock mensagem="Carregando pontuação..." />}
      {!carregando && erro && <ErrorMessage mensagem={erro} onTentarNovamente={recarregar} />}
      {!carregando && !erro && semPeriodoAtivo && (
        <EmptyState
          titulo="Nenhum período ativo"
          descricao="Ainda não há um período (semestre) cadastrado para a data de hoje."
          icone="🗓️"
        />
      )}
      {!carregando && !erro && pontuacao && (
        <div className="flex flex-col gap-6">
          <div className="rounded-card border border-border bg-surface p-8 text-center">
            <p className="text-5xl font-extrabold text-accent">{pontuacao.pontos_total}</p>
            <p className="mt-1 text-sm text-text-muted">pontos acumulados</p>
          </div>

          {pontuacao.eventos.length === 0 ? (
            <EmptyState titulo="Nenhum evento pontuado ainda" icone="⭐" />
          ) : (
            <div className="overflow-x-auto rounded-card border border-border bg-surface">
              <table className="w-full min-w-[520px] text-left text-sm">
                <thead>
                  <tr className="border-b border-border text-text-muted">
                    <th className="px-5 py-3 font-medium">Evento</th>
                    <th className="px-5 py-3 font-medium">Status</th>
                    <th className="px-5 py-3 font-medium text-right">Pontos</th>
                  </tr>
                </thead>
                <tbody>
                  {pontuacao.eventos.map((item) => (
                    <tr key={item.evento_id} className="border-b border-border last:border-0">
                      <td className="px-5 py-3 text-text">{item.titulo}</td>
                      <td className="px-5 py-3">
                        <Badge status={item.status} />
                      </td>
                      <td
                        className={`px-5 py-3 text-right font-semibold ${
                          item.pontos > 0 ? 'text-accent' : item.pontos < 0 ? 'text-red-400' : 'text-text-muted'
                        }`}
                      >
                        {item.pontos > 0 ? `+${item.pontos}` : item.pontos}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}
    </PageContainer>
  );
}
