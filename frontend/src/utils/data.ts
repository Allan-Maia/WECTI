/** Formata um LocalDateTime do backend ("2026-08-20T19:00:00") para pt-BR. */
export function formatarDataHora(iso: string | null | undefined): string {
  if (!iso) return '-';
  const data = new Date(iso);
  if (Number.isNaN(data.getTime())) return '-';
  const dia = String(data.getDate()).padStart(2, '0');
  const mes = String(data.getMonth() + 1).padStart(2, '0');
  const ano = data.getFullYear();
  const hora = String(data.getHours()).padStart(2, '0');
  const minuto = String(data.getMinutes()).padStart(2, '0');
  return `${dia}/${mes}/${ano} ${hora}:${minuto}`;
}

/** Mesma coisa, so a data (sem hora) - usado pra Periodo.data_inicio/data_fim.
 *  Não usa `new Date(string)` de propósito: uma string "yyyy-MM-dd" (sem
 *  horário) é interpretada como UTC-meia-noite pelo JS, e exibida depois
 *  no fuso local - em fusos negativos (Brasil, UTC-3) isso mostra sempre
 *  um dia a menos. Faz o parse manual pra evitar essa armadilha. */
export function formatarData(iso: string | null | undefined): string {
  if (!iso) return '-';
  const partes = iso.split('-');
  if (partes.length !== 3) return '-';
  const [ano, mes, dia] = partes;
  return `${dia}/${mes}/${ano}`;
}

/** Converte um LocalDateTime do backend pro formato aceito por
 *  <input type="datetime-local"> (yyyy-MM-ddTHH:mm). */
export function paraInputDateTime(iso: string | null | undefined): string {
  if (!iso) return '';
  return iso.slice(0, 16);
}
