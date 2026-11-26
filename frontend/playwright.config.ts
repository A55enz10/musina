import { defineConfig } from '@playwright/test';

export default defineConfig({
  testDir: './e2e',
  use: { baseURL: process.env.MUSINA_E2E_URL ?? 'http://127.0.0.1:8080', browserName: 'chromium' },
  workers: 1,
  reporter: [['list'], ['html', { open: 'never' }]],
  // No traces/screenshots: credentials remain outside test artifacts.
});
