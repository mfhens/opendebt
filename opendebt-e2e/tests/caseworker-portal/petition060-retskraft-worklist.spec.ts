import { test, expect, type APIRequestContext, type Page } from '@playwright/test';
import { authenticateCaseworkerWritable, CW_BASE, DEBT_SVC } from '../helpers/caseworker-portal-auth';

type WorklistResponse = {
  worklistId: string;
  debtorId: string;
};

const OVERRIDE_DEBTOR = '06000000-e2e0-0060-db00-000000000001';
const SURPLUS_DEBTOR = '06000000-e2e0-0060-db00-000000000002';
const MODREGNING_DEBTOR = '06000000-e2e0-0060-db00-000000000003';

function worklistPath(worklist: WorklistResponse): string {
  return `${CW_BASE}/debtors/${worklist.debtorId}/retskraft-worklists/${worklist.worklistId}`;
}

async function generateWorklist(
  request: APIRequestContext,
  debtorId: string,
  payload: Record<string, unknown>,
): Promise<WorklistResponse> {
  const response = await request.post(
    `${DEBT_SVC}/api/v1/debtors/${debtorId}/retskraft-worklists`,
    { data: payload },
  );
  expect(response.ok(), `generate worklist for ${debtorId}`).toBeTruthy();
  return (await response.json()) as WorklistResponse;
}

async function openWorklist(page: Page, worklist: WorklistResponse): Promise<void> {
  await page.goto(worklistPath(worklist));
  await expect(page.locator('[data-testid="section50-worklist"]')).toBeVisible({ timeout: 15_000 });
}

test.describe('Retskraft evaluation worklists (P060) @backlog', () => {
  test('VAL-P060-002 override reason, legal basis, and changed ordering are visible', async ({
    page,
    request,
  }) => {
    const worklist = await generateWorklist(request, OVERRIDE_DEBTOR, {
      contextType: 'DEFAULT',
      candidateClaimIds: ['C-06021', 'C-06022'],
      requestedBySystem: false,
    });

    await authenticateCaseworkerWritable(page);
    await openWorklist(page, worklist);

    await page.fill('#overrideReason', 'Urgent court deadline on C-06022');
    await page.fill('#legalBasis', 'Section 50 override');
    await page.fill('#selectedClaimOrder', 'C-06022\nC-06021');
    await Promise.all([
      page.waitForURL(worklistPath(worklist)),
      page.click('#section50-override-form button[type="submit"]'),
    ]);

    await expect(page.locator('[data-testid="section50-override"]')).toContainText(
      'Urgent court deadline on C-06022',
    );
    await expect(page.locator('[data-testid="section50-override"]')).toContainText(
      'Section 50 override',
    );
    const rows = page.locator('tr[data-section50-entry-row="true"]');
    await expect(rows.first()).toContainText('C-06022');
    await expect(rows.nth(1)).toContainText('C-06021');
  });

  test('VAL-P060-007 expedited surplus deviation is visible', async ({ page, request }) => {
    const worklist = await generateWorklist(request, SURPLUS_DEBTOR, {
      contextType: 'VOLUNTARY_PAYMENT_SURPLUS',
      availableAmount: 400,
      confirmedAmountCovered: 0,
      candidateClaimIds: ['C-06061', 'C-06062'],
      requestedBySystem: false,
    });

    await authenticateCaseworkerWritable(page);
    await openWorklist(page, worklist);

    await page.fill('#overrideReason', 'Normal ordering would delay same-day coverage');
    await page.fill('#legalBasis', 'Section 50 subsection 4 expedited');
    await page.check('#expedited');
    await Promise.all([
      page.waitForURL(worklistPath(worklist)),
      page.click('#section50-override-form button[type="submit"]'),
    ]);

    await expect(page.locator('[data-testid="section50-worklist"]')).toContainText('EXPEDITED_SURPLUS');
    await expect(page.locator('[data-testid="section50-modregning"]')).toContainText(
      'Normal ordering would delay same-day coverage',
    );
    await expect(page.locator('[data-testid="section50-modregning"]')).toContainText(
      'Quicker-to-apply claims were prioritised',
    );
    await expect(page.locator('[data-testid="section50-decision-snapshot"]')).toContainText(
      'SECTION_50_EXPEDITED_SURPLUS_PATH',
    );
    const rows = page.locator('tr[data-section50-entry-row="true"]');
    await expect(rows.first()).toContainText('C-06062');
  });

  test('VAL-P060-009 no-modregning decision and reason are visible', async ({ page, request }) => {
    const worklist = await generateWorklist(request, MODREGNING_DEBTOR, {
      contextType: 'MODREGNING',
      availableAmount: 1200,
      confirmedAmountCovered: 700,
      candidateClaimIds: ['C-06071', 'C-06072', 'A-06072'],
      requestedBySystem: false,
    });

    await authenticateCaseworkerWritable(page);
    await openWorklist(page, worklist);

    await page.selectOption('#modregningOutcome', 'NO_MODREGNING');
    await page.fill('#reason', 'Timing pressure before payout deadline');
    await Promise.all([
      page.waitForURL(worklistPath(worklist)),
      page.click('#section50-modregning-form button[type="submit"]'),
    ]);

    await expect(page.locator('[data-testid="section50-modregning"]')).toContainText(
      'NO_MODREGNING',
    );
    await expect(page.locator('[data-testid="section50-modregning"]')).toContainText(
      'Timing pressure before payout deadline',
    );
  });

  test('VAL-P060-010 audit path and technical identifiers are visible', async ({ page, request }) => {
    const worklist = await generateWorklist(request, OVERRIDE_DEBTOR, {
      contextType: 'DEFAULT',
      candidateClaimIds: ['C-06021', 'C-06022'],
      requestedBySystem: false,
    });

    await authenticateCaseworkerWritable(page);
    await openWorklist(page, worklist);

    await expect(page.locator('[data-testid="section50-decision-snapshot"]')).toContainText(
      'DEFAULT_SECTION_50_PATH',
    );
    await expect(page.locator('[data-testid="section50-decision-snapshot"]')).toContainText(
      'Section 50 default',
    );
    await expect(page.locator('[data-testid="section50-decision-snapshot"]')).toContainText('SYSTEM');
    await expect(page.locator('[data-testid="section50-entries-table"]')).toContainText('C-06021');
    await expect(page.locator('body')).not.toContainText('Anna Jensen');
    await expect(page.locator('body')).not.toContainText('Erik Sørensen');
  });
});
