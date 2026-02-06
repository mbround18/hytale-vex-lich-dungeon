import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'node:path';

const __dirname = path.dirname(new URL(import.meta.url).pathname);

const pagesFallback = () => {
  const rewrite = (req: any, _res: any, next: any) => {
    const rawUrl = req.url || '/';
    const url = rawUrl.split('?')[0];
    const hasExtension = url.includes('.') && !url.endsWith('/');
    if (hasExtension) {
      next();
      return;
    }
    if (url.startsWith('/dev/hosting/')) {
      req.url = '/dev/hosting/index.html';
      next();
      return;
    }
    if (url.startsWith('/dev/ui/')) {
      req.url = '/dev/ui/index.html';
      next();
      return;
    }
    if (url.startsWith('/dev/')) {
      req.url = '/dev/index.html';
      next();
      return;
    }
    if (url === '/' || url.startsWith('/#')) {
      next();
      return;
    }
    req.url = '/index.html';
    next();
  };

  return {
    name: 'pages-fallback',
    configureServer(server: any) {
      server.middlewares.use(rewrite);
    },
    configurePreviewServer(server: any) {
      server.middlewares.use(rewrite);
    }
  };
};

export default defineConfig({
  plugins: [react(), pagesFallback()],
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
