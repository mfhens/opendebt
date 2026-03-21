# Sagsbehandlerportalen

Velkommen til vejledningen for **Sagsbehandlerportalen** i OpenDebt.

Sagsbehandlerportalen er arbejdsværktøjet for sagsbehandlere i Gældsstyrelsen, der håndterer inddrivelse af fordringer.

## Hvad kan du som sagsbehandler?

- **Administrere sager** og fordringer
- **Vurdere indrivelsesparathed** for nye fordringer
- **Iværksætte inddrivelsesskridt** (modregning, lønindeholdelse, udlæg)
- **Håndtere indsigelser** fra skyldnere
- **Se bogføring og tidslinje** for fordringer
- **Administrere hæftelsesforhold** for skyldnere
- **Administrere forretningskonfiguration** — rentesatser, gebyrer og tærskler (kræver rollen CONFIGURATION_MANAGER eller ADMIN)

## Vejledninger

| Emne | Beskrivelse |
|------|-------------|
| [Sagsoversigt](sagsoversigt.md) | Sagshåndtering og tildeling |
| [Fordringer](fordringer.md) | Fordringsoversigt og detaljer |
| [Inddrivelsesskridt](inddrivelse.md) | Modregning, lønindeholdelse og udlæg |
| [Hæftelse](haeftelse.md) | Skyldnerforhold og hæftelsestyper |
| [Indsigelseshåndtering](indsigelse.md) | Behandling af indsigelser |
| [Bogføring og tidslinje](bogfoering.md) | Finansiel historik og audit trail |
| [Konfiguration](konfiguration.md) | Tidsversionerede forretningsværdier (renter, gebyrer, tærskler) |

## Roller og adgang

| Rolle | Adgang |
|-------|--------|
| `CASEWORKER` | Sager, fordringer, bogføring, indsigelser — **læseadgang** til konfiguration |
| `SENIOR_CASEWORKER` | Som CASEWORKER, med udvidede rettigheder til godkendelse |
| `TEAM_LEAD` | Som SENIOR_CASEWORKER |
| `CONFIGURATION_MANAGER` | Fuld adgang til konfigurationsadministration (opret, godkend, afvis) |
| `ADMIN` | Fuld adgang til alle funktioner |

## Demo-brugere

I demotilstand kan du vælge en af følgende brugere:

| Navn | Rolle | Adgang |
|------|-------|--------|
| Anna Jensen | CASEWORKER | Sager og fordringer |
| Erik Sørensen | SENIOR_CASEWORKER | Sager og fordringer |
| Mette Larsen | TEAM_LEAD | Sager og fordringer |
| Bro Karsten | CONFIGURATION_MANAGER | Konfigurationsadministration |
| Lars Nielsen | CONFIGURATION_MANAGER | Konfigurationsadministration |
| System Administrator | ADMIN | Fuld adgang |


Velkommen til vejledningen for **Sagsbehandlerportalen** i OpenDebt.

Sagsbehandlerportalen er arbejdsværktøjet for sagsbehandlere i Gældsstyrelsen, der håndterer inddrivelse af fordringer.

## Hvad kan du som sagsbehandler?

- **Administrere sager** og fordringer
- **Vurdere indrivelsesparathed** for nye fordringer
- **Iværksætte inddrivelsesskridt** (modregning, lønindeholdelse, udlæg)
- **Håndtere indsigelser** fra skyldnere
- **Se bogføring og tidslinje** for fordringer
- **Administrere hæftelsesforhold** for skyldnere

## Vejledninger

| Emne | Beskrivelse |
|------|-------------|
| [Sagsoversigt](sagsoversigt.md) | Sagshåndtering og tildeling |
| [Fordringer](fordringer.md) | Fordringsoversigt og detaljer |
| [Inddrivelsesskridt](inddrivelse.md) | Modregning, lønindeholdelse og udlæg |
| [Hæftelse](haeftelse.md) | Skyldnerforhold og hæftelsestyper |
| [Indsigelseshåndtering](indsigelse.md) | Behandling af indsigelser |
| [Bogføring og tidslinje](bogfoering.md) | Finansiel historik og audit trail |
