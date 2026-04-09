import type { Page } from '@playwright/test';

const CREDITOR_BASE = 'http://localhost:8085/creditor-portal';

/** SKAT demo creditor org UUID from creditor-service seed (allows portal claim creation). */
export const SKAT_DEMO_CREDITOR_ORG_ID = '00000000-0000-0000-0000-000000000001';

/**
 * Full browser login: Keycloak OAuth (realm user) + demo creditor picker.
 * Requires /etc/hosts mapping: 127.0.0.1 keycloak (see CI workflow).
 *
 * @param creditorOrgId optional `creditorOrgId` value from the demo-login dropdown (e.g. {@link SKAT_DEMO_CREDITOR_ORG_ID}); if omitted, the first active creditor is used.
 */
export async function authenticateCreditorPortalDemo(
  page: Page,
  creditorOrgId?: string,
): Promise<void> {
  const user = process.env.E2E_CREDITOR_USERNAME ?? 'creditor';
  const password = process.env.E2E_CREDITOR_PASSWORD ?? 'creditor123';

  await page.goto(`${CREDITOR_BASE}/demo-login`, { waitUntil: 'domcontentloaded', timeout: 60_000 });

  const url = page.url();
  if (url.includes('/realms/') && url.includes('openid-connect')) {
    await page.locator('input[name="username"]').fill(user);
    await page.locator('input[name="password"]').fill(password);
    const kcSubmit = page.locator(
      '#kc-form-login input[type="submit"], #kc-form-login button[type="submit"], input#kc-login',
    );
    if ((await kcSubmit.count()) > 0) {
      await kcSubmit.first().click();
    } else {
      await page.getByRole('button', { name: /sign in|log in|log ind/i }).click();
    }
    await page.waitForURL(/localhost:8085\/creditor-portal/, { timeout: 60_000 });
  }

  await page.waitForSelector('select#creditorOrgId', { timeout: 30_000 });
  let value = creditorOrgId?.trim();
  if (!value) {
    const option = page.locator('#creditorOrgId option[value]:not([value=""])').first();
    await option.waitFor({ state: 'attached', timeout: 30_000 });
    value = (await option.getAttribute('value')) ?? '';
  } else {
    const match = page.locator(`#creditorOrgId option[value="${value}"]`);
    await match.first().waitFor({ state: 'attached', timeout: 30_000 });
  }
  if (!value) {
    throw new Error('No creditor in demo-login dropdown — creditor-service returned no active creditors');
  }
  await page.selectOption('#creditorOrgId', value);
  await page.locator('form.skat-card button[type="submit"]').click();
  await page.waitForURL(/localhost:8085\/creditor-portal\/?$/, { timeout: 30_000 });
}
