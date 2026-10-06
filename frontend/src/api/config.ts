// The only place that reads the base URL of the backend (R26). Vite sets it at build time.
const DEFAULT_API_BASE_URL = "http://localhost:8080";

export const API_BASE_URL: string = (
  import.meta.env.VITE_API_BASE_URL || DEFAULT_API_BASE_URL
).replace(/\/+$/, "");
