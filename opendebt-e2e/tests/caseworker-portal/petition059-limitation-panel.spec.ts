import { test, expect, type APIRequestContext } from '@playwright/test';
import {
  authenticateCaseworkerWritable,
  authenticateCaseworkerReadOnly,
  CW_BASE,
  CASE_SVC,
  DEBT_SVC,
} from '../helpers/caseworker-portal-auth';

/**
 * Forældelse — prescription tracking and interruption (P059)
 *
 * Petition : petition059
 * Feature  : petitions/petition059-foraeldelse.feature
 * Spec     : petitions/specs/petition059-specs.yaml
 * Contract : petitions/petition059-foraeldelse-outcome-contract.md
 *
 * Tag: @backlog — manual execution only against a running docker-compose stack.
 * CI excludes @backlog tests via `grepInvert` in playwright.config.ts.
 *
 * Prerequisites for state-specific tests (FR-7.2 – FR-7.6):
 *   - At least 4 fordringer must have ACTIVE foraeldelse_record rows in the
 *     debt-service DB (created when claims are transferred for collection via
 *     the integration-gateway acceptClaim flow).
 *   - @EnableMethodSecurity is disabled under the "local" and "dev" Spring profiles
 *     (MethodSecurityConfig uses @Profile("!dev & !local")), so all API calls to
 *     case-service and debt-service succeed without auth headers on the local stack.
 */

// Traceability:
// - Spec module: opendebt-caseworker-portal.limitation-panel
// - Validation: VAL-P059-040 .. VAL-P059-046
// - Scope: caseworker-portal user-visible scenarios only (FR-7.*)

// ── Types ─────────────────────────────────────────────────────────────────────

interface FordringPair {
  caseId: string;
  fordringId: string;
}

// ── Discovery ─────────────────────────────────────────────────────────────────

/**
 * Queries case-service and debt-service to find fordringer that have an ACTIVE
 * foraeldelse_record row. Returns up to `needed` pairs.
 */
async function findActiveFordringer(
  request: APIRequestContext,
  needed: number,
): Promise<FordringPair[]> {
  const found: FordringPair[] = [];
  try {
    const casesRes = await request.get(`${CASE_SVC}/api/v1/cases?size=30`);
    if (!casesRes.ok()) return found;
    const body = await casesRes.json();
    const cases: Array<{ id: string }> = body.content ?? body ?? [];
    for (const c of cases) {
      if (found.length >= needed) break;
      const debtsRes = await request.get(`${CASE_SVC}/api/v1/cases/${c.id}/debts`);
      if (!debtsRes.ok()) continue;
      for (const d of (await debtsRes.json()) as Array<{ debtId?: string; id?: string }>) {
        if (found.length >= needed) break;
        const fordringId = d.debtId ?? d.id ?? '';
        if (!fordringId) continue;
        const limRes = await request.get(`${DEBT_SVC}/api/v1/foraeldelse/${fordringId}`);
        if (limRes.ok()) {
          const limBody = await limRes.json();
          if (limBody.status === 'ACTIVE') {
            found.push({ caseId: c.id, fordringId });
          }
        }
      }
    }
  } catch {
    // stack not running — return empty list; tests skip gracefully
  }
  return found;
}

// ── Suite ─────────────────────────────────────────────────────────────────────

test.describe('Forældelse — prescription tracking and interruption (P059) @backlog', () => {
  // Ref: petitions/petition059-foraeldelse.feature — Feature: "Forældelse — prescription tracking and interruption (P059)"
  // Spec: petitions/specs/petition059-specs.yaml — module "opendebt-caseworker-portal.limitation-panel"

  /**
   * Synthetic pair: the caseworker portal renders the limitation panel via
   * circuit-breaker fallback for any UUID. Panel shows "Aktiv" status with
   * null dates. Safe for structural tests (FR-7.1, FR-7.7) that do not require
   * specific limitation state.
   */
  const FALLBACK_PAIR: FordringPair = {
    caseId: '00000000-0000-0000-0000-000000000059',
    fordringId: '00000000-0000-0000-0000-000000000059',
  };

  /**
   * Pairs with confirmed ACTIVE foraeldelse_record rows.
   * Populated in beforeAll. Index assignments:
   *   [0] → FR-7.2 (afbrydelse events), FR-7.4 (write button)
   *   [1] → FR-7.3 fordringA (complex + tillaegsfrist + afbrydelse) [exclusive]
   *   [2] → FR-7.5 (indsigelse → INDSIGELSE_PENDING) [exclusive]
   *   [3] → FR-7.6 (indsigelse → evaluate → FORAELDET)
   *
   * FR-7.3 fordringB uses a STATIC seed UUID (fd00-005, not in activePairs).
   * This ensures createComplex() never races with FR-7.2's afbrydelse saves on
   * fd00-001 — both update ForaeldelseRecord, which has @Version optimistic locking.
   */
  let activePairs: FordringPair[] = [];
  let anyPair: FordringPair = FALLBACK_PAIR;

  test.beforeAll(async ({ request }) => {
    activePairs = await findActiveFordringer(request, 4);
    if (activePairs.length > 0) {
      anyPair = activePairs[0];
    }
  });

  // ── FR-7.1 ───────────────────────────────────────────────────────────────

  test('FR-7.1 Sagsbehandlerportalen viser forældelsesstatus med ISO-dato og udskydelse', async ({
    page,
  }) => {
    // Ref: petition059-foraeldelse.feature — Scenario: "FR-7.1 Sagsbehandlerportalen viser forældelsesstatus med ISO-dato og udskydelse"
    // Validation: VAL-P059-040
    //
    // Works even without a real foraeldelse_record: circuit-breaker fallback
    // returns empty LimitationPanelData; statusLabel defaults to "Aktiv".

    await authenticateCaseworkerWritable(page);
    await page.goto(
      `${CW_BASE}/cases/${anyPair.caseId}/debts/${anyPair.fordringId}/limitation-panel`,
    );

    const panel = page.locator('[data-testid="limitation-status-panel"]');
    await expect(panel).toBeVisible({ timeout: 15_000 });

    // The dl contains 4 dt/dd pairs: Status, Frist udløber, Udskydelsesdato, I udskydelsesvindue
    const dds = panel.locator('dl dd');
    await expect(dds.nth(0)).toBeVisible(); // statusLabel
    await expect(dds.nth(1)).toBeVisible(); // currentFristExpires
    await expect(dds.nth(2)).toBeVisible(); // udskydelseDato
    await expect(dds.nth(3)).toBeVisible(); // yesNoUdskydelse ("Ja"/"Nej")
  });

  // ── FR-7.2 ───────────────────────────────────────────────────────────────

  test('FR-7.2 Sagsbehandlerportalen viser afbrydelseshistorik med resulting new frist', async ({
    page,
    request,
  }) => {
    // Ref: petition059-foraeldelse.feature — Scenario: "FR-7.2 Sagsbehandlerportalen viser afbrydelseshistorik med resulting new frist"
    // Validation: VAL-P059-041
    test.skip(
      activePairs.length === 0,
      'Requires ≥1 fordring with ACTIVE foraeldelse_record. Transfer a claim for collection via the integration-gateway first.',
    );
    const { caseId, fordringId } = activePairs[0];

    // Register two afbrydelse events (each call appends a new row; may accumulate across runs)
    await request.post(`${DEBT_SVC}/api/v1/foraeldelse/${fordringId}/afbrydelse`, {
      data: {
        type: 'BEROSTILLELSE',
        eventDate: '2023-04-01',
        legalReference: 'GIL § 18a, stk. 8',
        afgoerelseRegistreret: false,
      },
    });
    await request.post(`${DEBT_SVC}/api/v1/foraeldelse/${fordringId}/afbrydelse`, {
      data: {
        type: 'UDLAEG',
        eventDate: '2023-10-01',
        legalReference: 'Forældelsesl. § 18, stk. 1',
        afgoerelseRegistreret: false,
      },
    });

    await authenticateCaseworkerWritable(page);
    await page.goto(`${CW_BASE}/cases/${caseId}/debts/${fordringId}/limitation-panel`);

    const rows = page.locator('[data-afbrydelse-row="true"]');
    // Assert ≥2 rows (count may be higher on repeated runs against same DB)
    const rowCount = await rows.count();
    expect(rowCount, 'afbrydelse history rows').toBeGreaterThanOrEqual(2);

    const firstRow = rows.first();
    await expect(firstRow.locator('td').nth(0)).not.toBeEmpty(); // type
    await expect(firstRow.locator('td').nth(1)).not.toBeEmpty(); // event date
    await expect(firstRow.locator('td').nth(2)).not.toBeEmpty(); // legal reference
    await expect(firstRow.locator('td').nth(3)).not.toBeEmpty(); // new frist expires
  });

  // ── FR-7.3 ───────────────────────────────────────────────────────────────

  test('FR-7.3 Sagsbehandlerportalen viser tillægsfristhistorik og fordringskompleks-medlemskab', async ({
    page,
    request,
  }) => {
    // Ref: petition059-foraeldelse.feature — Scenario: "FR-7.3 Sagsbehandlerportalen viser tillægsfristhistorik og fordringskompleks-medlemskab"
    // Validation: VAL-P059-042
    //
    // Note: sourceFordringId/targetFordringId on propagated afbrydelse events are set
    // internally by the complex propagation engine — they cannot be set via REST API.
    // The presence of those table columns is verified; the propagated IDs assertion
    // requires a pre-existing propagated event.
    test.skip(
      activePairs.length < 2,
      'Requires ≥2 fordringer with ACTIVE foraeldelse_record for complex and tillaegsfrist setup.',
    );
    const fordringA = activePairs[1].fordringId;
    // fordringB is a static seed UUID (fd00-005) — not in activePairs so it is
    // never mutated by other parallel tests, preventing @Version optimistic-lock
    // conflicts when createComplex() and FR-7.2 run concurrently.
    const fordringB = '05900000-e2e0-0059-fd00-000000000005';
    const { caseId } = activePairs[1];

    // Create a fordringskompleks containing both fordringer
    const kompleksRes = await request.post(`${DEBT_SVC}/api/v1/fordringskompleks`, {
      data: { memberFordringIds: [fordringA, fordringB] },
    });
    expect(kompleksRes.ok(), 'fordringskompleks creation').toBeTruthy();

    // Register a tillaegsfrist on fordringA
    const tillaegRes = await request.post(
      `${DEBT_SVC}/api/v1/foraeldelse/${fordringA}/tillaegsfrist`,
      {
        data: {
          type: 'AFVENTER_KLAGE',
          appliedDate: '2024-06-01',
          legalReference: 'GFL § 26',
        },
      },
    );
    expect(tillaegRes.ok(), 'tillaegsfrist registration').toBeTruthy();

    // Register an afbrydelse event on fordringA to ensure the afbrydelse history
    // section renders in the panel (section is guarded by th:if="!afbrydelseHistory.isEmpty()").
    // sourceFordringId/targetFordringId column presence (lines 249–250) requires the table.
    const afbrydelseRes = await request.post(
      `${DEBT_SVC}/api/v1/foraeldelse/${fordringA}/afbrydelse`,
      {
        data: {
          type: 'BEROSTILLELSE',
          eventDate: '2024-03-01',
          legalReference: 'GIL § 18a, stk. 8',
          afgoerelseRegistreret: false,
        },
      },
    );
    expect(afbrydelseRes.ok(), 'afbrydelse registration for panel table render').toBeTruthy();

    await authenticateCaseworkerWritable(page);
    await page.goto(`${CW_BASE}/cases/${caseId}/debts/${fordringA}/limitation-panel`);

    // Complex section must be visible with fordringB listed as member
    await expect(page.locator('h2:has-text("Fordringskompleks")')).toBeVisible({ timeout: 15_000 });
    await expect(page.locator('li', { hasText: fordringB })).toBeVisible();

    // Tillaegsfrist section must be visible with at least one row
    await expect(page.locator('h2:has-text("Tillægsfristhistorik")')).toBeVisible();
    const tillaegRow = page
      .locator('section:has(h2:has-text("Tillægsfristhistorik")) tbody tr')
      .first();
    await expect(tillaegRow.locator('td').nth(0)).not.toBeEmpty(); // type
    await expect(tillaegRow.locator('td').nth(1)).not.toBeEmpty(); // applied date
    await expect(tillaegRow.locator('td').nth(2)).not.toBeEmpty(); // extension years
    await expect(tillaegRow.locator('td').nth(3)).not.toBeEmpty(); // new frist expires

    // Afbrydelse table columns for propagated events exist (values are null unless
    // propagation engine has run — verified separately via integration test)
    await expect(page.locator('th:has-text("sourceFordringId")')).toBeAttached();
    await expect(page.locator('th:has-text("targetFordringId")')).toBeAttached();
  });

  // ── FR-7.4 ───────────────────────────────────────────────────────────────

  test('FR-7.4 Sagsbehandler med skriveadgang ser knap til registrering af forældelsesindsigelse', async ({
    page,
  }) => {
    // Ref: petition059-foraeldelse.feature — Scenario: "FR-7.4 Sagsbehandler med skriveadgang ser knap til registrering af forældelsesindsigelse"
    // Validation: VAL-P059-043
    //
    // canRegisterObjection = hasWriteAccess(CASEWORKER) && "ACTIVE".equals(status)
    // Uses activePairs[0] which is still ACTIVE after FR-7.2 afbrydelse events.
    test.skip(
      activePairs.length === 0,
      'Requires ≥1 fordring with ACTIVE foraeldelse_record.',
    );
    const { caseId, fordringId } = activePairs[0];

    await authenticateCaseworkerWritable(page);
    await page.goto(`${CW_BASE}/cases/${caseId}/debts/${fordringId}/limitation-panel`);

    await expect(
      page.locator('button', { hasText: 'Registrer forældelsesindsigelse' }),
    ).toBeVisible({ timeout: 15_000 });
  });

  // ── FR-7.5 ───────────────────────────────────────────────────────────────

  test('FR-7.5 Afventende indsigelse viser evalueringsformular med rationalefelt', async ({
    page,
    request,
  }) => {
    // Ref: petition059-foraeldelse.feature — Scenario: "FR-7.5 Afventende indsigelse viser evalueringsformular med rationalefelt"
    // Validation: VAL-P059-044
    //
    // Uses activePairs[2] (exclusive fordring for FR-7.5). Dedicated fordring
    // prevents optimistic-lock conflicts with FR-7.3 in parallel execution.
    test.skip(
      activePairs.length < 3,
      'Requires ≥3 fordringer with ACTIVE foraeldelse_record; activePairs[2] is mutated to INDSIGELSE_PENDING.',
    );
    const { caseId, fordringId } = activePairs[2];

    // Register indsigelse → status transitions to INDSIGELSE_PENDING
    const indsRes = await request.post(
      `${DEBT_SVC}/api/v1/foraeldelse/${fordringId}/indsigelse`,
      { data: {} },
    );
    expect(indsRes.ok(), 'indsigelse registration').toBeTruthy();

    await authenticateCaseworkerWritable(page);
    await page.goto(`${CW_BASE}/cases/${caseId}/debts/${fordringId}/limitation-panel`);

    // showEvaluationForm = hasWriteAccess && "INDSIGELSE_PENDING".equals(status)
    await expect(page.locator('input[name="outcome"][value="VALID"]')).toBeVisible({
      timeout: 15_000,
    });
    await expect(page.locator('input[name="outcome"][value="INVALID"]')).toBeVisible();
    await expect(page.locator('textarea[name="rationale"]')).toBeVisible();

    // Registration button must NOT be present when status is INDSIGELSE_PENDING
    await expect(
      page.locator('button', { hasText: 'Registrer forældelsesindsigelse' }),
    ).not.toBeVisible();
  });

  // ── FR-7.6 ───────────────────────────────────────────────────────────────

  test('FR-7.6 Forældet fordring viser udfald og rationale uden registreringsknap', async ({
    page,
    request,
  }) => {
    // Ref: petition059-foraeldelse.feature — Scenario: "FR-7.6 Forældet fordring viser udfald og rationale uden registreringsknap"
    // Validation: VAL-P059-045
    test.skip(
      activePairs.length < 4,
      'Requires ≥4 fordringer with ACTIVE foraeldelse_record; activePairs[3] is mutated to FORAELDET.',
    );
    const { caseId, fordringId } = activePairs[3];
    const RATIONALE = 'Forældelsesfrist udløb 2023-11-21';

    // Register indsigelse → INDSIGELSE_PENDING
    const indsRes = await request.post(
      `${DEBT_SVC}/api/v1/foraeldelse/${fordringId}/indsigelse`,
      { data: {} },
    );
    expect(indsRes.ok(), 'indsigelse registration').toBeTruthy();
    const { indsigelsesId } = await indsRes.json();

    // Evaluate as VALID → status transitions to FORAELDET, rationale is stored
    const evalRes = await request.put(
      `${DEBT_SVC}/api/v1/foraeldelse/${fordringId}/indsigelse/${indsigelsesId}`,
      { data: { outcome: 'VALID', rationale: RATIONALE } },
    );
    expect(evalRes.ok(), 'indsigelse evaluation').toBeTruthy();

    await authenticateCaseworkerWritable(page);
    await page.goto(`${CW_BASE}/cases/${caseId}/debts/${fordringId}/limitation-panel`);

    // Udfald section visible (rendered when objectionRationale != null)
    await expect(page.locator('h2:has-text("Udfald")')).toBeVisible({ timeout: 15_000 });
    // statusLabel for FORAELDET = "Forældet"
    await expect(page.locator('section:has(h2:has-text("Udfald")) p').first()).toContainText(
      'Forældet',
    );
    await expect(page.locator('section:has(h2:has-text("Udfald")) p').last()).toContainText(
      RATIONALE,
    );

    // Registration button must not appear for FORAELDET status
    await expect(
      page.locator('button', { hasText: 'Registrer forældelsesindsigelse' }),
    ).not.toBeVisible();
  });

  // ── FR-7.7 ───────────────────────────────────────────────────────────────

  test('FR-7.7 Read-only caseworker ser panelet men ingen skrivehandlinger', async ({ page }) => {
    // Ref: petition059-foraeldelse.feature — Scenario: "FR-7.7 Read-only caseworker ser panelet men ingen skrivehandlinger"
    // Validation: VAL-P059-046
    //
    // Works even without a real foraeldelse_record: circuit-breaker fallback renders
    // the panel. SENIOR_CASEWORKER fails hasWriteAccess() → readOnly=true → all
    // Thymeleaf th:if="${!readOnly}" blocks are suppressed.

    await authenticateCaseworkerReadOnly(page);
    await page.goto(
      `${CW_BASE}/cases/${anyPair.caseId}/debts/${anyPair.fordringId}/limitation-panel`,
    );

    const panel = page.locator('[data-testid="limitation-status-panel"]');
    await expect(panel).toBeVisible({ timeout: 15_000 });

    // readOnly=true: afbrydelse button, objection button, and evaluation form are hidden
    await expect(
      page.locator('button', { hasText: 'Registrer afbrydelseshændelse' }),
    ).not.toBeVisible();
    await expect(
      page.locator('button', { hasText: 'Registrer forældelsesindsigelse' }),
    ).not.toBeVisible();
    await expect(page.locator('input[name="outcome"]')).not.toBeVisible();
  });
});
