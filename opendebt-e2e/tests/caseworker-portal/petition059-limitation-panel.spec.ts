import { test } from '@playwright/test';

/**
 * Forældelse — prescription tracking and interruption (P059)
 *
 * Petition : petition059
 * Feature  : petitions/petition059-foraeldelse.feature
 * Spec     : petitions/specs/petition059-specs.yaml
 * Contract : petitions/petition059-foraeldelse-outcome-contract.md
 *
 * RED PHASE — all tests intentionally fail.
 * Replace throw statements with Playwright actions to turn green.
 */

// Traceability:
// - Spec module: opendebt-caseworker-portal.limitation-panel
// - Validation: VAL-P059-040 .. VAL-P059-046
// - Scope: caseworker-portal user-visible scenarios only (FR-7.*)

test.describe('Forældelse — prescription tracking and interruption (P059) @backlog', () => {
  // Ref: petitions/petition059-foraeldelse.feature — Feature: "Forældelse — prescription tracking and interruption (P059)"
  // Spec: petitions/specs/petition059-specs.yaml — module "opendebt-caseworker-portal.limitation-panel"

  test('FR-7.1 Sagsbehandlerportalen viser forældelsesstatus med ISO-dato og udskydelse', async () => {
    // Ref: petition059-foraeldelse.feature — Scenario: "FR-7.1 Sagsbehandlerportalen viser forældelsesstatus med ISO-dato og udskydelse"
    // Spec: petition059-specs.yaml — opendebt-caseworker-portal.limitation-panel
    // Validation: VAL-P059-040

    // Given en sagsbehandler er autentificeret med rollen "CASEWORKER"
    // And fordringen "FDR-59090" har følgende forældelsesstatus:
    //   | status              | ACTIVE      |
    //   | currentFristExpires | 2027-03-15  |
    //   | udskydelseDato      | 2021-11-20  |
    //   | isInUdskydelse      | false       |
    // When sagsbehandleren navigerer til detaljevisningen for fordringen "FDR-59090"
    // Then vises forældelsesstatus-panelet med:
    //   | Status               | Aktiv       |
    //   | Frist udløber        | 2027-03-15  |
    //   | Udskydelsesdato      | 2021-11-20  |
    //   | I udskydelsesvindue  | Nej         |

    throw new Error(
      'Not implemented: petition059 — "FR-7.1 Sagsbehandlerportalen viser forældelsesstatus med ISO-dato og udskydelse"',
    );
  });

  test('FR-7.2 Sagsbehandlerportalen viser afbrydelseshistorik med resulting new frist', async () => {
    // Ref: petition059-foraeldelse.feature — Scenario: "FR-7.2 Sagsbehandlerportalen viser afbrydelseshistorik med resulting new frist"
    // Spec: petition059-specs.yaml — opendebt-caseworker-portal.limitation-panel
    // Validation: VAL-P059-041

    // Given en fordring "FDR-59091" har to registrerede afbrydelseshændelser
    // When sagsbehandleren navigerer til detaljevisningen for fordringen "FDR-59091"
    // Then vises afbrydelseshistorik-tabellen med 2 rækker i kronologisk rækkefølge
    // And indeholder hver række type, dato, juridisk reference og resulting new frist

    throw new Error(
      'Not implemented: petition059 — "FR-7.2 Sagsbehandlerportalen viser afbrydelseshistorik med resulting new frist"',
    );
  });

  test('FR-7.3 Sagsbehandlerportalen viser tillægsfristhistorik og fordringskompleks-medlemskab', async () => {
    // Ref: petition059-foraeldelse.feature — Scenario: "FR-7.3 Sagsbehandlerportalen viser tillægsfristhistorik og fordringskompleks-medlemskab"
    // Spec: petition059-specs.yaml — opendebt-caseworker-portal.limitation-panel
    // Validation: VAL-P059-042

    // Given fordringen "FDR-59092" er medlem af kompleks "K-010" med medlemmet "FDR-59093"
    // And fordringen "FDR-59092" har en registreret tillægsfrist
    // And fordringen "FDR-59092" har en propageret afbrydelseshændelse med "sourceFordringId" = "FDR-59093"
    // And fordringen "FDR-59092" har en propageret afbrydelseshændelse med "targetFordringId" = "FDR-59092"
    // When sagsbehandleren navigerer til detaljevisningen for fordringen "FDR-59092"
    // Then vises afsnittet "Fordringskompleks" i panelet
    // And listes fordringen "FDR-59093" som medlem af komplekset
    // And vises tillægsfristhistorikken med type, dato, extension og ny frist
    // And vises "sourceFordringId" = "FDR-59093" for den propagerede afbrydelseshændelse
    // And vises "targetFordringId" = "FDR-59092" for den propagerede afbrydelseshændelse

    throw new Error(
      'Not implemented: petition059 — "FR-7.3 Sagsbehandlerportalen viser tillægsfristhistorik og fordringskompleks-medlemskab"',
    );
  });

  test('FR-7.4 Sagsbehandler med skriveadgang ser knap til registrering af forældelsesindsigelse', async () => {
    // Ref: petition059-foraeldelse.feature — Scenario: "FR-7.4 Sagsbehandler med skriveadgang ser knap til registrering af forældelsesindsigelse"
    // Spec: petition059-specs.yaml — opendebt-caseworker-portal.limitation-panel
    // Validation: VAL-P059-043

    // Given en fordring "FDR-59094" har status "ACTIVE"
    // And sagsbehandleren har rolle "CASEWORKER" med skriveadgang
    // When sagsbehandleren navigerer til detaljevisningen for fordringen "FDR-59094"
    // Then vises knappen "Registrer forældelsesindsigelse"

    throw new Error(
      'Not implemented: petition059 — "FR-7.4 Sagsbehandler med skriveadgang ser knap til registrering af forældelsesindsigelse"',
    );
  });

  test('FR-7.5 Afventende indsigelse viser evalueringsformular med rationalefelt', async () => {
    // Ref: petition059-foraeldelse.feature — Scenario: "FR-7.5 Afventende indsigelse viser evalueringsformular med rationalefelt"
    // Spec: petition059-specs.yaml — opendebt-caseworker-portal.limitation-panel
    // Validation: VAL-P059-044

    // Given en fordring "FDR-59095" har status "INDSIGELSE_PENDING"
    // When sagsbehandleren navigerer til detaljevisningen for fordringen "FDR-59095"
    // Then vises evalueringsformularen med valg for "Gyldig" og "Ugyldig"
    // And vises et tekstfelt til "rationale"
    // And er registreringsknappen ikke tilgængelig

    throw new Error(
      'Not implemented: petition059 — "FR-7.5 Afventende indsigelse viser evalueringsformular med rationalefelt"',
    );
  });

  test('FR-7.6 Forældet fordring viser udfald og rationale uden registreringsknap', async () => {
    // Ref: petition059-foraeldelse.feature — Scenario: "FR-7.6 Forældet fordring viser udfald og rationale uden registreringsknap"
    // Spec: petition059-specs.yaml — opendebt-caseworker-portal.limitation-panel
    // Validation: VAL-P059-045

    // Given en fordring "FDR-59096" har status "FORAELDET"
    // And den seneste indsigelse blev vurderet som "VALID" med rationale "Forældelsesfrist udløb 2023-11-21"
    // When sagsbehandleren navigerer til detaljevisningen for fordringen "FDR-59096"
    // Then vises udfaldet "Forældet"
    // And vises rationalet "Forældelsesfrist udløb 2023-11-21"
    // And vises knappen "Registrer forældelsesindsigelse" ikke

    throw new Error(
      'Not implemented: petition059 — "FR-7.6 Forældet fordring viser udfald og rationale uden registreringsknap"',
    );
  });

  test('FR-7.7 Read-only caseworker ser panelet men ingen skrivehandlinger', async () => {
    // Ref: petition059-foraeldelse.feature — Scenario: "FR-7.7 Read-only caseworker ser panelet men ingen skrivehandlinger"
    // Spec: petition059-specs.yaml — opendebt-caseworker-portal.limitation-panel
    // Validation: VAL-P059-046

    // Given en sagsbehandler er autentificeret med læseadgang til fordringen "FDR-59097"
    // And fordringen "FDR-59097" har status "ACTIVE"
    // When sagsbehandleren navigerer til detaljevisningen for fordringen "FDR-59097"
    // Then vises forældelsesstatus-panelet
    // And kan sagsbehandleren ikke registrere afbrydelseshændelser
    // And kan sagsbehandleren ikke registrere eller evaluere indsigelser

    throw new Error(
      'Not implemented: petition059 — "FR-7.7 Read-only caseworker ser panelet men ingen skrivehandlinger"',
    );
  });
});
