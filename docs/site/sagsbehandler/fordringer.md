# Fordringer

Som sagsbehandler har du detaljeret adgang til alle fordringer i systemet.

## Fordringsoversigt

Fordringsoversigten viser alle fordringer med filtrering efter:

- Status (aktiv, høring, afskrevet, indfriet m.m.)
- Fordringstype
- Fordringshaver
- Beløbsinterval

## Fordringdetaljer

For hver fordring kan du se:

| Felt | Beskrivelse |
|------|-------------|
| Hovedstol | Oprindeligt skyldigt beløb |
| Udestående saldo | Restbeløb inkl. renter og gebyrer |
| Inddrivelsesrenter | Tilskrevne renter |
| Livscyklustilstand | Aktuel tilstand i livscyklussen |
| Indrivelsesparathed | Status for validering af indrivelsesparathed |
| Stamdata | De 22 PSRM-stamdata-felter |
| Hæftelser | Tilknyttede skyldnere og hæftelsestyper |

## Forældelsespanel

Fra fordringsdetaljen kan du åbne panelet **Forældelse** for den valgte fordring.

Panelet viser:

- **Status** for forældelse (Aktiv, Forældet eller Afventer indsigelse)
- **Aktuel frist** og eventuel udskydelsesdato
- **Fordringskompleks** med øvrige tilknyttede fordringer
- **Afbrydelseshistorik** med type, dato og juridisk reference
- **Tillægsfrister** med ny fristdato
- **Udfald og begrundelse** hvis en forældelsesindsigelse allerede er afgjort

Som `CASEWORKER` eller `ADMIN` kan du fra panelet:

- registrere en afbrydelseshændelse
- registrere en forældelsesindsigelse, når status er **Aktiv**
- vurdere en forældelsesindsigelse som **Gyldig** eller **Ugyldig**, når status er **Afventer indsigelse**

Brug panelet, når du skal dokumentere forældelsesforløb direkte på en konkret fordring uden at forlade sagsvisningen.

## Indrivelsesparathed

Før en fordring kan overdrages til inddrivelse, vurderes dens indrivelsesparathed:

1. **Strukturvalidering**: Er alle krævede felter korrekt udfyldt?
2. **Autorisationsvalidering**: Har fordringshaveren ret til at indsende denne type?
3. **Livscyklusvalidering**: Er fordringen i en gyldig tilstand?
4. **Indholdsvalidering**: Er data konsistente?

Du kan:

- **Godkende** en fordring der har bestået automatisk validering
- **Afvise** en fordring med begrundelse
- **Sende til høring** for yderligere vurdering

## Regulering

Du kan regulere fordringer på vegne af fordringshaveren:

- **Opskrivning**: Forøg hovedstolen
- **Nedskrivning**: Reducer hovedstolen
- **Tilbagekald**: Tilbagekald med årsagskode (KLAG, HENS, FEJL, ANDET)
