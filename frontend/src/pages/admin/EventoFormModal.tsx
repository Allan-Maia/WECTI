import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import Button from '../../components/Button';
import FormField from '../../components/FormField';
import { listarPeriodos } from '../../services/periodos';
import { listarPalestrantes } from '../../services/palestrantes';
import type { Evento, Periodo, Palestrante } from '../../types';
import { paraInputDateTime } from '../../utils/data';

const schema = z.object({
  titulo: z.string().min(1, 'Informe o título'),
  descricao: z.string().optional(),
  periodo_id: z.string().min(1, 'Selecione um período'),
  local: z.string().optional(),
  data_hora_inicio: z.string().min(1, 'Informe a data de início'),
  data_hora_fim: z.string().min(1, 'Informe a data de término'),
  pontos: z.coerce.number().int('Deve ser um número inteiro').min(0, 'Não pode ser negativo'),
});

type FormInput = z.input<typeof schema>;
type FormValues = z.output<typeof schema>;

interface Props {
  evento?: Evento | null;
  onSalvar: (dados: FormValues & { palestrante_ids: string[] }) => Promise<void>;
  onFechar: () => void;
}

export default function EventoFormModal({ evento, onSalvar, onFechar }: Props) {
  const [periodos, setPeriodos] = useState<Periodo[]>([]);
  const [palestrantes, setPalestrantes] = useState<Palestrante[]>([]);
  const [selecionados, setSelecionados] = useState<string[]>(evento?.palestrantes.map((p) => p.id) ?? []);
  const [salvando, setSalvando] = useState(false);
  const [erroGeral, setErroGeral] = useState<string | null>(null);

  useEffect(() => {
    listarPeriodos().then(setPeriodos).catch(() => setPeriodos([]));
    listarPalestrantes().then(setPalestrantes).catch(() => setPalestrantes([]));
  }, []);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<FormInput, unknown, FormValues>({
    resolver: zodResolver(schema),
    defaultValues: evento
      ? {
          titulo: evento.titulo,
          descricao: evento.descricao ?? '',
          periodo_id: evento.periodo_id,
          local: evento.local ?? '',
          data_hora_inicio: paraInputDateTime(evento.data_hora_inicio),
          data_hora_fim: paraInputDateTime(evento.data_hora_fim),
          pontos: evento.pontos,
        }
      : { pontos: 0 },
  });

  const alternarPalestrante = (id: string) => {
    setSelecionados((atual) => (atual.includes(id) ? atual.filter((p) => p !== id) : [...atual, id]));
  };

  const onSubmit = async (dados: FormValues) => {
    setErroGeral(null);
    setSalvando(true);
    try {
      await onSalvar({ ...dados, palestrante_ids: selecionados });
    } catch (e) {
      setErroGeral(e instanceof Error ? e.message : 'Não foi possível salvar o evento.');
    } finally {
      setSalvando(false);
    }
  };

  return (
    <div className="fixed inset-0 z-[200] flex items-center justify-center bg-black/80 p-4" onClick={onFechar}>
      <form
        onSubmit={handleSubmit(onSubmit)}
        onClick={(e) => e.stopPropagation()}
        className="flex max-h-[90vh] w-full max-w-lg flex-col gap-4 overflow-y-auto rounded-card border border-border bg-surface p-6"
      >
        <h2 className="text-lg font-bold text-text">{evento ? 'Editar evento' : 'Novo evento'}</h2>

        <FormField label="Título" erro={errors.titulo?.message} registro={register('titulo')} />
        <FormField label="Descrição" erro={errors.descricao?.message} registro={register('descricao')} />

        <label className="flex flex-col gap-1.5 text-sm">
          <span className="font-medium text-text">Período</span>
          <select
            className="rounded-lg border border-border bg-surface px-3.5 py-2.5 text-text outline-none focus:border-accent"
            {...register('periodo_id')}
          >
            <option value="">Selecione...</option>
            {periodos.map((p) => (
              <option key={p.id} value={p.id}>
                {p.nome}
              </option>
            ))}
          </select>
          {errors.periodo_id && <span className="text-xs text-red-400">{errors.periodo_id.message}</span>}
        </label>

        <FormField label="Local" erro={errors.local?.message} registro={register('local')} />

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <FormField
            label="Início"
            type="datetime-local"
            erro={errors.data_hora_inicio?.message}
            registro={register('data_hora_inicio')}
          />
          <FormField
            label="Término"
            type="datetime-local"
            erro={errors.data_hora_fim?.message}
            registro={register('data_hora_fim')}
          />
        </div>

        <FormField label="Pontos" type="number" min={0} erro={errors.pontos?.message} registro={register('pontos')} />

        {palestrantes.length > 0 && (
          <div>
            <span className="mb-1.5 block text-sm font-medium text-text">Palestrantes</span>
            <div className="flex flex-wrap gap-2">
              {palestrantes.map((p) => (
                <button
                  type="button"
                  key={p.id}
                  onClick={() => alternarPalestrante(p.id)}
                  className={`rounded-full border px-3 py-1.5 text-xs font-medium transition ${
                    selecionados.includes(p.id)
                      ? 'border-accent bg-accent/10 text-accent'
                      : 'border-border text-text-muted hover:border-accent/50'
                  }`}
                >
                  {p.nome}
                </button>
              ))}
            </div>
          </div>
        )}

        {erroGeral && (
          <p className="rounded-lg border border-red-500/30 bg-red-500/5 px-3 py-2 text-sm text-red-400">{erroGeral}</p>
        )}

        <div className="mt-2 flex justify-end gap-3">
          <Button type="button" variante="outline" onClick={onFechar}>
            Cancelar
          </Button>
          <Button type="submit" carregando={salvando}>
            Salvar
          </Button>
        </div>
      </form>
    </div>
  );
}
