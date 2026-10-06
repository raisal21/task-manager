/** The one date helper. The API sends ISO-8601 UTC times. The UI shows local time (A9). */
export function formatDateTime(iso: string): string {
  return new Date(iso).toLocaleString();
}
