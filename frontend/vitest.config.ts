import { defineConfig } from 'vitest/config';

export default defineConfig({
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: './src/setupTests.ts',
    // only run tests from our source tree; explicitly exclude node_modules and Playwright e2e
    include: [
      'src/**/*.test.{ts,tsx,js,jsx}',
      'src/**/*.spec.{ts,tsx,js,jsx}',
      'src/__tests__/**/*.{ts,tsx,js,jsx}'
    ],
    exclude: ['node_modules/**', 'dist/**', 'src/__tests__/e2e/**']
  }
});
