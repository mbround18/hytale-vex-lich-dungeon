import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'node:path';

const __dirname = path.dirname(new URL(import.meta.url).pathname);

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@shared': path.resolve(__dirname, '../ui-shared/src')
    }
  },
  publicDir: path.resolve(__dirname, 'public'),
  build: {
    outDir: path.resolve(__dirname, '../../dist/static'),
    emptyOutDir: false
  }
});
