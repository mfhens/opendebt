import { test, expect } from '@playwright/test';
import { authenticateCreditorPortalDemo } from './helpers/creditor-portal-auth';

/**
 * Creditor portal claims lists (petition029)
 *
 * Petition : petition029
 * Feature  : petitions/petition029-fordringshaverportal-fordringer-oversigt.feature
 * Spec     : petitions/specs/petition029-specs.yaml
 * Contract : petitions/petition029-fordringshaverportal-fordringer-oversigt-outcome-contract.md
 */

const CREDITOR = 'http://localhost:8085/creditor-portal';

test.describe('petition029 creditor claims lists', () => {
  test('Unauthenticated user cannot access claims recovery list (redirect to OAuth)', async ({ request }) => {
    const response = await request.get(`${CREDITOR}/fordringer`, { maxRedirects: 0 });
    expect(response.status(), 'expect redirect when not logged in').toBe(302);
    const location = response.headers().location ?? '';
    expect(location, 'OAuth authorization redirect').toMatch(/oauth2|authorization/i);
  });

  test.describe('authenticated via Keycloak + demo creditor', () => {
    test.beforeEach(async ({ page }) => {
      await authenticateCreditorPortalDemo(page);
    });

    test('Recovery list page shell and HTMX-loaded table with 13 column headers', async ({ page }) => {
      await page.goto(`${CREDITOR}/fordringer`, { waitUntil: 'domcontentloaded' });
      await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
      await expect(page.locator('#claims-search-form')).toBeVisible();
      await expect(page.locator('#claims-table-container')).toBeVisible();
      await expect(page.locator('table.skat-table thead th[scope="col"]')).toHaveCount(13, { timeout: 45_000 });
      await expect(page.locator('table.skat-table caption')).toBeAttached();
    });

    test('Zero-balance list page shell loads', async ({ page }) => {
      await page.goto(`${CREDITOR}/fordringer/nulfordringer`, { waitUntil: 'domcontentloaded' });
      await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
      await expect(page.locator('#claims-table-container')).toBeVisible();
      await expect(page.locator('table.skat-table thead th[scope="col"]')).toHaveCount(13, { timeout: 45_000 });
    });

    test('Claims counts page renders', async ({ page }) => {
      await page.goto(`${CREDITOR}/fordringer/optaellinger`, { waitUntil: 'domcontentloaded' });
      await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
    });

    test('Claims list uses layout skip link and main landmark', async ({ page }) => {
      await page.goto(`${CREDITOR}/fordringer`, { waitUntil: 'domcontentloaded' });
      await expect(page.locator('a.skat-skip-link[href="#main-content"]')).toBeVisible();
      await expect(page.locator('main#main-content[role="main"]')).toBeVisible();
    });
  });
});
