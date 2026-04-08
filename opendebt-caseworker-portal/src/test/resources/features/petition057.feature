@petition057
Feature: Dækningsrækkefølge — GIL § 4 sagsbehandlerportal view (P057 — FR-8)
  # Module scope: opendebt-caseworker-portal (FR-8 only).
  # FR-1 through FR-7 (rule engine and API) are in opendebt-payment-service petition057.feature.
  # Legal basis: GIL § 4 stk. 1–4, GIL § 6a stk. 1 og stk. 12
  # G.A. snapshot: v3.16 (2026-03-28)
  # AC-16: Sagsbehandlerportal (DaekningsRaekkefoeigenViewController + daekningsraekkefoelge.html)
  # Canonical source: petitions/petition057-daekningsraekkefoeigen.feature

  Background:
    Given the payment-service rule engine is active
    And the sagsbehandler portal is running

  # ==============================================================================
  # FR-8: Sagsbehandler portal — dækningsrækkefølge view
  # AC-16
  # ==============================================================================

  Scenario: Sagsbehandlerportal viser ordnet liste med GIL § 4-kategorietiketter
    Given debtor "SKY-3022" has active fordringer in categories UNDERHOLDSBIDRAG_PRIVATRETLIG and ANDRE_FORDRINGER
    And a sagsbehandler is authenticated with access to debtor "SKY-3022"
    When the sagsbehandler navigates to the dækningsrækkefølge view for debtor "SKY-3022"
    Then the portal displays a ranked list ordered by GIL § 4 priority
    And each row shows the translated Danish category label (e.g. "Underholdsbidrag — privatretlig")
    And each row shows the cost component type in Danish (e.g. "Opkrævningsrenter", "Inddrivelsesrenter § 9, stk. 1", "Hovedfordring")
    And the view is read-only (no dækning actions available)
    And the view is reachable from the debtor's case overview page

  Scenario: Sagsbehandlerportal markerer opskrivningsfordringer med link til stamfordring
    Given debtor "SKY-3023" has a fordring "FDR-30231" with an associated opskrivningsfordring "FDR-30232"
    And a sagsbehandler navigates to the dækningsrækkefølge view for debtor "SKY-3023"
    Then the row for "FDR-30232" is visually marked as an opskrivningsfordring
    And the row displays a reference linking "FDR-30232" to its parent "FDR-30231"
    And the row for "FDR-30232" appears immediately after the row for "FDR-30231" in the list
