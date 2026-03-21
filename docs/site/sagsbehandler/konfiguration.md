# Konfigurationsadministration

Konfigurationsadministrationssiden giver dig mulighed for at administrere **tidsversionerede forretningsværdier** — rentesatser, gebyrer og tærskler — der bruges i beregninger på tværs af OpenDebt.

Siden er tilgængelig via menupunktet **Konfiguration** i portalen.

!!! note "Rollekrav"
    - **Læseadgang**: alle sagsbehandlerroller (CASEWORKER, SENIOR_CASEWORKER, TEAM_LEAD)
    - **Skriveadgang**: kræver rollen `CONFIGURATION_MANAGER` eller `ADMIN`

---

## Konfigurationsoversigt

Oversigtssiden viser alle konfigurerede værdier grupperet i tre kategorier:

### Renter

| Nøgle | Beskrivelse | Retsgrundlag |
|-------|-------------|--------------|
| `RATE_NB_UDLAAN` | Nationalbankens udlånsrente | NB-meddelelse |
| `RATE_INDR_STD` | Standard inddrivelsesrente (NB + 4 procentpoint) | § 5 renteloven |
| `RATE_INDR_TOLD` | Told inddrivelsesrente (NB + 2 procentpoint) | § 7 renteloven |
| `RATE_INDR_TOLD_AFD` | Told afdragsrente (NB + 1 procentpoint) | § 8 renteloven |

### Gebyrer

| Nøgle | Beskrivelse |
|-------|-------------|
| `FEE_STANDARDGEBYR` | Standardgebyr i kroner |

### Tærskler

| Nøgle | Beskrivelse |
|-------|-------------|
| `THRESHOLD_MIN_INTEREST` | Minimumsrentegrænse — rente under denne grænse bogføres ikke |

### Statusbadges

Hver konfigurationspost vises med et statuskendingsmærke:

| Status | Farve | Betydning |
|--------|-------|-----------|
| **Aktiv** | Grøn | Gyldig i dag — bruges i beregninger |
| **Fremtidig** | Blå | Godkendt men træder ikke i kraft endnu |
| **Afventer godkendelse** | Gul | Oprettet men endnu ikke godkendt |
| **Udløbet** | Grå | Historisk post — ikke længere gyldig |

---

## Versionshistorik

Klik på en nøgle (f.eks. `RATE_INDR_STD`) for at se **detailsiden** med den fulde versionshistorik — alle versioner fra ældst til nyest med gyldighedsperiode, retsgrundlag og oprettelsesdato.

---

## Opret ny konfigurationsversion

1. Gå til oversigtssiden (`/konfiguration`)
2. Udfyld formularen i bunden af siden:
   - **Nøgle**: vælg fra dropdown-listen
   - **Værdi**: den numeriske eller tekstmæssige værdi
   - **Datatype**: DECIMAL, INTEGER, STRING eller BOOLEAN
   - **Gyldig fra**: den dato, værdien skal træde i kraft (skal være dags dato eller fremtidig)
   - **Gyldig til**: valgfrit slutdato
   - **Beskrivelse**: kort forklaring af ændringen
   - **Retsgrundlag**: lovparagraf eller anden autoritetsreference
3. Klik **Opret** — posten oprettes med status **Afventer godkendelse**

!!! warning "Krav til godkendelse"
    Nye poster er ikke aktive, før de er godkendt af en anden bruger med CONFIGURATION_MANAGER- eller ADMIN-rollen.

---

## NB-rente og afledte satser

Når du opretter en ny version af `RATE_NB_UDLAAN`, beregner systemet automatisk de tre afledte rentesatser:

| Afledt nøgle | Beregning |
|--------------|-----------|
| `RATE_INDR_STD` | NB-rente + 4,00 pp |
| `RATE_INDR_TOLD` | NB-rente + 2,00 pp |
| `RATE_INDR_TOLD_AFD` | NB-rente + 1,00 pp |

Brug **Forhåndsvisning**-panelet på detailsiden for `RATE_NB_UDLAAN` til at se de afledte værdier, inden du gemmer:

1. Angiv den nye NB-rente i %-feltet
2. Angiv startdato
3. Klik **Forhåndsvis** — systemet viser de beregnede afledte satser
4. Opret posten, hvis beregningerne er korrekte

De afledte satser oprettes automatisk som separate PENDING_REVIEW-poster, der alle skal godkendes.

---

## Godkendelse og afvisning

Poster med status **Afventer godkendelse** kan godkendes eller afvises:

- **Godkend**: klik `Godkend`-knappen — posten skifter til APPROVED og bliver aktiv på sin gyldig-fra-dato
- **Afvis**: klik `Afvis`-knappen — posten slettes permanent

!!! info "4-øjne-princip"
    Det anbefales at opretteren og godkenderen er to forskellige brugere.

---

## Sletning af fremtidige poster

Poster med status **Fremtidig** (godkendt, men ikke trådt i kraft endnu) kan slettes:

1. Gå til detailsiden for den pågældende nøgle
2. Find den fremtidige post i historiktabellen
3. Klik **Slet** og bekræft

Aktive og udløbne poster kan ikke slettes.

---

## Audit trail

Alle ændringer i konfiguration logges med:

- **Handling**: CREATE, UPDATE, APPROVE, REJECT, DELETE
- **Gammel/ny værdi**
- **Udført af**: bruger-ID
- **Tidspunkt**

Auditrapporten er tilgængelig via REST API (`GET /api/v1/config/{key}/audit`) og kræver CONFIGURATION_MANAGER- eller ADMIN-rollen.

---

## Virkning på renteberegning

Ændringer i rentesatser tager effekt fra `gyldig-fra`-datoen. Systemet anvender **daglig satsopløsning**:

- Hvert dag bruger den rentesats, der er aktiv på netop den dato
- Hvis rentesatsen skifter midt i en beregningsperiode (f.eks. ved tilbageberegning efter en krydsende transaktion), splittes perioden ved ændringsdatoen
- Hvert rentejournal-entry gemmer den præcise sats, der blev brugt

Se [Bogføring og tidslinje](bogfoering.md) for detaljer om rentetilskrivning og tilbageberegning.
