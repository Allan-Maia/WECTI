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

/** Mesma coisa, so a data (sem hora) - usado pra Periodo.data_inicio/data_fim. */
export function formatarData(iso: string | null | undefined): string {
  if (!iso) return '-';
  const data = new Date(iso);
  if (Number.isNaN(data.getTime())) return '-';
  const dia = String(data.getDate()).padStart(2, '0');
  const mes = String(data.getMonth() + 1).padStart(2, '0');
  const ano = data.getFullYear();
  return `${dia}/${mes}/${ano}`;
}

/** Converte um LocalDateTime do backend pro formato aceito por
 *  <input type="datetime-local"> (yyyy-MM-ddTHH:mm). */
export function paraInputDateTime(iso: string | null | undefined): string {
  if (!iso) return '';
  return iso.slice(0, 16);
}
