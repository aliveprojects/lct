import { defineConfig } from 'vitest/config';

export default defineConfig({
  base: './',
  build: {
    target: 'chrome84',
    cssTarget: 'chrome84',
    outDir: 'dist',
    sourcemap: false,
  },
  test: {
    environment: 'node',
    include: ['tests/**/*.test.ts'],
  },
});
