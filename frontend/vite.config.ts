import react from "@vitejs/plugin-react-swc";
import { defineConfig } from "vitest/config";

// strictPort: the backend allows the origin http://localhost:5173 by default (A15).
// A silent move to another port would look like a CORS fault.
export default defineConfig({
  plugins: [react()],
  server: { port: 5173, strictPort: true },
  // The component tests need a DOM. They never reach a real backend (see src/test/stubFetch.ts).
  test: { environment: "jsdom" },
});
