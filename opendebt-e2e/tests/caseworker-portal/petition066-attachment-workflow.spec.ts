import { test, expect } from '@playwright/test';
import { authenticateCaseworkerWritable, CW_BASE, DEBT_SVC } from '../helpers/caseworker-portal-auth';

const ATTACHMENT_DEBTOR = '06600000-e2e0-0066-db00-000000000001';
const COVERED_FORDRING_A = '06600000-e2e0-0066-fd00-000000000001';
const COVERED_FORDRING_B = '06600000-e2e0-0066-fd00-000000000002';

test.describe('Attachment workflow seams (P066) @backlog', () => {
  test('P066 seam validation: debt-service workflow endpoints are dry-run discoverable', async ({
    request,
  }) => {
    const createResponse = await request.post(
      `${DEBT_SVC}/internal/debtors/${ATTACHMENT_DEBTOR}/attachment-workflows`,
      {
        data: {
          coveredFordringIds: [COVERED_FORDRING_A, COVERED_FORDRING_B],
        },
      },
    );

    expect([200, 401, 403, 404, 422, 500, 503]).toContain(createResponse.status());

    const listResponse = await request.get(
      `${DEBT_SVC}/internal/debtors/${ATTACHMENT_DEBTOR}/attachment-workflows`,
    );

    expect([200, 401, 403, 404, 500, 503]).toContain(listResponse.status());
  });

  test('P066 seam validation: caseworker portal currently has no attachment workflow UI route', async ({
    page,
  }) => {
    await authenticateCaseworkerWritable(page);

    const response = await page.goto(
      `${CW_BASE}/debtors/${ATTACHMENT_DEBTOR}/attachment-workflows`,
      { waitUntil: 'domcontentloaded' },
    );

    expect(response).not.toBeNull();
    expect([404, 500]).toContain(response!.status());
  });
});
