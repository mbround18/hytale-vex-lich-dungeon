import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import path from "node:path";

const __dirname = path.dirname(new URL(import.meta.url).pathname);

export default defineConfig({
  plugins: [react()],
  build: {
    outDir: path.resolve(__dirname, "../serve-cli/dist"),
    emptyOutDir: false,
  },
});
