import { test, expect } from '@playwright/test';
import { authenticateCreditorPortalDemo, SKAT_DEMO_CREDITOR_ORG_ID } from '../helpers/creditor-portal-auth';

/**
 * Petition012 — creditor portal BFF and manual submission (browser acceptance)
 *
 * Petition  : petition012
 * Feature   : petitions/petition012-fordringshaverportal-bff-and-manual-submission.feature
 * Spec      : petitions/specs/petition012-specs.yaml
 * Contract  : petitions/petition012-fordringshaverportal-bff-and-manual-submission-outcome-contract.md
 *
 * New portal petitions: copy this file’s structure; use @backlog in titles until GREEN.
 */

const CREDITOR = 'http://localhost:8085/creditor-portal';
/** Seeded municipal org (not the acting session) for act-as denial checks. */
const OTHER_CREDITOR_ORG_ID = 'c0010000-0000-0000-0000-000000000002';

/** Demo natural person — matches `DemoPersonSeeder` when person-registry runs with demo seed. */
const DEMO_CPR_DEBTOR = {
  cpr: '0503581234',
  firstName: 'Lars',
  lastName: 'Andersen',
} as const;

/** YYYY-MM-DD in local calendar (avoids UTC shift from `toISOString()` on non-UTC hosts). */
function formatLocalYmd(d: Date): string {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}

function localDateDaysAgo(days: number): string {
  const d = new Date();
  d.setDate(d.getDate() - days);
  return formatLocalYmd(d);
}

test.describe('petition012 creditor portal BFF and manual submission', () => {
  test('FR-P012-01 — portal reads creditor profile from backend service', async ({ page }) => {
    await authenticateCreditorPortalDemo(page);
    await page.goto(`${CREDITOR}/`, { waitUntil: 'domcontentloaded' });

    const main = page.locator('main#main-content');
    await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
    await expect(
      main.getByRole('heading', { level: 2, name: /Fordringshaverprofil|Creditor profile/i }),
    ).toBeVisible();

    const profileTable = main.locator('table.skat-table').first();
    await expect(profileTable).toBeVisible();
    await expect(profileTable.getByRole('row', { name: /Eksternt ID|External ID/i })).toBeVisible();
    await expect(profileTable.getByRole('row', { name: /Status/i })).toBeVisible();
    await expect(
      profileTable.getByRole('row', { name: /Forbindelsestype|Connection type/i }),
    ).toBeVisible();
    await expect(profileTable.locator('.skat-badge').first()).toBeVisible();
  });

  test('FR-P012-02 — manual claim wizard posts through BFF (portal not system of record)', async ({
    page,
  }) => {
    await authenticateCreditorPortalDemo(page, SKAT_DEMO_CREDITOR_ORG_ID);
    await page.goto(`${CREDITOR}/fordring/opret/step/1`, { waitUntil: 'domcontentloaded' });

    if (!page.url().includes('/fordring/opret/step/1')) {
      test.skip(true, 'Claim creation not enabled for selected creditor agreement');
    }

    await expect(page.locator('.skat-step-indicator')).toBeVisible();
    await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
    const form = page.locator('form[method="post"][action*="/fordring/opret/step/1"]');
    await expect(form).toBeVisible();
    await expect(form).toHaveAttribute('action', /fordring\/opret\/step\/1/i);
  });

  /**
   * Full happy path: wizard step 1 → person-registry CPR check → step 2 → review → POST to debt-service
   * `/debts/submit` via BFF, then receipt (UDFOERT). Portal-side claim persistence is still an architectural
   * invariant (no claim entities in creditor-portal), not something this browser test can prove alone.
   */
  test('FR-P012-02b — manual claim wizard completes submit and shows debt-service receipt', async ({
    page,
  }) => {
    await authenticateCreditorPortalDemo(page, SKAT_DEMO_CREDITOR_ORG_ID);
    await page.goto(`${CREDITOR}/fordring/opret/step/1`, { waitUntil: 'domcontentloaded' });

    if (!page.url().includes('/fordring/opret/step/1')) {
      test.skip(true, 'Claim creation not enabled for selected creditor agreement');
    }

    await page.selectOption('#debtorType', 'CPR');
    await page.locator('#debtorIdentifier').fill(DEMO_CPR_DEBTOR.cpr);
    await page.locator('#debtorFirstName').fill(DEMO_CPR_DEBTOR.firstName);
    await page.locator('#debtorLastName').fill(DEMO_CPR_DEBTOR.lastName);
    await page.getByRole('button', { name: /Next|Næste/i }).click();
    await page.waitForURL(/fordring\/opret\/step\/2/, { timeout: 60_000 });

    await page.selectOption('#claimType', 'SKAT');
    await page.locator('#amount').fill('1250.50');
    await page.locator('#principalAmount').fill('1000.00');
    await page.locator('#creditorReference').fill(`E2E-P012-${Date.now()}`);
    const dueYmd = localDateDaysAgo(30);
    await page.locator('#dueDate').fill(dueYmd);
    await expect(page.locator('#dueDate')).toHaveValue(dueYmd);
    // Fixed calendar date avoids CI timezone / Date#setFullYear edge cases vs debt-service validation
    await page.locator('#limitationDate').fill('2035-12-31');
    await expect(page.locator('#limitationDate')).toHaveValue('2035-12-31');
    await page.selectOption('#estateProcessing', 'false');
    await page.getByRole('button', { name: /Next|Næste/i }).click();
    await page.waitForURL(/fordring\/opret\/step\/3/, { timeout: 60_000 });

    page.once('dialog', (d) => d.accept());
    await page.getByRole('button', { name: /Submit claim|Indsend fordring/i }).click();
    await page.waitForURL(/fordring\/opret\/step\/4/, { timeout: 120_000 });

    const accepted = page.getByTestId('wizard-result-accepted');
    const rejected = page.getByTestId('wizard-result-rejected');
    await expect(accepted.or(rejected)).toBeVisible({ timeout: 60_000 });
    if (await rejected.isVisible()) {
      const errors = await page.locator('.skat-error-list').innerText().catch(() => '(no error list)');
      throw new Error(`E2E expected UDFOERT but portal showed AFVIST. Validation: ${errors}`);
    }
    await expect(accepted).toBeVisible();
    await expect(
      page.getByRole('heading', { name: /Claim accepted|Fordring accepteret/i }),
    ).toBeVisible();
    const claimIdDd = page
      .locator('dl.skat-description-list dt')
      .filter({ hasText: /Claim ID|Fordrings-ID/i });
    await expect(claimIdDd).toBeVisible();
    const row = claimIdDd.locator('xpath=following-sibling::dd[1]');
    await expect(row).toHaveText(
      /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i,
    );
  });

  test('FR-P012-03 — act-as for unrelated creditor shows rejection on dashboard', async ({ page }) => {
    // SKAT demo org is never the municipal seed UUID below (first dropdown option can be that kommune).
    await authenticateCreditorPortalDemo(page, SKAT_DEMO_CREDITOR_ORG_ID);
    await page.goto(`${CREDITOR}/?actAs=${OTHER_CREDITOR_ORG_ID}`, { waitUntil: 'domcontentloaded' });

    const denial = page.locator('.skat-alert--error[role="alert"]').first();
    await expect(denial).toBeVisible();
    await expect(denial).toContainText(/.+/);
  });
});
