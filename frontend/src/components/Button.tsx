import type { ButtonHTMLAttributes } from 'react';
import LoadingSpinner from './LoadingSpinner';

interface Props extends ButtonHTMLAttributes<HTMLButtonElement> {
  variante?: 'primary' | 'outline' | 'danger';
  carregando?: boolean;
}

const VARIANTES: Record<NonNullable<Props['variante']>, string> = {
  primary:
    'bg-accent text-[#04211c] hover:brightness-110 disabled:hover:brightness-100 shadow-[0_0_0_0_rgba(0,232,196,0)] hover:shadow-[0_0_24px_-4px_rgba(0,232,196,0.6)]',
  outline: 'border border-accent text-accent bg-transparent hover:bg-accent/10',
  danger: 'border border-red-500/50 text-red-400 bg-transparent hover:bg-red-500/10',
};

export default function Button({
  variante = 'primary',
  carregando = false,
  disabled,
  className = '',
  children,
  ...rest
}: Props) {
  return (
    <button
      disabled={disabled || carregando}
      className={`inline-flex items-center justify-center gap-2 rounded-full px-5 py-2.5 text-sm font-semibold transition duration-200 disabled:cursor-not-allowed disabled:opacity-50 ${VARIANTES[variante]} ${className}`}
      {...rest}
    >
      {carregando && <LoadingSpinner tamanho="sm" />}
      {children}
    </button>
  );
}
