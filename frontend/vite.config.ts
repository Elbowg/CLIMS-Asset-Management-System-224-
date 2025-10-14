import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  const target = env.VITE_API_BASE || 'http://localhost:8080';
  return {
    plugins: [react()],
    server: {
      port: 5173,
      proxy: {
        // Proxy API calls during dev to backend
        '/api': {
          target,
          changeOrigin: true,
          secure: false
        }
      }
    }
  };
});
