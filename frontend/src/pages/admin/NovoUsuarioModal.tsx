import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import Button from '../../components/Button';
import FormField from '../../components/FormField';
import type { NovoUsuario } from '../../types';

const schema = z
  .object({
    nome: z.string().min(1, 'Informe o nome'),
    email: z.string().min(1, 'Informe o email').email('Email inválido'),
    perfil: z.enum(['ADMIN', 'PROFESSOR', 'ALUNO']),
    rgm: z.string().optional(),
  })
  .refine((dados) => dados.perfil !== 'ALUNO' || /^\d{8}$/.test(dados.rgm ?? ''), {
    message: 'RGM obrigatório (8 dígitos) para perfil Aluno',
    path: ['rgm'],
  });

type FormValues = z.infer<typeof schema>;

interface Props {
  onSalvar: (dados: NovoUsuario) => Promise<void>;
  onFechar: () => void;
}

export default function NovoUsuarioModal({ onSalvar, onFechar }: Props) {
  const [salvando, setSalvando] = useState(false);
  const [erroGeral, setErroGeral] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    watch,
    formState: { errors },
  } = useForm<FormValues>({ resolver: zodResolver(schema), defaultValues: { perfil: 'ALUNO' } });

  const perfil = watch('perfil');

  const onSubmit = async (dados: FormValues) => {
    setErroGeral(null);
    setSalvando(true);
    try {
      await onSalvar(dados);
    } catch (e) {
      setErroGeral(e instanceof Error ? e.message : 'Não foi possível criar o usuário.');
    } finally {
      setSalvando(false);
    }
  };

  return (
    <div className="fixed inset-0 z-[200] flex items-center justify-center bg-black/80 p-4" onClick={onFechar}>
      <form
        onSubmit={handleSubmit(onSubmit)}
        onClick={(e) => e.stopPropagation()}
        className="flex w-full max-w-md flex-col gap-4 rounded-card border border-border bg-surface p-6"
      >
        <h2 className="text-lg font-bold text-text">Novo usuário</h2>

        <FormField label="Nome" erro={errors.nome?.message} registro={register('nome')} />
        <FormField label="Email" type="email" erro={errors.email?.message} registro={register('email')} />

        <label className="flex flex-col gap-1.5 text-sm">
          <span className="font-medium text-text">Perfil</span>
          <select
            className="rounded-lg border border-border bg-surface px-3.5 py-2.5 text-text outline-none focus:border-accent"
            {...register('perfil')}
          >
            <option value="ALUNO">Aluno</option>
            <option value="PROFESSOR">Professor</option>
            <option value="ADMIN">Administrador</option>
          </select>
        </label>

        {perfil === 'ALUNO' && (
          <FormField label="RGM (8 dígitos)" maxLength={8} erro={errors.rgm?.message} registro={register('rgm')} />
        )}

        {erroGeral && (
          <p className="rounded-lg border border-red-500/30 bg-red-500/5 px-3 py-2 text-sm text-red-400">{erroGeral}</p>
        )}

        <div className="mt-2 flex justify-end gap-3">
          <Button type="button" variante="outline" onClick={onFechar}>
            Cancelar
          </Button>
          <Button type="submit" carregando={salvando}>
            Criar
          </Button>
        </div>
      </form>
    </div>
  );
}
