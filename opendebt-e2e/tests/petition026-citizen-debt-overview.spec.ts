import { randomUUID } from 'crypto';
import { test, expect, type APIRequestContext, type Browser, type Page } from '@playwright/test';
import {
  CITIZEN_BASE,
  SEEDED_CITIZENS,
  authenticateCitizenPortal,
  deleteCitizenPortalUser,
  loginCitizenIfPrompted,
  provisionCitizenPortalUser,
  type CitizenPortalUser,
} from './helpers/citizen-portal-auth';

/**
 * Citizen debt overview page (petition026)
 *
 * Petition : petition026
 * Feature  : petitions/petition026-citizen-debt-overview-page.feature
 * Spec     : petitions/specs/petition026-specs.yaml
 * Contract : petitions/validation/petition026/validation-contract.md
 *
 * Prerequisites:
 * - Full docker-compose stack running on localhost ports 8080, 8082, 8086, and 8090.
 * - `127.0.0.1 keycloak` mapped in the local hosts file, same as the CI workflow.
 * - Petition026 portal route and petition024/025 citizen auth flow wired end to end.
 */

const LANDING = `${CITIZEN_BASE}/`;
const DEBT_SVC = 'http://localhost:8082/debt-service';
const DEMO_CREDITOR_ORG_ID = '00000000-0000-0000-0000-000000000001';
const DEBT_SUMMARY_PATH = '/api/v1/citizen/debts';
const DEBT_OVERVIEW_PAGE_SIZE = 100;

type ScenarioResources = {
  citizens: CitizenPortalUser[];
  debtIds: string[];
};

type DebtSeedOptions = {
  debtTypeCode?: string;
  principalAmount?: number;
  interestAmount?: number;
  feesAmount?: number;
  dueDate?: string;
  lastPaymentDate?: string;
  description?: string;
  claimArt?: string;
};

type StubCitizenDebtItem = {
  debtId: string;
  debtTypeCode: string;
  debtTypeName: string;
  creditorDisplayName: string;
  principalAmount: number;
  outstandingAmount: number;
  interestAmount: number;
  feesAmount: number;
  dueDate: string;
  status: string;
  citizenStatus: string;
  interestAccrualState: 'ACTIVE' | 'PAUSED';
  statusReasonCode?: string;
  interestPauseReasonCode?: string;
  currentInterestRate?: number;
  writtenOffReasonCode?: string;
};

type WrittenOffFixture = {
  debt: StubCitizenDebtItem;
  visibleReason: RegExp;
};

const WRITTEN_OFF_REASON_FIXTURES: WrittenOffFixture[] = [
  {
    debt: {
      debtId: '00000000-0000-0000-0000-000000000601',
      debtTypeCode: 'RESTSKAT',
      debtTypeName: 'Restskat',
      creditorDisplayName: 'Skattestyrelsen',
      principalAmount: 1200,
      outstandingAmount: 0,
      interestAmount: 0,
      feesAmount: 0,
      dueDate: '2024-01-15',
      status: 'WRITTEN_OFF',
      citizenStatus: 'WRITTEN_OFF',
      interestAccrualState: 'ACTIVE',
      writtenOffReasonCode: 'LIMITATION_EXPIRED',
    },
    visibleReason: /limitation expired|forældelse/i,
  },
  {
    debt: {
      debtId: '00000000-0000-0000-0000-000000000602',
      debtTypeCode: 'MOMSGAELD',
      debtTypeName: 'Momsgæld',
      creditorDisplayName: 'Skattestyrelsen',
      principalAmount: 2500,
      outstandingAmount: 0,
      interestAmount: 0,
      feesAmount: 0,
      dueDate: '2024-02-15',
      status: 'WRITTEN_OFF',
      citizenStatus: 'WRITTEN_OFF',
      interestAccrualState: 'ACTIVE',
      writtenOffReasonCode: 'BANKRUPTCY',
    },
    visibleReason: /bankruptcy|konkurs/i,
  },
  {
    debt: {
      debtId: '00000000-0000-0000-0000-000000000603',
      debtTypeCode: 'UNDERHOLDSBIDRAG',
      debtTypeName: 'Underholdsbidrag',
      creditorDisplayName: 'Familieretshuset',
      principalAmount: 1800,
      outstandingAmount: 0,
      interestAmount: 0,
      feesAmount: 0,
      dueDate: '2024-03-15',
      status: 'WRITTEN_OFF',
      citizenStatus: 'WRITTEN_OFF',
      interestAccrualState: 'ACTIVE',
      writtenOffReasonCode: 'ESTATE_OF_DECEASED',
    },
    visibleReason: /estate of deceased|dødsbo/i,
  },
  {
    debt: {
      debtId: '00000000-0000-0000-0000-000000000604',
      debtTypeCode: 'SU_GAELD',
      debtTypeName: 'SU-gæld',
      creditorDisplayName: 'Uddannelses- og Forskningsstyrelsen',
      principalAmount: 4100,
      outstandingAmount: 0,
      interestAmount: 0,
      feesAmount: 0,
      dueDate: '2024-04-15',
      status: 'WRITTEN_OFF',
      citizenStatus: 'WRITTEN_OFF',
      interestAccrualState: 'ACTIVE',
      writtenOffReasonCode: 'DEBT_RESTRUCTURING',
    },
    visibleReason: /debt restructuring|gældssanering/i,
  },
  {
    debt: {
      debtId: '00000000-0000-0000-0000-000000000605',
      debtTypeCode: 'DAGBOEDE',
      debtTypeName: 'Dagbøde',
      creditorDisplayName: 'Politiet',
      principalAmount: 900,
      outstandingAmount: 0,
      interestAmount: 0,
      feesAmount: 0,
      dueDate: '2024-05-15',
      status: 'WRITTEN_OFF',
      citizenStatus: 'WRITTEN_OFF',
      interestAccrualState: 'ACTIVE',
      writtenOffReasonCode: 'RECOVERY_FUTILE',
    },
    visibleReason: /recovery .* futile|nyttesløst/i,
  },
  {
    debt: {
      debtId: '00000000-0000-0000-0000-000000000606',
      debtTypeCode: 'ERSTATNING',
      debtTypeName: 'Erstatning',
      creditorDisplayName: 'Ankestyrelsen',
      principalAmount: 600,
      outstandingAmount: 0,
      interestAmount: 0,
      feesAmount: 0,
      dueDate: '2024-06-15',
      status: 'WRITTEN_OFF',
      citizenStatus: 'WRITTEN_OFF',
      interestAccrualState: 'ACTIVE',
      writtenOffReasonCode: 'RECOVERY_COST_DISPROPORTIONATE',
    },
    visibleReason: /recovery costs .* disproportionate|omkostninger .* uforholdsmæssige/i,
  },
];

function createScenarioResources(): ScenarioResources {
  return {
    citizens: [],
    debtIds: [],
  };
}

function formatCurrency(amount: number, locale = 'da-DK'): string {
  return new Intl.NumberFormat(locale, {
    style: 'currency',
    currency: 'DKK',
  }).format(amount);
}

function formatYmd(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function offsetDate(daysFromToday: number): string {
  const date = new Date();
  date.setDate(date.getDate() + daysFromToday);
  return formatYmd(date);
}

async function registerCitizen(
  request: APIRequestContext,
  resources: ScenarioResources,
  options: Parameters<typeof provisionCitizenPortalUser>[1] = {},
): Promise<CitizenPortalUser> {
  const citizen = await provisionCitizenPortalUser(request, options);
  resources.citizens.push(citizen);
  return citizen;
}

async function createDebt(
  request: APIRequestContext,
  resources: ScenarioResources,
  debtorPersonId: string,
  options: DebtSeedOptions = {},
): Promise<{ id: string }> {
  const uniqueSuffix = randomUUID().slice(0, 8);
  const response = await request.post(`${DEBT_SVC}/api/v1/debts`, {
    data: {
      debtorId: debtorPersonId,
      creditorId: DEMO_CREDITOR_ORG_ID,
      debtTypeCode: options.debtTypeCode ?? 'RESTSKAT',
      principalAmount: options.principalAmount ?? 1000,
      interestAmount: options.interestAmount ?? 0,
      feesAmount: options.feesAmount ?? 0,
      dueDate: options.dueDate ?? offsetDate(-14),
      originalDueDate: options.dueDate ?? offsetDate(-14),
      lastPaymentDate: options.lastPaymentDate ?? offsetDate(-30),
      claimArt: options.claimArt ?? 'INDR',
      externalReference: `P026-${uniqueSuffix}`,
      creditorReference: `P026-${uniqueSuffix}`,
      ocrLine: `71${Date.now()}${resources.debtIds.length}`.slice(0, 15),
      description: options.description ?? `petition026-e2e-${uniqueSuffix}`,
    },
  });
  expect(response.ok(), `Create debt for person ${debtorPersonId}`).toBeTruthy();

  const body = (await response.json()) as { id: string };
  resources.debtIds.push(body.id);
  return body;
}

async function writeDownDebt(
  request: APIRequestContext,
  debtId: string,
  amount: number,
): Promise<void> {
  const response = await request.post(`${DEBT_SVC}/api/v1/debts/${debtId}/write-down`, {
    params: {
      amount: amount.toFixed(2),
    },
  });
  expect(response.ok(), `Write down debt ${debtId}`).toBeTruthy();
}

async function cleanupScenario(
  request: APIRequestContext,
  resources: ScenarioResources,
): Promise<void> {
  await Promise.allSettled(
    resources.debtIds.map((debtId) =>
      request.delete(`${DEBT_SVC}/api/v1/debts/${debtId}`),
    ),
  );
  await Promise.allSettled(
    resources.citizens.map((citizen) =>
      deleteCitizenPortalUser(request, citizen.keycloakUserId),
    ),
  );
}

async function expectDebtOverviewHeading(page: Page): Promise<void> {
  await expect(page.getByRole('heading', { level: 1 })).toBeVisible({ timeout: 60_000 });
  await expect(page.getByRole('heading', { level: 1 })).toContainText(/g[æa]ld|debt/i);
}

async function openDebtOverview(
  page: Page,
  citizen: Pick<CitizenPortalUser, 'username' | 'password'>,
  relativePath = '/min-gaeld',
): Promise<void> {
  await authenticateCitizenPortal(page, citizen, relativePath);
  await expect(page).toHaveURL(/\/borger\/min-gaeld(?:\?|$)/, { timeout: 60_000 });
  await expectDebtOverviewHeading(page);
}

function debtTable(page: Page) {
  return page.locator('main#main-content table').first();
}

async function expectDebtTableStructure(page: Page): Promise<void> {
  const table = debtTable(page);
  await expect(table).toBeVisible({ timeout: 60_000 });
  await expect(table.locator('caption')).toBeVisible();
  await expect(table.locator('thead')).toBeVisible();

  const scopedHeaders = table.locator('thead th[scope="col"]');
  const headerCount = await scopedHeaders.count();
  expect(headerCount, 'Debt table must expose semantic column headers').toBeGreaterThanOrEqual(6);

  await expect(table.getByRole('columnheader', { name: /debt type|gaeldstype|claim type/i })).toBeVisible();
  await expect(table.getByRole('columnheader', { name: /creditor|fordringshaver/i })).toBeVisible();
  await expect(table.getByRole('columnheader', { name: /principal|hovedstol/i })).toBeVisible();
  await expect(table.getByRole('columnheader', { name: /outstanding|saldo|balance/i })).toBeVisible();
  await expect(table.getByRole('columnheader', { name: /due date|forfald|payment deadline/i })).toBeVisible();
  await expect(table.getByRole('columnheader', { name: /status/i })).toBeVisible();
}

async function clickNextPaginationLink(page: Page): Promise<void> {
  const namedNextLink = page.getByRole('link', { name: /next|næste/i });
  if ((await namedNextLink.count()) > 0) {
    await Promise.all([
      page.waitForURL(/page=1/, { timeout: 30_000 }),
      namedNextLink.first().click(),
    ]);
    return;
  }

  const pageTwoLink = page.locator('a[href*="page=1"]').first();
  await expect(pageTwoLink, 'Pagination link for the next page').toBeVisible();
  await Promise.all([
    page.waitForURL(/page=1/, { timeout: 30_000 }),
    pageTwoLink.click(),
  ]);
}

async function openNoScriptDebtOverview(
  browser: Browser,
  citizen: Pick<CitizenPortalUser, 'username' | 'password'>,
): Promise<Page> {
  const context = await browser.newContext({ javaScriptEnabled: false });
  const page = await context.newPage();
  await authenticateCitizenPortal(page, citizen, '/min-gaeld');
  await expect(page).toHaveURL(/\/borger\/min-gaeld(?:\?|$)/, { timeout: 60_000 });
  return page;
}

function isCitizenDebtSummaryRequestUrl(requestUrl: string): boolean {
  return new URL(requestUrl).pathname.endsWith(DEBT_SUMMARY_PATH);
}

function readPaginationContract(requestUrl: URL): { pageNumber: string | null; pageSize: string | null } {
  return {
    pageNumber: requestUrl.searchParams.get('pageNumber'),
    pageSize: requestUrl.searchParams.get('pageSize'),
  };
}

function waitForCitizenDebtSummaryRequest(page: Page) {
  return page.waitForRequest(
    (request) => request.method() === 'GET' && isCitizenDebtSummaryRequestUrl(request.url()),
    { timeout: 60_000 },
  );
}

function waitForCitizenDebtSummaryResponse(page: Page) {
  return page.waitForResponse(
    (response) =>
      response.request().method() === 'GET' && isCitizenDebtSummaryRequestUrl(response.url()),
    { timeout: 60_000 },
  );
}

function buildCitizenDebtSummaryResponse(
  debts: StubCitizenDebtItem[],
  pageNumber = 0,
  pageSize = DEBT_OVERVIEW_PAGE_SIZE,
) {
  return {
    debts,
    totalOutstandingAmount: debts.reduce((total, debt) => total + debt.outstandingAmount, 0),
    totalDebtCount: debts.length,
    pageNumber,
    pageSize,
    totalPages: Math.max(1, Math.ceil(debts.length / pageSize)),
    totalElements: debts.length,
    effectiveInterestRates: [],
  };
}

async function stubCitizenDebtSummary(
  page: Page,
  responder: (requestUrl: URL) => { status?: number; body: object },
): Promise<void> {
  await page.route('**/api/v1/citizen/debts**', async (route) => {
    const requestUrl = new URL(route.request().url());
    const { status = 200, body } = responder(requestUrl);
    await route.fulfill({
      status,
      contentType: 'application/json',
      body: JSON.stringify(body),
    });
  });
}

test.describe('Citizen debt overview page (petition026)', () => {
  test.describe.configure({ mode: 'serial' });

  test('VAL-P026-001 — unauthenticated citizen is redirected to MitID/TastSelv from the debt overview page', async ({
    request,
  }) => {
    // Validation: VAL-P026-001
    const response = await request.get(`${CITIZEN_BASE}/min-gaeld`, { maxRedirects: 0 });

    expect(response.status(), 'Unauthenticated access must redirect into the citizen login flow').toBe(
      302,
    );
    expect(response.headers().location ?? '', 'Citizen login redirect location').toMatch(
      /oauth2|authorization|openid-connect/i,
    );
  });

  test('VAL-P026-002 — landing page MitID call-to-action opens the internal debt overview after login', async ({
    page,
    request,
  }) => {
    // Validation: VAL-P026-002
    const resources = createScenarioResources();

    try {
      const citizen = await registerCitizen(request, resources);

      await page.goto(LANDING, { waitUntil: 'domcontentloaded' });
      const cta = page.locator('a.skat-button--primary').filter({ hasText: /g[æa]ld|debt/i }).first();
      await expect(cta).toBeVisible();

      const href = await cta.getAttribute('href');
      expect(href ?? '', 'Landing CTA must point to the internal debt overview flow').toMatch(
        /(^\/(borger\/)?min-gaeld$|^https?:\/\/[^/]+\/(?:borger\/)?min-gaeld$)/i,
      );

      await cta.click();
      await page.waitForLoadState('domcontentloaded');
      await loginCitizenIfPrompted(page, citizen);

      await expect(page).toHaveURL(/\/borger\/min-gaeld(?:\?|$)/, { timeout: 60_000 });
      await expectDebtOverviewHeading(page);
    } finally {
      await cleanupScenario(request, resources);
    }
  });

  test('VAL-P026-003 — debt overview loads debt data only for the authenticated session person', async ({
    page,
    request,
  }) => {
    // Validation: VAL-P026-003
    const resources = createScenarioResources();

    try {
      const citizen = await registerCitizen(request, resources);
      const otherCitizen = await registerCitizen(request, resources);
      const debtSummaryRequestPromise = waitForCitizenDebtSummaryRequest(page);

      await createDebt(request, resources, citizen.personId, {
        principalAmount: 11111.11,
        debtTypeCode: 'RESTSKAT',
      });
      await createDebt(request, resources, otherCitizen.personId, {
        principalAmount: 99999.99,
        debtTypeCode: 'MOMSGAELD',
      });

      await openDebtOverview(page, citizen);
      const debtSummaryRequest = await debtSummaryRequestPromise;
      const debtSummaryUrl = new URL(debtSummaryRequest.url());
      const { pageNumber, pageSize } = readPaginationContract(debtSummaryUrl);

      expect(debtSummaryUrl.pathname, 'Debt overview must call GET /api/v1/citizen/debts').toMatch(
        /\/api\/v1\/citizen\/debts$/,
      );
      expect(pageNumber, 'Debt overview request must include the first pageNumber').toBe('0');
      expect(pageSize, 'Debt overview request must include the fixed portal pageSize').toBe(
        `${DEBT_OVERVIEW_PAGE_SIZE}`,
      );

      await expect(page.locator('body')).toContainText(formatCurrency(11111.11));
      await expect(page.locator('body')).not.toContainText(formatCurrency(99999.99));
    } finally {
      await cleanupScenario(request, resources);
    }
  });

  test('VAL-P026-004 — debt overview shows total outstanding amount, a semantic debt table, and accessible pagination', async ({
    page,
    request,
  }) => {
    // Validation: VAL-P026-004
    test.slow();

    const resources = createScenarioResources();

    try {
      const citizen = await registerCitizen(request, resources);
      let totalOutstanding = 0;

      for (let index = 0; index < 101; index += 1) {
        const principalAmount = 1000 + index;
        totalOutstanding += principalAmount;
        await createDebt(request, resources, citizen.personId, {
          principalAmount,
          debtTypeCode: index % 2 === 0 ? 'RESTSKAT' : 'UNDERHOLDSBIDRAG',
          dueDate: offsetDate(-(index + 1)),
        });
      }

      await openDebtOverview(page, citizen);
      await expect(page.locator('body')).toContainText(formatCurrency(totalOutstanding));
      await expectDebtTableStructure(page);

      const pageZeroFirstRow = await debtTable(page).locator('tbody tr').first().innerText();
      await clickNextPaginationLink(page);
      await expect(page.locator('body')).toContainText(/page|side|1|2/i);

      const pageOneFirstRow = await debtTable(page).locator('tbody tr').first().innerText();
      expect(pageOneFirstRow.trim(), 'Second page must not repeat the first-page row set').not.toBe(
        pageZeroFirstRow.trim(),
      );
    } finally {
      await cleanupScenario(request, resources);
    }
  });

  test('VAL-P026-005 — debt rows display the status returned for a debt', async ({ page, request }) => {
    // Validation: VAL-P026-005
    const resources = createScenarioResources();

    try {
      const citizen = await registerCitizen(request, resources, {
        cpr: SEEDED_CITIZENS.lars.cpr,
      });

      await openDebtOverview(page, citizen);

      const collectionRow = page.getByRole('row').filter({ hasText: /in collection|inddrivelse/i }).first();
      await expect(collectionRow).toBeVisible();
      await expect(collectionRow).not.toContainText(/paid|betalt/i);
    } finally {
      await cleanupScenario(request, resources);
    }
  });

  test('VAL-P026-006 — citizen with no outstanding debt sees a clear accessible no-debt state', async ({
    page,
    request,
  }) => {
    // Validation: VAL-P026-006
    const resources = createScenarioResources();

    try {
      const citizen = await registerCitizen(request, resources);
      await openDebtOverview(page, citizen);

      const noDebtMessage = page.getByText(/no debt|ingen gæld|no outstanding debt/i);
      await expect(noDebtMessage).toBeVisible();
      await expect(page.locator('[role="status"], [role="alert"]')).toContainText(
        /no debt|ingen gæld|no outstanding debt/i,
      );
      await expect(page.getByRole('table')).toHaveCount(0);
    } finally {
      await cleanupScenario(request, resources);
    }
  });

  test('VAL-P026-007 — debt overview presents interest, snapshot, and contact explanations', async ({
    page,
    request,
  }) => {
    // Validation: VAL-P026-007
    const resources = createScenarioResources();

    try {
      const citizen = await registerCitizen(request, resources, {
        cpr: SEEDED_CITIZENS.mads.cpr,
      });

      await openDebtOverview(page, citizen);

      await expect(page.locator('body')).toContainText(/daily|dagligt/i);
      await expect(page.locator('body')).toContainText(/before principal|før hovedstol/i);
      await expect(page.locator('body')).toContainText(/interest rate|rentesats|current rate/i);
      await expect(page.locator('body')).toContainText(formatCurrency(1392.91));
      await expect(page.locator('body')).toContainText(/snapshot|øjebliksbillede/i);
      await expect(page.locator('body')).toContainText(/may differ slightly|kan afvige/i);
      await expect(page.locator('body')).toContainText('70 15 73 04');
    } finally {
      await cleanupScenario(request, resources);
    }
  });

  test('VAL-P026-008 — debt overview shows payment, PDF placeholder, and navigation links', async ({
    page,
    request,
  }) => {
    // Validation: VAL-P026-008
    const resources = createScenarioResources();

    try {
      const citizen = await registerCitizen(request, resources, {
        cpr: SEEDED_CITIZENS.mads.cpr,
      });

      await openDebtOverview(page, citizen);

      const paymentLink = page.getByRole('link', { name: /pay|betal/i }).first();
      await expect(paymentLink).toBeVisible();
      await expect(paymentLink).toHaveAttribute('href', /^https?:\/\//);

      const pdfAffordance = page.locator('a,button').filter({ hasText: /pdf/i }).first();
      await expect(pdfAffordance).toBeVisible();
      await expect(page.locator('body')).toContainText(/coming soon|future enhancement|kommende/i);

      const backLink = page.getByRole('link', { name: /back|tilbage/i }).first();
      await expect(backLink).toBeVisible();
      await expect(backLink).toHaveAttribute('href', /\/borger\/?$/);
    } finally {
      await cleanupScenario(request, resources);
    }
  });

  test('VAL-P026-009 — debt overview uses localized message bundles and locale-aware currency formatting', async ({
    page,
    request,
  }) => {
    // Validation: VAL-P026-009
    const resources = createScenarioResources();

    try {
      const citizen = await registerCitizen(request, resources);
      await createDebt(request, resources, citizen.personId, {
        principalAmount: 12345.67,
      });

      await authenticateCitizenPortal(page, citizen, '/min-gaeld?lang=da-DK');
      await page.goto(`${CITIZEN_BASE}/min-gaeld?lang=da-DK`, { waitUntil: 'domcontentloaded' });
      await expect(page).toHaveURL(/\/borger\/min-gaeld.*lang=da-DK/, { timeout: 60_000 });
      await expect(page.locator('html')).toHaveAttribute('lang', /da/i);
      await expect(page.getByRole('heading', { level: 1 })).toContainText(/g[æa]ld/i);
      await expect(page.locator('body')).toContainText(formatCurrency(12345.67, 'da-DK'));

      await page.goto(`${CITIZEN_BASE}/min-gaeld?lang=en-GB`, { waitUntil: 'domcontentloaded' });
      await expect(page).toHaveURL(/\/borger\/min-gaeld.*lang=en-GB/, { timeout: 60_000 });
      await expect(page.locator('html')).toHaveAttribute('lang', /en/i);
      await expect(page.getByRole('heading', { level: 1 })).toContainText(/debt/i);
      await expect(page.locator('body')).toContainText(formatCurrency(12345.67, 'en-GB'));
    } finally {
      await cleanupScenario(request, resources);
    }
  });

  test('VAL-P026-010 — debt overview is keyboard-navigable and screen-reader compatible', async ({
    page,
    request,
  }) => {
    // Validation: VAL-P026-010
    const resources = createScenarioResources();

    try {
      const citizen = await registerCitizen(request, resources);
      await createDebt(request, resources, citizen.personId, {
        principalAmount: 4242.42,
        interestAmount: 42.42,
      });

      await openDebtOverview(page, citizen);
      await expectDebtTableStructure(page);

      await page.keyboard.press('Tab');
      await expect(page.locator(':focus')).toHaveAttribute('href', '#main-content');

      const paymentLink = page.getByRole('link', { name: /pay|betal/i }).first();
      const paymentHref = await paymentLink.getAttribute('href');
      let paymentReachedByKeyboard = false;
      for (let index = 0; index < 20; index += 1) {
        await page.keyboard.press('Tab');
        const focusedHref = await page.locator(':focus').getAttribute('href');
        if (paymentHref && focusedHref === paymentHref) {
          paymentReachedByKeyboard = true;
          break;
        }
      }

      expect(paymentReachedByKeyboard, 'Keyboard navigation must reach the payment action').toBeTruthy();
      await expect(debtTable(page).locator('caption')).toBeVisible();
      const headerCount = await debtTable(page).locator('thead th[scope="col"]').count();
      expect(headerCount, 'Debt table must keep at least the required semantic columns').toBeGreaterThanOrEqual(
        7,
      );
    } finally {
      await cleanupScenario(request, resources);
    }
  });

  test('VAL-P026-011 — debt-service unavailability is communicated without exposing stack traces', async ({
    page,
    request,
  }) => {
    // Validation: VAL-P026-011
    const resources = createScenarioResources();

    try {
      const citizen = await registerCitizen(request, resources);
      const debtSummaryRequestPromise = waitForCitizenDebtSummaryRequest(page);
      const debtSummaryResponsePromise = waitForCitizenDebtSummaryResponse(page);
      await stubCitizenDebtSummary(page, () => ({
        status: 503,
        body: {
          timestamp: '2026-01-01T00:00:00Z',
          status: 503,
          error: 'Service Unavailable',
          path: DEBT_SUMMARY_PATH,
        },
      }));

      await authenticateCitizenPortal(page, citizen, '/min-gaeld');
      await expect(page).toHaveURL(/\/borger\/min-gaeld(?:\?|$)/, { timeout: 60_000 });
      const debtSummaryRequest = await debtSummaryRequestPromise;
      const debtSummaryResponse = await debtSummaryResponsePromise;
      const debtSummaryUrl = new URL(debtSummaryRequest.url());
      const { pageNumber, pageSize } = readPaginationContract(debtSummaryUrl);

      expect(debtSummaryResponse.status(), 'Debt overview must observe a 503 downstream response').toBe(
        503,
      );
      expect(pageNumber, 'Service-unavailable request must still include pageNumber').toBe('0');
      expect(pageSize, 'Service-unavailable request must still include the fixed pageSize').toBe(
        `${DEBT_OVERVIEW_PAGE_SIZE}`,
      );

      await expect(page.locator('[role="alert"], [role="status"]')).toContainText(
        /temporarily unavailable|midlertidigt utilgængelig|service unavailable/i,
      );
      await expect(page.locator('body')).not.toContainText(
        /stack trace|exception|java\.lang|org\.springframework/i,
      );
    } finally {
      await cleanupScenario(request, resources);
    }
  });

  test('VAL-P026-012 — debt overview works without client-side scripting', async ({
    browser,
    request,
  }) => {
    // Validation: VAL-P026-012
    const resources = createScenarioResources();
    let page: Page | undefined;

    try {
      const citizen = await registerCitizen(request, resources);
      await createDebt(request, resources, citizen.personId, {
        principalAmount: 2222.22,
      });

      page = await openNoScriptDebtOverview(browser, citizen);
      await expectDebtOverviewHeading(page);
      await expect(page.locator('body')).toContainText(formatCurrency(2222.22));
      await expect(debtTable(page)).toBeVisible();
    } finally {
      await page?.context().close();
      await cleanupScenario(request, resources);
    }
  });

  test('VAL-P026-013 — paused interest is explained when accrual is suspended for unclear debt', async ({
    page,
    request,
  }) => {
    // Validation: VAL-P026-013
    const resources = createScenarioResources();

    try {
      const citizen = await registerCitizen(request, resources, {
        cpr: SEEDED_CITIZENS.jens.cpr,
      });

      await openDebtOverview(page, citizen);

      await expect(page.locator('body')).toContainText(/paused|pauset/i);
      await expect(page.locator('body')).toContainText(/unclear|uklar/i);
    } finally {
      await cleanupScenario(request, resources);
    }
  });

  test('VAL-P026-014 — debt overview explains that recovery interest is not tax-deductible', async ({
    page,
    request,
  }) => {
    // Validation: VAL-P026-014
    const resources = createScenarioResources();

    try {
      const citizen = await registerCitizen(request, resources, {
        cpr: SEEDED_CITIZENS.emma.cpr,
      });

      await openDebtOverview(page, citizen);

      await expect(page.locator('body')).toContainText(/not tax-deductible|ikke fradragsberettiget/i);
    } finally {
      await cleanupScenario(request, resources);
    }
  });

  test('VAL-P026-015 — written-off debt status explains why the debt was closed', async ({
    page,
    request,
  }) => {
    // Validation: VAL-P026-015
    const resources = createScenarioResources();

    try {
      const citizen = await registerCitizen(request, resources);
      await stubCitizenDebtSummary(page, (requestUrl) => {
        const { pageNumber, pageSize } = readPaginationContract(requestUrl);
        return {
          body: buildCitizenDebtSummaryResponse(
            WRITTEN_OFF_REASON_FIXTURES.map(({ debt }) => debt),
            Number.parseInt(pageNumber ?? '0', 10),
            Number.parseInt(pageSize ?? String(DEBT_OVERVIEW_PAGE_SIZE), 10),
          ),
        };
      });

      await openDebtOverview(page, citizen);

      await expect(page.locator('body')).toContainText(/written off|afskrevet/i);
      for (const { visibleReason } of WRITTEN_OFF_REASON_FIXTURES) {
        await expect(page.locator('body')).toContainText(visibleReason);
      }
    } finally {
      await cleanupScenario(request, resources);
    }
  });
});
