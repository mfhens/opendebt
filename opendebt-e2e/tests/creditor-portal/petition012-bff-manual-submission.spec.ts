import { test } from '@playwright/test';
import { authenticateCreditorPortalDemo } from '../helpers/creditor-portal-auth';

/**
 * =============================================================================
 * TEMPLATE — portal E2E (ADR 0034 + Gas City playwright-test-generator)
 * =============================================================================
 *
 * 1. One file per petition under tests/<portal>/petition<NNN>-*.spec.ts
 * 2. Map each Gherkin scenario to one test(); keep titles traceable to the feature file
 * 3. Put @backlog in every title until the scenario is GREEN — CI omits /@backlog/ (playwright.config.ts)
 * 4. RED body: throw new Error('Not implemented: petition<NNN> — "<scenario title>"')
 * 5. When implementing: remove @backlog, replace throw with real navigation + expect(), reuse auth helpers
 *
 * This file (petition012) is intentionally RED-only so new portal work can copy the structure.
 *
 * Petition  : petition012
 * Feature   : petitions/petition012-fordringshaverportal-bff-and-manual-submission.feature
 * Spec      : petitions/specs/petition012-specs.yaml
 * Contract  : petitions/petition012-fordringshaverportal-bff-and-manual-submission-outcome-contract.md
 */

const CREDITOR = 'http://localhost:8085/creditor-portal';
const PETITION = 'petition012';

function notImplemented(scenarioTitle: string): never {
  throw new Error(`Not implemented: ${PETITION} — "${scenarioTitle}"`);
}

test.describe('petition012 creditor portal BFF and manual submission', () => {
  test.beforeEach(async ({ page }) => {
    await authenticateCreditorPortalDemo(page);
  });

  test('FR-P012-01 — portal reads creditor profile from backend service @backlog', async ({
    page,
  }) => {
    await page.goto(`${CREDITOR}/`, { waitUntil: 'domcontentloaded' });
    // Gherkin: Given portal user "U1" is bound to fordringshaver "K1"
    // When user "U1" opens the fordringshaver portal
    // Then the portal reads creditor profile data from the creditor master data service
    notImplemented('The portal reads creditor profile from the backend service');
  });

  test('FR-P012-02 — manual fordring submitted to debt-service; portal not system of record @backlog', async ({
    page,
  }) => {
    await page.goto(`${CREDITOR}/`, { waitUntil: 'domcontentloaded' });
    // Gherkin: Given portal user "U2" is allowed to create fordringer for fordringshaver "K2"
    // When user "U2" submits a manual fordring in the portal
    // Then the portal sends the request to debt-service
    // And the portal does not persist the fordring as its own domain data
    notImplemented('Manual fordring creation is submitted to debt-service');
  });

  test('FR-P012-03 — user cannot act for unrelated fordringshaver @backlog', async ({ page }) => {
    await page.goto(`${CREDITOR}/`, { waitUntil: 'domcontentloaded' });
    // Gherkin: Given portal user "U3" is bound to fordringshaver "K3"
    // And fordringshaver "K3" may not act on behalf of fordringshaver "K4"
    // When user "U3" attempts to act for fordringshaver "K4"
    // Then the request is rejected
    notImplemented('A portal user cannot act for an unrelated fordringshaver');
  });
});
