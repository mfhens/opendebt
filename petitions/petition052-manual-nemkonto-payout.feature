Feature: Manuel NemKonto-udbetaling med 4-øjne-princip
  Som sagsbehandler i OpenDebt
  Ønsker jeg at kunne initiere en manuel udbetaling af en kreditsaldo til skyldnerens NemKonto
  Så at overskydende betalinger tilbagebetales via den lovpligtige kanal med korrekt godkendelse

  # ─────────────────────────────────────────────────────────────────────────────
  # Background: standard testsag med kreditsaldo
  # ─────────────────────────────────────────────────────────────────────────────

  Background:
    Given der eksisterer en sag "SAG-1052" med fordring "FORD-9001"
    And skyldneren er identificeret ved person_id "person-uuid-1001"
    And skyldneren er IKKE markeret som PEP eller VIP i person-registret
    And fordringen har en positiv kreditsaldo på 15 000,00 DKK i betalingsservicens bogholderi
    And konfigurationsnøglen "PAYOUT_SMALL_THRESHOLD" er aktiv med værdien 10 000,00 DKK
    And konfigurationsnøglen "PAYOUT_LARGE_THRESHOLD" er aktiv med værdien 100 000,00 DKK
    And sagsbehandler "alice" er logget ind med rollen CASEWORKER
    And supervisor "bob" er tilgængelig med rollen SUPERVISOR
    And direktør "eva" er tilgængelig med rollen DIRECTOR

  # ─────────────────────────────────────────────────────────────────────────────
  # A: Visning af "Ny udbetaling"-knap
  # ─────────────────────────────────────────────────────────────────────────────

  Scenario: Sagsbehandler ser "Ny udbetaling"-knap ved positiv kreditsaldo
    Given sagsbehandler "alice" er på detaljesiden for sag "SAG-1052"
    When siden indlæses
    Then vises knappen "Ny udbetaling"

  Scenario: "Ny udbetaling"-knap er fraværende ved nul-saldo
    Given fordringen på sag "SAG-1052" har en kreditsaldo på 0,00 DKK
    When sagsbehandler "alice" indlæser detaljesiden for sag "SAG-1052"
    Then vises knappen "Ny udbetaling" IKKE

  Scenario: "Ny udbetaling"-knap er fraværende ved debitsaldo
    Given fordringen på sag "SAG-1052" har en negativ saldo (skyldner skylder stadig penge)
    When sagsbehandler "alice" indlæser detaljesiden for sag "SAG-1052"
    Then vises knappen "Ny udbetaling" IKKE

  # ─────────────────────────────────────────────────────────────────────────────
  # B: Valideringer ved initiering
  # ─────────────────────────────────────────────────────────────────────────────

  Scenario: Udbetalingsformular viser korrekt tilgængelig kreditsaldo
    When sagsbehandler "alice" klikker på "Ny udbetaling" for sag "SAG-1052"
    Then vises formularen med kreditsaldo "15.000,00 DKK" som skrivebeskyttet felt
    And beløbsfeltets maksimumværdi er 15 000,00 DKK

  Scenario: Beløb på nul afvises
    When sagsbehandler "alice" udfylder udbetalingsformularen med beløb 0,00 DKK og begrundelse "Test"
    And indsender formularen
    Then vises valideringsfejlen "Beløb skal være større end 0 kr."
    And oprettes ingen PayoutRequest

  Scenario: Beløb der overstiger kreditsaldoen afvises
    When sagsbehandler "alice" udfylder formularen med beløb 20 000,00 DKK og begrundelse "Test"
    And indsender formularen
    Then vises valideringsfejlen "Beløbet overstiger den tilgængelige kreditsaldo."
    And oprettes ingen PayoutRequest

  Scenario: Tom begrundelse afvises
    When sagsbehandler "alice" udfylder formularen med beløb 5 000,00 DKK og tom begrundelse
    And indsender formularen
    Then vises valideringsfejlen "Begrundelse er påkrævet."
    And oprettes ingen PayoutRequest

  Scenario: Vellykket initiering opretter PayoutRequest med status PENDING_APPROVAL
    When sagsbehandler "alice" udfylder formularen med beløb 5 000,00 DKK og begrundelse "Overskydende betaling tilbagebetales"
    And indsender formularen
    Then oprettes en PayoutRequest med status "PENDING_APPROVAL"
    And PayoutRequest indeholder felterne: payoutId, caseId, debtId, personId, amount, currency "DKK", requestedBy "alice", begrundelse
    And PayoutRequest indeholder IKKE NemKonto-kontonummer eller andre personoplysninger ud over personId
    And approvalTier er sat til "SMALL"

  # ─────────────────────────────────────────────────────────────────────────────
  # C: Godkendelsesniveauer (tier-routing)
  # ─────────────────────────────────────────────────────────────────────────────

  Scenario: Lille udbetaling (≤ PAYOUT_SMALL_THRESHOLD) tildeles tier SMALL
    When sagsbehandler "alice" initierer en udbetaling på 5 000,00 DKK
    Then er approvalTier "SMALL"
    And enhver anden CASEWORKER (ikke "alice") er berettiget til at godkende

  Scenario: Udbetaling præcis på PAYOUT_SMALL_THRESHOLD-grænsen tildeles tier SMALL
    When sagsbehandler "alice" initierer en udbetaling på 10 000,00 DKK
    Then er approvalTier "SMALL"

  Scenario: Stor udbetaling (> PAYOUT_SMALL_THRESHOLD og ≤ PAYOUT_LARGE_THRESHOLD) tildeles tier LARGE
    When sagsbehandler "alice" initierer en udbetaling på 50 000,00 DKK
    Then er approvalTier "LARGE"
    And kun brugere med rollen SUPERVISOR er berettigede til at godkende

  Scenario: Udbetaling over PAYOUT_LARGE_THRESHOLD tildeles tier LARGE
    Given fordringen har en kreditsaldo på 200 000,00 DKK
    When sagsbehandler "alice" initierer en udbetaling på 150 000,00 DKK
    Then er approvalTier "LARGE"
    And kun brugere med rollen SUPERVISOR er berettigede til at godkende

  Scenario: PEP-skyldner eskaleres til tier PEP_VIP uanset beløb — lille beløb
    Given skyldneren med person_id "person-uuid-1001" er markeret som PEP i person-registret
    When sagsbehandler "alice" initierer en udbetaling på 500,00 DKK
    Then er approvalTier "PEP_VIP"
    And kun brugere med rollen DIRECTOR er berettigede til at godkende

  Scenario: VIP-skyldner eskaleres til tier PEP_VIP uanset beløb — stort beløb
    Given skyldneren med person_id "person-uuid-1001" er markeret som VIP i person-registret
    And fordringen har en kreditsaldo på 200 000,00 DKK
    When sagsbehandler "alice" initierer en udbetaling på 180 000,00 DKK
    Then er approvalTier "PEP_VIP"
    And kun brugere med rollen DIRECTOR er berettigede til at godkende

  Scenario: PEP/VIP-flag re-kontrolleres ved godkendelsestidspunktet
    Given sagsbehandler "alice" har initieret en SMALL-tier udbetaling på 5 000,00 DKK
    And skyldneren er efterfølgende blevet markeret som PEP i person-registret
    When sagsbehandler "charlie" (en anden CASEWORKER) forsøger at godkende udbetalingen
    Then afvises godkendelsen med fejlen "Skyldner er nu markeret som PEP/VIP — godkendelse kræver direktørrolle"
    And approvalTier opdateres til "PEP_VIP"
    And udbetalingen forbliver i status "PENDING_APPROVAL"

  Scenario: Manglende tærskler blokerer initiering
    Given ingen aktiv "PAYOUT_SMALL_THRESHOLD"-konfigurationspost eksisterer
    When sagsbehandler "alice" forsøger at initiere en udbetaling
    Then afvises initieringen med fejlen "Udbetalingsgrænser er ikke konfigureret — kontakt en konfigurationsansvarlig"
    And oprettes ingen PayoutRequest

  # ─────────────────────────────────────────────────────────────────────────────
  # D: Godkendelseskø — Udbetalingskø
  # ─────────────────────────────────────────────────────────────────────────────

  Scenario: Sagsbehandler ser kun SMALL-tier udbetalinger i kø (ikke egne)
    Given der er PENDING_APPROVAL-udbetalinger af tier SMALL (initieret af "alice"), LARGE og PEP_VIP
    When sagsbehandler "charlie" åbner Udbetalingskøen
    Then vises kun SMALL-tier udbetalingen initieret af "alice"
    And LARGE- og PEP_VIP-tier udbetalinger vises IKKE

  Scenario: Sagsbehandler ser ikke egne udbetalinger i kø
    Given sagsbehandler "alice" har initieret en SMALL-tier udbetaling på 5 000,00 DKK
    When "alice" åbner Udbetalingskøen
    Then vises "alices" egen udbetaling IKKE i køen

  Scenario: Supervisor ser SMALL- og LARGE-tier udbetalinger i kø
    Given der er PENDING_APPROVAL-udbetalinger af tier SMALL, LARGE og PEP_VIP
    When supervisor "bob" åbner Udbetalingskøen
    Then vises udbetalinger af tier SMALL og LARGE
    And PEP_VIP-tier udbetalinger vises IKKE

  Scenario: Direktør ser kun PEP_VIP-tier udbetalinger i kø
    Given der er PENDING_APPROVAL-udbetalinger af tier SMALL, LARGE og PEP_VIP
    When direktør "eva" åbner Udbetalingskøen
    Then vises kun PEP_VIP-tier udbetalingen
    And SMALL- og LARGE-tier udbetalinger vises IKKE

  Scenario: Køvisning viser påkrævede felter pr. post
    Given en PENDING_APPROVAL-udbetaling er i køen
    When en berettiget godkender ser køen
    Then viser hvert køelement: udbetaling-id, sagsreference, beløb i DKK, initieringsdato, initierende bruger, tier-badge og et "Gennemgå"-link

  # ─────────────────────────────────────────────────────────────────────────────
  # E: Godkendelsesflow — Lille udbetaling (Sagsbehandler godkender)
  # ─────────────────────────────────────────────────────────────────────────────

  Scenario: Sagsbehandler godkender lille udbetaling (SMALL tier)
    Given sagsbehandler "alice" har initieret en SMALL-tier udbetaling på 5 000,00 DKK for sag "SAG-1052"
    When sagsbehandler "charlie" (en anden CASEWORKER) åbner gennemgangssiden og klikker "Godkend"
    Then ændres udbetalingsstatus til "APPROVED"
    And NemKonto-udbetalingen igangsættes via integration-gateway
    And et "PAYOUT_APPROVED"-hændelse tilføjes til Tidslinje for sag "SAG-1052"

  Scenario: Sagsbehandler kan ikke godkende sin egen udbetaling — UI-niveau
    Given sagsbehandler "alice" har initieret en SMALL-tier udbetaling
    When "alice" navigerer til gennemgangssiden for sin egen udbetaling
    Then vises IKKE knapperne "Godkend" eller "Afvis"
    And en besked indikerer at "alice" er initierende sagsbehandler og ikke kan godkende

  Scenario: Sagsbehandler kan ikke godkende sin egen udbetaling — API-niveau
    Given sagsbehandler "alice" har initieret en SMALL-tier udbetaling
    When "alice" sender en godkendelsesanmodning til API'et
    Then returneres HTTP 403
    And udbetalingen forbliver i status "PENDING_APPROVAL"

  # ─────────────────────────────────────────────────────────────────────────────
  # F: Godkendelsesflow — Stor udbetaling (Supervisor godkender)
  # ─────────────────────────────────────────────────────────────────────────────

  Scenario: Supervisor godkender stor udbetaling (LARGE tier)
    Given sagsbehandler "alice" har initieret en LARGE-tier udbetaling på 50 000,00 DKK for sag "SAG-1052"
    When supervisor "bob" åbner gennemgangssiden og klikker "Godkend"
    Then ændres udbetalingsstatus til "APPROVED"
    And NemKonto-udbetalingen igangsættes via integration-gateway
    And et "PAYOUT_APPROVED"-hændelse tilføjes til Tidslinje for sag "SAG-1052"

  Scenario: Sagsbehandler (uden supervisorrolle) kan ikke godkende LARGE-tier udbetaling
    Given sagsbehandler "alice" har initieret en LARGE-tier udbetaling på 50 000,00 DKK
    When sagsbehandler "charlie" (kun CASEWORKER-rolle) forsøger at godkende
    Then returneres HTTP 403
    And udbetalingen forbliver i status "PENDING_APPROVAL"

  # ─────────────────────────────────────────────────────────────────────────────
  # G: Godkendelsesflow — PEP/VIP-udbetaling (Direktør godkender)
  # ─────────────────────────────────────────────────────────────────────────────

  Scenario: Direktør godkender PEP/VIP-udbetaling
    Given skyldneren er markeret som PEP i person-registret
    And sagsbehandler "alice" har initieret en PEP_VIP-tier udbetaling på 5 000,00 DKK for sag "SAG-1052"
    When direktør "eva" åbner gennemgangssiden og klikker "Godkend"
    Then ændres udbetalingsstatus til "APPROVED"
    And NemKonto-udbetalingen igangsættes via integration-gateway
    And et "PAYOUT_APPROVED"-hændelse tilføjes til Tidslinje for sag "SAG-1052"

  Scenario: Supervisor kan ikke godkende PEP_VIP-tier udbetaling
    Given skyldneren er markeret som PEP i person-registret
    And sagsbehandler "alice" har initieret en PEP_VIP-tier udbetaling på 5 000,00 DKK
    When supervisor "bob" forsøger at godkende
    Then returneres HTTP 403
    And udbetalingen forbliver i status "PENDING_APPROVAL"

  Scenario: Gennemgangsside viser PEP/VIP-indikator for direktøren
    Given skyldneren er markeret som PEP i person-registret
    And en PEP_VIP-tier udbetaling afventer direktørens godkendelse
    When direktør "eva" åbner gennemgangssiden
    Then vises en PEP/VIP-advarselindikator på siden
    And NemKonto-kontonummeret vises IKKE

  # ─────────────────────────────────────────────────────────────────────────────
  # H: Afvisningsflow
  # ─────────────────────────────────────────────────────────────────────────────

  Scenario: Afvisning uden årsag afvises
    Given en SMALL-tier udbetaling på 5 000,00 DKK afventer godkendelse
    When sagsbehandler "charlie" forsøger at afvise uden at angive afvisningsårsag
    Then vises valideringsfejlen "Afvisningsårsag er påkrævet."
    And udbetalingen forbliver i status "PENDING_APPROVAL"

  Scenario: Supervisor afviser LARGE-tier udbetaling med årsag
    Given sagsbehandler "alice" har initieret en LARGE-tier udbetaling på 50 000,00 DKK
    When supervisor "bob" angiver årsagen "Dokumentation mangler" og klikker "Afvis"
    Then ændres udbetalingsstatus til "REJECTED"
    And oprettes ingen posteringer i bogholderiet
    And fordringens kreditsaldo forbliver 15 000,00 DKK
    And et "PAYOUT_REJECTED"-hændelse tilføjes til Tidslinje
    And afvisningsårsagen vises i Tidslinje for interne roller (CASEWORKER, SUPERVISOR, DIRECTOR)

  Scenario: Direktør afviser PEP/VIP-udbetaling med årsag
    Given skyldneren er markeret som PEP i person-registret
    And sagsbehandler "alice" har initieret en PEP_VIP-tier udbetaling på 5 000,00 DKK
    When direktør "eva" angiver årsagen "Kræver yderligere compliance-vurdering" og klikker "Afvis"
    Then ændres udbetalingsstatus til "REJECTED"
    And et "PAYOUT_REJECTED"-hændelse tilføjes til Tidslinje

  # ─────────────────────────────────────────────────────────────────────────────
  # I: Annulleringsflow
  # ─────────────────────────────────────────────────────────────────────────────

  Scenario: Initierende sagsbehandler annullerer en afventende udbetaling
    Given sagsbehandler "alice" har initieret en SMALL-tier udbetaling der afventer godkendelse
    When "alice" angiver annulleringsårsagen "Forkert beløb angivet" og klikker "Annuller"
    Then ændres udbetalingsstatus til "CANCELLED"
    And fordringens kreditsaldo forbliver 15 000,00 DKK
    And et "PAYOUT_CANCELLED"-hændelse tilføjes til Tidslinje

  Scenario: Annullering uden årsag afvises
    Given sagsbehandler "alice" har initieret en afventende udbetaling
    When "alice" forsøger at annullere uden at angive årsag
    Then vises valideringsfejlen "Annulleringsårsag er påkrævet."
    And udbetalingen forbliver i status "PENDING_APPROVAL"

  Scenario: En anden sagsbehandler kan ikke annullere en kollegas udbetaling
    Given sagsbehandler "alice" har initieret en SMALL-tier udbetaling
    When sagsbehandler "charlie" forsøger at annullere via API'et
    Then returneres HTTP 403
    And udbetalingen forbliver i status "PENDING_APPROVAL"

  Scenario: Godkendt udbetaling kan ikke annulleres
    Given sagsbehandler "alice" har initieret en SMALL-tier udbetaling der er blevet godkendt (APPROVED)
    When "alice" forsøger at annullere udbetalingen
    Then afvises annulleringen med fejlen "Godkendte udbetalinger kan ikke annulleres"
    And udbetalingsstatus forbliver "APPROVED"

  # ─────────────────────────────────────────────────────────────────────────────
  # J: NemKonto-udbetaling og bogholderi
  # ─────────────────────────────────────────────────────────────────────────────

  Scenario: Godkendt udbetaling igangsætter NemKonto-udbetaling og opdaterer status til DISBURSED
    Given en SMALL-tier udbetaling på 5 000,00 DKK er godkendt af "charlie"
    When integration-gateway bekræfter gennemførelsen af NemKonto-udbetalingen
    Then ændres udbetalingsstatus til "DISBURSED"
    And disbursement-referencen (integration-gateways transaktions-id) gemmes på PayoutRequest
    And et "PAYOUT_DISBURSED"-hændelse tilføjes til Tidslinje

  Scenario: Vellykket udbetaling skaber dobbeltbogholderpostering
    Given en SMALL-tier udbetaling på 5 000,00 DKK er godkendt og udbetalt
    When posteringerne i bogholderiet inspiceres
    Then eksisterer en debetpostering på skyldners gældskonti
    And en kreditpostering på NemKonto-clearingskontoen
    And begge posteringer er dual-skrevet til immudb

  Scenario: NemKonto-kontonummer gemmes ikke i betalingsservicen
    Given en udbetaling er godkendt og udbetalt
    When PayoutRequest-posten og alle bogholderiposteringer inspiceres
    Then indeholder ingen post NemKonto-kontonummeret

  Scenario: Fejl ved NemKonto-udbetaling sætter status DISBURSEMENT_FAILED
    Given en SMALL-tier udbetaling på 5 000,00 DKK er godkendt
    And integration-gateway returnerer en fejl for NemKonto-kaldet
    When betalingsservicen modtager fejlen
    Then forbliver udbetalingsstatus "APPROVED" (ikke rullet tilbage)
    And registreres et "DISBURSEMENT_FAILED"-hændelse
    And et "PAYOUT_DISBURSEMENT_FAILED"-hændelse med rød advarselsbadge vises i Tidslinje

  Scenario: Afviste og annullerede udbetalinger igangsætter ingen NemKonto-kald
    Given en udbetaling har status "REJECTED" eller "CANCELLED"
    Then foretages intet kald til integration-gateway
    And oprettes ingen bogholderiposteringer

  # ─────────────────────────────────────────────────────────────────────────────
  # K: Tidslinje-integration (Petition 050)
  # ─────────────────────────────────────────────────────────────────────────────

  Scenario: PAYOUT_INITIATED-hændelse vises i Tidslinje ved initiering
    When sagsbehandler "alice" initierer en udbetaling på 5 000,00 DKK for sag "SAG-1052"
    Then tilføjes et "PAYOUT_INITIATED"-hændelse til Tidslinje for sag "SAG-1052"
    And hændelsen viser beløbet "5.000,00 DKK", tier-badge "SMALL" og statusbadge "Afventer godkendelse" (amber)

  Scenario: Alle interne udbetalingshændelser er skjulte for borgeren
    Given sag "SAG-1052" har hændelserne PAYOUT_INITIATED, PAYOUT_APPROVED og PAYOUT_REJECTED
    When borgeren (skyldneren) åbner Tidslinjen for sagen
    Then vises ingen af disse hændelser for borgeren

  Scenario: PAYOUT_DISBURSED-hændelse er synlig for borgeren
    Given en udbetaling er udbetalt (status DISBURSED) for sag "SAG-1052"
    When borgeren åbner Tidslinjen
    Then vises hændelsen "PAYOUT_DISBURSED" med beløbet og status "Udbetalt"
    And NemKonto-kontonummeret, begrundelsen og interne metadata vises IKKE

  Scenario: Statusbadges er korrekt farvekodede i Tidslinjen
    Given Tidslinje for sag "SAG-1052" indeholder udbetalingshændelser med varierende status
    Then vises amber badge for "PENDING_APPROVAL"
    And blå badge for "APPROVED"
    And grøn badge for "DISBURSED"
    And rød badge for "REJECTED" og "DISBURSEMENT_FAILED"
    And grå badge for "CANCELLED"

  # ─────────────────────────────────────────────────────────────────────────────
  # L: Konfiguration af tærskler (business_config)
  # ─────────────────────────────────────────────────────────────────────────────

  Scenario: Konfigurationsansvarlig opretter ny PAYOUT_SMALL_THRESHOLD-post
    Given en CONFIGURATION_MANAGER "config-alice" er logget ind
    When "config-alice" opretter en ny "PAYOUT_SMALL_THRESHOLD"-post med værdien 12 000,00 DKK gyldig fra "2026-07-01"
    Then oprettes posten med status "PENDING_REVIEW"
    And posten er synlig i Konfigurationsadministrations-UI'et (Petition 047)

  Scenario: Konfigurationsansvarlig kan ikke godkende sin egen tærskelposts
    Given "config-alice" har oprettet en "PAYOUT_SMALL_THRESHOLD"-post under PENDING_REVIEW
    When "config-alice" forsøger at godkende sin egen post
    Then afvises godkendelsen med fejlen "Du kan ikke godkende din egen konfiguration"
    And posten forbliver i status "PENDING_REVIEW"

  Scenario: Anden konfigurationsansvarlig godkender PAYOUT_LARGE_THRESHOLD-tærsklen
    Given "config-alice" har oprettet en "PAYOUT_LARGE_THRESHOLD"-post med værdien 120 000,00 DKK under PENDING_REVIEW
    When en anden CONFIGURATION_MANAGER "config-bob" godkender posten
    Then ændres postens status til "APPROVED"
    And tærsklen aktiveres fra den angivne valid_from-dato

  Scenario: LARGE-tærskel må ikke være mindre end SMALL-tærskel — valideringsfejl ved godkendelse
    Given den aktive "PAYOUT_SMALL_THRESHOLD" er 10 000,00 DKK
    And en "PAYOUT_LARGE_THRESHOLD"-post med værdien 8 000,00 DKK afventer godkendelse
    When "config-bob" forsøger at godkende "PAYOUT_LARGE_THRESHOLD"-posten
    Then afvises godkendelsen med fejlen "PAYOUT_LARGE_THRESHOLD skal være større end PAYOUT_SMALL_THRESHOLD"
    And posten forbliver i status "PENDING_REVIEW"

  Scenario: Begge tærskelnøgler er synlige i Konfigurationsadministrations-UI'et
    Given CONFIGURATION_MANAGER "config-alice" navigerer til konfigurationslisten (Petition 047)
    Then vises "PAYOUT_SMALL_THRESHOLD" med aktuel værdi og livscyklusstatus
    And "PAYOUT_LARGE_THRESHOLD" vises med aktuel værdi og livscyklusstatus

  # ─────────────────────────────────────────────────────────────────────────────
  # M: ROLE_DIRECTOR og adgangskontrol
  # ─────────────────────────────────────────────────────────────────────────────

  Scenario: Direktør kan logge ind og modtage ROLE_DIRECTOR i JWT
    When "director-user" logger ind med adgangskoden "director-password" i demo-miljøet
    Then indeholder JWT-adgangstokenet rollen "ROLE_DIRECTOR"

  Scenario: Direktør kan ikke oprette sager, ændre fordringer eller tilgå konfiguration
    Given direktør "eva" er autentificeret
    When "eva" forsøger at tilgå opret-sag-formularen, ændre en fordring eller Konfigurationsadministrations-UI'et
    Then returneres HTTP 403 for alle disse handlinger

  Scenario: Direktør har ikke adgang til immudb-revisions-UI'et
    Given direktør "eva" er autentificeret
    When "eva" navigerer til "/audit/ledger"
    Then vises en "Ingen adgang"-side eller omdirigeres til dashboardet

  # ─────────────────────────────────────────────────────────────────────────────
  # N: GDPR og dataisolering
  # ─────────────────────────────────────────────────────────────────────────────

  Scenario: PayoutRequest-post indeholder ingen PII ud over personId
    Given en udbetaling er oprettet for skyldneren med person_id "person-uuid-1001"
    When PayoutRequest-posten i betalingsservicens database inspiceres
    Then indeholder posten feltet personId med værdien "person-uuid-1001"
    And indeholder posten IKKE CPR-nummer, navn, adresse eller NemKonto-kontonummer

  Scenario: PEP/VIP-flag gemmes ikke på PayoutRequest
    Given skyldneren er markeret som PEP og en PEP_VIP-tier udbetaling er oprettet
    When PayoutRequest-posten inspiceres
    Then indeholder approvalTier "PEP_VIP"
    And indeholder ingen felter der hedder isPep, isVip eller lignende

  Scenario: NemKonto-kontonummer optræder ikke i logfiler
    Given en udbetaling er godkendt og udbetalt via integration-gateway
    When applikationslogfilerne for betalingsservicen og sagsbehandlerportalen inspiceres
    Then optræder NemKonto-kontonummeret IKKE i nogen loglinjer

  # ─────────────────────────────────────────────────────────────────────────────
  # O: Optimistisk låsning (race condition)
  # ─────────────────────────────────────────────────────────────────────────────

  Scenario: Samtidig godkendelse af samme udbetaling resulterer i præcis én succes
    Given en SMALL-tier udbetaling afventer godkendelse
    And to berettigede sagsbehandlere ("charlie" og "dave") åbner gennemgangssiden samtidigt
    When begge forsøger at godkende udbetalingen på samme tid
    Then godkendes udbetalingen præcis én gang
    And den anden godkendelsesanmodning returnerer fejlen "Udbetalingen er allerede behandlet af en anden bruger"
    And udbetalingen udbetales IKKE to gange
