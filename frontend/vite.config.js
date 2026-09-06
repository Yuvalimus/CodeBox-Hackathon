import { defineConfig } from 'vite';

export default defineConfig(({ mode }) => {
  return { server: { proxy: { '/api': { target: 'https://study.happyxd.dev', changeOrigin: true } } } };
});
