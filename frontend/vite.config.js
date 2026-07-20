import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Vite config
// proxy: all /api requests in dev are forwarded to Spring Boot on 8080
// This avoids CORS issues during development
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8089',
        changeOrigin: true,
      }
    }
  }
})
