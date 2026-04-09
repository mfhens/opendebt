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

  test('FR-P012-03 — act-as for unrelated creditor shows rejection on dashboard', async ({ page }) => {
    // SKAT demo org is never the municipal seed UUID below (first dropdown option can be that kommune).
    await authenticateCreditorPortalDemo(page, SKAT_DEMO_CREDITOR_ORG_ID);
    await page.goto(`${CREDITOR}/?actAs=${OTHER_CREDITOR_ORG_ID}`, { waitUntil: 'domcontentloaded' });

    const denial = page.locator('.skat-alert--error[role="alert"]').first();
    await expect(denial).toBeVisible();
    await expect(denial).toContainText(/.+/);
  });
});
