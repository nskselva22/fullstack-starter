import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// During local dev, proxy /api and /oauth2 /login to the Spring Boot backend.
// This keeps cookies first-party so Spring Security sessions work cleanly.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api':     { target: 'http://localhost:8080', changeOrigin: true },
      '/oauth2':  { target: 'http://localhost:8080', changeOrigin: true },
      '/login':   { target: 'http://localhost:8080', changeOrigin: true },
    },
  },
  build: {
    outDir: 'dist',
    sourcemap: true,
  },
});
