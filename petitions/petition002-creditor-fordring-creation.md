# Petition 002: Creditor creation of a new fordring

## Summary

OpenDebt shall allow a fordringshaver to create a new fordring primarily via API and, for small fordringshavere, manually via the fordringshaverportal. API creation requires a valid OCES3 certificate, while portal creation requires MitID Erhverv login and is limited to the fordringshaver linked to the logged-in identity. Every submitted fordring shall be checked against the rules for inddrivelsesparathed; if the fordring is not inddrivelsesparat, the fordringshaver shall receive an error message stating the reason. If the fordring is inddrivelsesparat, OpenDebt shall create a new debt post for the person or company and keep bookkeeping updated.

## Context and motivation

OpenDebt needs a standard creditor submission flow for new fordringer. The default flow is system-to-system creation via API for fordringshavere that integrate directly with OpenDebt. In addition, small fordringshavere need a manual submission path through the fordringshaverportal.

Both channels must enforce the correct authentication and access model:

- API access is certificate-based using OCES3
- Portal access is user-based using MitID Erhverv
- Portal users may act only for the fordringshaver their MitID Erhverv identity is linked to

Before a submitted fordring can become an active debt in OpenDebt, it must pass the rules for inddrivelsesparathed. Submissions that fail this readiness check must be rejected with a reason. Submissions that pass must create the debt post and keep bookkeeping aligned with the creation.

## Functional requirements

1. A fordringshaver shall be able to create a new fordring via the API.
2. API creation shall be the default way for a fordringshaver to create a new fordring.
3. Small fordringshavere shall also be able to create a new fordring manually via the fordringshaverportal.
4. API creation shall require a valid OCES3 certificate.
5. Portal creation shall require MitID Erhverv login.
6. In the fordringshaverportal, a user shall be allowed to create fordringer only for the fordringshaver linked to that user’s MitID Erhverv identity.
7. Every submitted fordring shall be evaluated against the rules to determine whether it is inddrivelsesparat.
8. If a submitted fordring is not inddrivelsesparat, OpenDebt shall reject the creation and return an error message that states the reason.
9. If a submitted fordring is inddrivelsesparat, OpenDebt shall create a new debt post for the relevant person or company.
10. When a new debt post is created from an inddrivelsesparat fordring, bookkeeping shall be updated accordingly.

## Constraints and assumptions

- The API is the primary creditor submission channel; the portal is a supplementary manual channel.
- This petition does not define how a fordringshaver is classified as “small”.
- This petition does not define the detailed payload fields for creating a fordring.
- This petition does not define the technical format of the returned error message beyond requiring that it states the reason.
- “Keep bookkeeping updated” is intentionally kept at a high level and does not define accounts, postings, or booking sequence.
- If authentication, access control, or inddrivelsesparathed validation fails, no debt post is created.

## PSRM Reference Context

The following reference material from Gældsstyrelsen's PSRM documentation provides concrete domain constraints that inform the implementation of fordring creation in OpenDebt.

**Source:** [Generelle krav til fordringer](https://gaeldst.dk/fordringshaver/find-vejledning/generelle-krav-til-fordringer)

### Stamdata requirements

PSRM requires or supports the following 22 stamdata fields when creating/submitting a fordring:

| # | Stamdatafelt | Obligatorisk / Valgfri | Notes |
|---|---|---|---|
| 1 | Beløb | Obligatorisk | Fordringens restgæld ved overdragelse |
| 2 | Hovedstol | Obligatorisk | Fordringens oprindelige pålydende |
| 3 | Fordringshaver | Obligatorisk | Kreditor for fordringen |
| 4 | Fordringshaver Reference | Obligatorisk | Unikt referencenummer (sagsnummer + evt. løbenummer) |
| 5 | Fordringsart | Obligatorisk | INDR (inddrivelse) eller MODR (kun modregning). Kun INDR i PSRM |
| 6 | Fordringstype (kode) | Obligatorisk | Kode der beskriver retligt grundlag (fx PSRESTS = restskat) |
| 7 | HovedfordringsID | Obligatorisk | Knytter relateret fordring til hovedfordring |
| 8 | Identifikation af skyldner | Obligatorisk | CPR, CVR/SE eller AKR nummer |
| 9 | Forældelsesdato | Obligatorisk | Sidste dag fordringen er retskraftig; skal afspejle aktuel dato ved oversendelse |
| 10 | Beskrivelse | Afhængig af fordringstype | Fritekst max 100 tegn; medtages i breve til skyldner |
| 11 | Fordringsperiode | Afhængig af fordringstype | Start- og sluttidspunkt; perioder defineret af lovgivning |
| 12 | Stiftelsesdato | Afhængig af fordringstype | Tidspunkt for retsstiftende begivenhed |
| 13 | Forfaldsdato | Afhængig af fordringstype | Tidligste tidspunkt fordring kan kræves betalt |
| 14 | Sidste rettidige betalingsdato (SRB) | Afhængig af fordringstype | Seneste betalingstidspunkt uden misligholdelse; henstand/betalingsordning ændrer SRB, rykkerskrivelser udskyder IKKE SRB |
| 15 | Bobehandling | Valgfri (S2S) / Obligatorisk (portal) | Om fordring er omfattet af bobehandling |
| 16 | Domsdato | Valgfri | Dato for domsafsigelse (kun ved forældelseslovens §5 stk 1 nr 3) |
| 17 | Forligsdato | Valgfri | Dato for forligsaftale |
| 18 | Rentevalg | Valgfri | Renteregel, rentesatskode, rentesats |
| 19 | Fordringsnote | Valgfri | Sagsrelevante bemærkninger til Gældsstyrelsen |
| 20 | Fordringsdokumenter | Valgfri | Dokumentation for fordringens eksistens |
| 21 | Kundenote | Valgfri | Information om skyldner |
| 22 | P-nummer | Valgfri | Produktionsenhed/lokation |

**GDPR constraint on Beskrivelse:** The field is max 100 characters and must NOT contain personal data (CPR, names, addresses) of persons other than the skyldner. Only the first 6 digits of another person's CPR may be included if required by the aftalegrundlag.

### Pre-submission requirements

Before a fordring may be submitted for inddrivelse, the following conditions must be met:

1. **Sædvanlig rykkerprocedure** must have been completed without result (forgæves)
2. **Skriftlig underretning** to the skyldner is required before overdragelse — may be embedded in the opkrævning/rykkerskrivelse
3. **Hovedfordringer must be separated** from renter og lignende ydelser (rente, provisioner, gebyrer) — each submitted as separate fordringer in their correct fordringstype
4. **Fordringer must be submitted individually** — two or more fordringer must NOT be aggregated into one; fordringer with different lovgrundlag must not be mixed (affects forældelsesfrist)

### Submission outcomes

When a fordring is received, the system returns a **kvittering** containing:

- The assigned **fordrings-ID**
- Any **hæftelsesforhold** and **AKR-nummer**
- A **slutstatus**:
  - **UDFØRT** — fordring accepted and received for inddrivelse
  - **AFVIST** — fordring rejected (failed validation)
  - **HØRING** — stamdata deviates from indgangsfilter rules; claim enters the hearing workflow (see below)

### Hearing workflow (fordringer i høring)

When stamdata deviates from the indgangsfilter rules, the fordring enters **høring** rather than being accepted or rejected outright:

- The fordring is placed in a waiting position under "Fordringer i høring" in the fordringshaverportal with status **"Afventer fordringshaver"**
- **The fordring is NOT received for inddrivelse** while in høring — fordringshaver's own forældelsesregler apply until stamdata is resolved
- The fordringshaver must either **approve** the submission (with written justification) or **withdraw** it (fortryd) and resubmit with corrected stamdata
- If approved, status changes to **"Afventer RIM"** and a Gældsstyrelsen caseworker decides: godkend, afvis, or tilpas indgangsfilter
- Gældsstyrelsen treats all fordringer i høring **within 14 days**

## Out of scope

- Detailed OCES3 certificate onboarding, issuance, and lifecycle management
- Detailed MitID Erhverv federation, session, and identity resolution behavior
- The detailed business rules that determine inddrivelsesparathed
- Portal UI design and form layout
- Detailed bookkeeping implementation, account mapping, and reconciliation behavior
