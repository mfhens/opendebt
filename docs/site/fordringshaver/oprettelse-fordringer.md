# Oprettelse af fordringer

Denne vejledning beskriver, hvordan du opretter og indsender fordringer til inddrivelse via Fordringshaverportalen.

## Krav til en fordring

Alle fordringer skal opfylde de generelle krav for at blive modtaget til inddrivelse. En fordring skal som minimum indeholde:

| Felt | Beskrivelse | Påkrævet |
|------|-------------|----------|
| Hovedstol | Det oprindelige skyldige beløb i DKK | Ja |
| Skyldner-ID | CPR- eller CVR-nummer på skyldneren | Ja |
| Fordringstype | Kode for fordringens art (f.eks. SKAT, TOLD) | Ja |
| Forfaldsdato | Dato hvor fordringen forfaldt til betaling | Ja |
| Betalingsfrist | Sidste dato for rettidig betaling | Nej (standard: forfaldsdato) |
| Forældelsesfrist | Dato for fordringens forældelse | Nej |
| OCR-linje | Betalingsidentifikation til automatisk afstemning | Nej |
| Periodestart/-slut | Den periode fordringen vedrører | Nej |
| Stiftelsesdato | Dato for fordringens opståen | Nej |
| Bemærkninger | Fritekst-bemærkninger til sagsbehandler | Nej |

## Oprettelsesproces

### Trin 1: Udfyld fordringsskemaet

I portalen vælges "Opret ny fordring". Udfyld de påkrævede felter og eventuelle valgfrie felter.

### Trin 2: Validering

Systemet validerer automatisk fordringen mod fire regelsæt:

1. **Strukturvalidering** -- Er alle påkrævede felter udfyldt? Er beløb positive? Er datoer gyldige?
2. **Autorisationsvalidering** -- Har du som fordringshaver rettigheder til at indsende denne fordringstype?
3. **Livscyklusvalidering** -- Er fordringen i en gyldig tilstand for indsendelse?
4. **Indholdsvalidering** -- Er felterne indbyrdes konsistente (f.eks. forfaldsdato efter stiftelsesdato)?

### Trin 3: Indsendelse

Når valideringen er bestået, indsendes fordringen. Resultatet kan være:

| Resultat | Betydning |
|----------|-----------|
| **Udført** | Fordringen er modtaget og registreret til inddrivelse |
| **Afvist** | Fordringen blev afvist pga. valideringsfejl -- se fejlbeskederne |
| **Høring** | Fordringen kræver yderligere vurdering og er sat til høring |

### Trin 4: Kvittering

Efter indsendelse modtages en kvittering med:

- Fordringens ID
- Sags-ID (hvis en sag blev oprettet)
- Status (udført, afvist eller høring)
- Eventuelle fejlbeskeder ved afvisning

## Skriftlig underretning til skyldner

Inden overdragelse til inddrivelse skal der være givet **skriftlig underretning** til skyldneren. Dette gælder også:

- Alle medhæftere (solidarisk hæftelse)
- Ved opskrivning af en eksisterende fordring

Det er fordringshaveren eller opkræveren, der er ansvarlig for denne underretning.

## Tips

!!! tip "Brug OCR-linjer"
    Hvis du anvender OCR-linjer, skal disse være unikke pr. fordring. Det gør automatisk betalingsafstemning mulig.

!!! warning "Kontroller forældelsesfrist"
    Angiv altid den korrekte forældelsesfrist. Fordringer der når deres forældelsesfrist bliver automatisk afskrevet.
