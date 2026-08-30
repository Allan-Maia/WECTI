import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import Button from '../../components/Button';
import FormField from '../../components/FormField';
import { listarPalestrantes } from '../../services/palestrantes';
import type { Evento, Palestrante } from '../../types';
import { paraInputDateTime } from '../../utils/data';

// Sem periodo_id aqui: o backend descobre sozinho o Periodo (semestre) a
// partir da data do evento - ver EventoService.buscarPeriodoPelaData.
//
// O schema é uma função (não um objeto fixo) porque a validação de "início
// não pode ser no passado" só faz sentido pra data nova - editando um
// evento que já começou (ou já terminou) sem mexer no campo de início,
// não faz sentido barrar o salvamento só por causa da data original já
// ter passado (ex.: corrigir a descrição ou os pontos de um evento
// encerrado). `valorOriginalInicio` é o valor de início como veio no
// formulário (evento existente) - só valida contra "agora" se for um
// evento novo ou se o valor foi alterado.
function construirSchema(valorOriginalInicio: string | undefined) {
  return z
    .object({
      titulo: z.string().min(1, 'Informe o título'),
      descricao: z.string().optional(),
      local: z.string().optional(),
      data_hora_inicio: z.string().min(1, 'Informe a data de início'),
      data_hora_fim: z.string().min(1, 'Informe a data de término'),
      pontos: z.coerce.number().int('Deve ser um número inteiro').min(0, 'Não pode ser negativo'),
      // Vazio = sem limite de vagas. Precisa passar por string primeiro
      // porque um <input type="number"> em branco chega como '', e
      // z.coerce.number() transformaria isso em 0 - ou seja, um evento
      // com zero vagas, que ninguém conseguiria acessar.
      capacidade: z
        .string()
        .optional()
        .transform((valor) => (valor == null || valor.trim() === '' ? null : Number(valor)))
        .refine((valor) => valor === null || (Number.isInteger(valor) && valor >= 1), {
          message: 'Informe pelo menos 1 vaga, ou deixe em branco para ilimitado.',
        }),
    })
    .refine(
      (dados) =>
        dados.data_hora_inicio === valorOriginalInicio || new Date(dados.data_hora_inicio) >= new Date(),
      {
        message: 'A data de início não pode ser no passado.',
        path: ['data_hora_inicio'],
      },
    )
    .refine((dados) => new Date(dados.data_hora_fim) > new Date(dados.data_hora_inicio), {
      message: 'A data de término deve ser após o início.',
      path: ['data_hora_fim'],
    });
}

type EventoSchema = ReturnType<typeof construirSchema>;
type FormInput = z.input<EventoSchema>;
type FormValues = z.output<EventoSchema>;

interface Props {
  evento?: Evento | null;
  onSalvar: (dados: FormValues & { palestrante_ids: string[] }) => Promise<void>;
  onFechar: () => void;
}

export default function EventoFormModal({ evento, onSalvar, onFechar }: Props) {
  const [palestrantes, setPalestrantes] = useState<Palestrante[]>([]);
  const [selecionados, setSelecionados] = useState<string[]>(evento?.palestrantes.map((p) => p.id) ?? []);
  const [salvando, setSalvando] = useState(false);
  const [erroGeral, setErroGeral] = useState<string | null>(null);

  useEffect(() => {
    listarPalestrantes().then(setPalestrantes).catch(() => setPalestrantes([]));
  }, []);

  const valorOriginalInicio = evento ? paraInputDateTime(evento.data_hora_inicio) : undefined;

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<FormInput, unknown, FormValues>({
    resolver: zodResolver(construirSchema(valorOriginalInicio)),
    defaultValues: evento
      ? {
          titulo: evento.titulo,
          descricao: evento.descricao ?? '',
          local: evento.local ?? '',
          data_hora_inicio: paraInputDateTime(evento.data_hora_inicio),
          data_hora_fim: paraInputDateTime(evento.data_hora_fim),
          pontos: evento.pontos,
          capacidade: evento.capacidade == null ? '' : String(evento.capacidade),
        }
      : { pontos: 0, capacidade: '' },
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

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <FormField label="Pontos" type="number" min={0} erro={errors.pontos?.message} registro={register('pontos')} />
          <FormField
            label="Vagas"
            type="number"
            min={1}
            placeholder="Sem limite"
            erro={errors.capacidade?.message}
            registro={register('capacidade')}
          />
        </div>
        <p className="-mt-2 text-xs text-text-muted">
          Deixe as vagas em branco para não limitar as inscrições. Quem cancela devolve a vaga para os outros
          alunos.
          {evento && evento.inscritos > 0 && (
            <> Este evento já tem <span className="font-medium text-text">{evento.inscritos} inscrito(s)</span>,
            então a capacidade não pode ser menor que isso.</>
          )}
        </p>

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
