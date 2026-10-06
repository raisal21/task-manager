import react from "@vitejs/plugin-react-swc";
import { defineConfig } from "vite";

// strictPort: the backend allows the origin http://localhost:5173 by default (A15).
// A silent move to another port would look like a CORS fault.
export default defineConfig({
  plugins: [react()],
  server: { port: 5173, strictPort: true },
});
