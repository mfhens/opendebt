import { defineConfig, devices } from '@playwright/test';

const isCi = !!process.env.CI;

/**
 * CI runs all tests except those tagged @backlog in the test title.
 * Use test('… @backlog', …) for RED or unfinished E2E so main/develop stays green.
 */
export default defineConfig({
  testDir: './tests',
  fullyParallel: true,
  forbidOnly: isCi,
  retries: isCi ? 2 : 0,
  workers: isCi ? 1 : undefined,
  reporter: [['html', { open: 'never' }]],
  grepInvert: isCi ? /@backlog/ : undefined,
  use: {
    trace: 'on-first-retry',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
});
