import { test, expect } from '@playwright/test';
import { authenticateCreditorPortalDemo } from './helpers/creditor-portal-auth';

/**
 * Creditor portal feature shells (petitions 030–038, Phase 9)
 *
 * Specs    : petitions/specs/petition030-specs.yaml … petition038-specs.yaml
 * Auth     : Keycloak + demo creditor — tests/helpers/creditor-portal-auth.ts
 */

const CREDITOR = 'http://localhost:8085/creditor-portal';
/** Stable UUID unlikely to exist in dev seed data — triggers not-found path on detail view. */
const UNKNOWN_CLAIM_ID = 'ffffffff-ffff-4fff-bfff-ffffffffffff';

const OAUTH_GATED_PATHS = [
  '/fordringer/hoering',
  '/fordringer/afviste',
  '/underretninger',
  '/afstemning',
  '/rapporter',
  '/indstillinger',
  `/fordring/${UNKNOWN_CLAIM_ID}`,
  '/fordring/opret/step/1',
];

test.describe('petition030-038 creditor portal surfaces', () => {
  for (const path of OAUTH_GATED_PATHS) {
    test(`Unauthenticated ${path} yields redirect toward OAuth`, async ({ request }) => {
      const response = await request.get(`${CREDITOR}${path}`, { maxRedirects: 0 });
      expect(response.status(), 'expect redirect when not logged in').toBe(302);
      const location = response.headers().location ?? '';
      expect(location, 'OAuth or demo-login hop').toMatch(/oauth2|authorization|demo-login/i);
    });
  }

  test.describe('authenticated via Keycloak + demo creditor', () => {
    test.beforeEach(async ({ page }) => {
      await authenticateCreditorPortalDemo(page);
    });

    test('petition030 — claim detail shows error when claim is missing', async ({ page }) => {
      await page.goto(`${CREDITOR}/fordring/${UNKNOWN_CLAIM_ID}`, { waitUntil: 'domcontentloaded' });
      await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
      await expect(page.locator('.skat-alert--error[role="alert"]')).toBeVisible();
      await expect(page.locator('main#main-content[role="main"]')).toBeVisible();
    });

    test('petition031 — hearing list shell and 11-column HTMX table', async ({ page }) => {
      await page.goto(`${CREDITOR}/fordringer/hoering`, { waitUntil: 'domcontentloaded' });
      await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
      await expect(page.locator('#hearing-search-form')).toBeVisible();
      await expect(page.locator('#hearing-table-container')).toBeVisible();
      await expect(page.locator('table.skat-table thead th[scope="col"]')).toHaveCount(11, {
        timeout: 45_000,
      });
    });

    test('petition032 — rejected claims list shell and 10-column HTMX table', async ({ page }) => {
      await page.goto(`${CREDITOR}/fordringer/afviste`, { waitUntil: 'domcontentloaded' });
      await expect(page).toHaveURL(/afviste/);
      await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
      await expect(page.locator('#claims-search-form')).toBeVisible();
      await expect(page.locator('#claims-table-container')).toBeVisible();
      await expect(page.locator('table.skat-table thead th[scope="col"]')).toHaveCount(10, {
        timeout: 45_000,
      });
    });

    test('petition033 — claim wizard step 1 or redirect when creation is disabled', async ({
      page,
    }) => {
      await page.goto(`${CREDITOR}/fordring/opret/step/1`, { waitUntil: 'domcontentloaded' });
      const url = page.url();
      if (!url.includes('/fordring/opret/step/1')) {
        test.skip(true, 'Claim creation wizard not enabled for seeded creditor agreement');
      }
      await expect(page.locator('.skat-step-indicator')).toBeVisible();
      await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
    });

    test('petition034 — adjustment page shell (permission gate or form)', async ({ page }) => {
      await page.goto(`${CREDITOR}/fordring/${UNKNOWN_CLAIM_ID}/adjustment`, {
        waitUntil: 'domcontentloaded',
      });
      await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
      const errorAlert = page.locator('.skat-alert--error[role="alert"]');
      const postForm = page.locator('form.skat-form[method="post"]');
      await expect(errorAlert.or(postForm).first()).toBeVisible({ timeout: 30_000 });
    });

    test('petition035 — notifications search page shell', async ({ page }) => {
      await page.goto(`${CREDITOR}/underretninger`, { waitUntil: 'domcontentloaded' });
      await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
      await expect(page.locator('form[action*="/underretninger/search"]')).toBeVisible();
      await expect(page.locator('#notification-results')).toBeVisible();
    });

    test('petition036 — reconciliation list shell', async ({ page }) => {
      await page.goto(`${CREDITOR}/afstemning`, { waitUntil: 'domcontentloaded' });
      await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
      await expect(page.locator('#reconciliation-filter-form')).toBeVisible();
    });

    test('petition037 — reports page year/month selector and report list region', async ({
      page,
    }) => {
      await page.goto(`${CREDITOR}/rapporter`, { waitUntil: 'domcontentloaded' });
      await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
      await expect(page.locator('#year-select')).toBeVisible();
      await expect(page.locator('#month-select')).toBeVisible();
      await expect(page.locator('#report-list-container')).toBeVisible();
      const empty = page.locator('#report-list-container .skat-empty-state');
      const tableHeaders = page.locator('#report-list-container table.skat-table thead th[scope="col"]');
      await expect(empty.or(tableHeaders.first())).toBeVisible({ timeout: 30_000 });
    });

    test('petition038 — dashboard claim counts and settings page', async ({ page }) => {
      await page.goto(`${CREDITOR}/`, { waitUntil: 'domcontentloaded' });
      await expect(page.locator('#claim-counts-container')).toBeVisible();
      await expect(page.locator('#claim-counts .skat-card--count')).toHaveCount(4, {
        timeout: 45_000,
      });

      await page.goto(`${CREDITOR}/indstillinger`, { waitUntil: 'domcontentloaded' });
      await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
      await expect(page.locator('main#main-content[role="main"]')).toBeVisible();
    });
  });
});
