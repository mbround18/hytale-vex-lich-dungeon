import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'node:path';

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@shared': path.resolve(__dirname, '../ui-shared/src')
    }
  },
  build: {
    outDir: path.resolve(__dirname, '../../plugins/roguelike/src/main/resources/Debug'),
    emptyOutDir: false
  },
  server: {
    port: 3391,
    proxy: {
      '/api': 'http://127.0.0.1:3390'
    }
  }
});
