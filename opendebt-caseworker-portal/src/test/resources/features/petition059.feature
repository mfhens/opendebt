@petition059
Feature: Forældelse — sagsbehandlerportal limitation panel (P059)
  # Module scope: opendebt-caseworker-portal.limitation-panel (FR-7)
  # Canonical source: petitions/petition059-foraeldelse.feature
  # Petition: petitions/petition059-foraeldelse.md
  # Outcome contract: petitions/petition059-foraeldelse-outcome-contract.md
  # Architecture: design/solution-architecture-p059-foraeldelse.md

  Scenario: FR-7.1 Sagsbehandlerportalen viser forældelsesstatus med ISO-dato og udskydelse
    Given en sagsbehandler er autentificeret med rollen "CASEWORKER"
    And fordringen "FDR-59090" har følgende forældelsesstatus:
      | status              | ACTIVE      |
      | currentFristExpires | 2027-03-15  |
      | udskydelseDato      | 2021-11-20  |
      | isInUdskydelse      | false       |
    When sagsbehandleren navigerer til detaljevisningen for fordringen "FDR-59090"
    Then vises forældelsesstatus-panelet med:
      | Status               | Aktiv       |
      | Frist udløber        | 2027-03-15  |
      | Udskydelsesdato      | 2021-11-20  |
      | I udskydelsesvindue  | Nej         |


  Scenario: FR-7.2 Sagsbehandlerportalen viser afbrydelseshistorik med resulting new frist
    Given en fordring "FDR-59091" har to registrerede afbrydelseshændelser
    When sagsbehandleren navigerer til detaljevisningen for fordringen "FDR-59091"
    Then vises afbrydelseshistorik-tabellen med 2 rækker i kronologisk rækkefølge
    And indeholder hver række type, dato, juridisk reference og resulting new frist


  Scenario: FR-7.3 Sagsbehandlerportalen viser tillægsfristhistorik og fordringskompleks-medlemskab
    Given fordringen "FDR-59092" er medlem af kompleks "K-010" med medlemmet "FDR-59093"
    And fordringen "FDR-59092" har en registreret tillægsfrist
    And fordringen "FDR-59092" har en propageret afbrydelseshændelse med "sourceFordringId" = "FDR-59093"
    And fordringen "FDR-59092" har en propageret afbrydelseshændelse med "targetFordringId" = "FDR-59092"
    When sagsbehandleren navigerer til detaljevisningen for fordringen "FDR-59092"
    Then vises afsnittet "Fordringskompleks" i panelet
    And listes fordringen "FDR-59093" som medlem af komplekset
    And vises tillægsfristhistorikken med type, dato, extension og ny frist
    And vises "sourceFordringId" = "FDR-59093" for den propagerede afbrydelseshændelse
    And vises "targetFordringId" = "FDR-59092" for den propagerede afbrydelseshændelse


  Scenario: FR-7.4 Sagsbehandler med skriveadgang ser knap til registrering af forældelsesindsigelse
    Given en fordring "FDR-59094" har status "ACTIVE"
    And sagsbehandleren har rolle "CASEWORKER" med skriveadgang
    When sagsbehandleren navigerer til detaljevisningen for fordringen "FDR-59094"
    Then vises knappen "Registrer forældelsesindsigelse"


  Scenario: FR-7.5 Afventende indsigelse viser evalueringsformular med rationalefelt
    Given en fordring "FDR-59095" har status "INDSIGELSE_PENDING"
    When sagsbehandleren navigerer til detaljevisningen for fordringen "FDR-59095"
    Then vises evalueringsformularen med valg for "Gyldig" og "Ugyldig"
    And vises et tekstfelt til "rationale"
    And er registreringsknappen ikke tilgængelig


  Scenario: FR-7.6 Forældet fordring viser udfald og rationale uden registreringsknap
    Given en fordring "FDR-59096" har status "FORAELDET"
    And den seneste indsigelse blev vurderet som "VALID" med rationale "Forældelsesfrist udløb 2023-11-21"
    When sagsbehandleren navigerer til detaljevisningen for fordringen "FDR-59096"
    Then vises udfaldet "Forældet"
    And vises rationalet "Forældelsesfrist udløb 2023-11-21"
    And vises knappen "Registrer forældelsesindsigelse" ikke


  Scenario: FR-7.7 Read-only caseworker ser panelet men ingen skrivehandlinger
    Given en sagsbehandler er autentificeret med læseadgang til fordringen "FDR-59097"
    And fordringen "FDR-59097" har status "ACTIVE"
    When sagsbehandleren navigerer til detaljevisningen for fordringen "FDR-59097"
    Then vises forældelsesstatus-panelet
    And kan sagsbehandleren ikke registrere afbrydelseshændelser
    And kan sagsbehandleren ikke registrere eller evaluere indsigelser

