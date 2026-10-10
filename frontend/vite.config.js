import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 后端地址：默认 http://localhost:8080，可用环境变量覆盖，例如
//   VITE_API_TARGET=http://localhost:8081 pnpm dev
const target = process.env.VITE_API_TARGET || 'http://localhost:8080'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 5173,
    // /api 与 /uploads 都转发到后端，前端与后端同源，Session Cookie 天然携带，不需要处理跨域
    proxy: {
      '/api': { target, changeOrigin: true },
      '/uploads': { target, changeOrigin: true }
    }
  }
})