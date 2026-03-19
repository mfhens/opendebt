# Underretningsmeddelelser

Gældsstyrelsen sender underretningsmeddelelser til fordringshavere om status og hændelser vedrørende deres fordringer. Der er seks typer underretninger.

## Oversigt over underretningstyper

| Type | Frekvens | Indhold |
|------|----------|---------|
| **Afregning** | Månedlig (sidste hverdag) | Dækninger med CPR, beløb, dato, fordring |
| **Udligning** | Daglig | Saldobevægelser på fordringer |
| **Allokering** | Daglig | Afdrag/hovedstol/renter-opdeling |
| **Renter** | Månedlig | Tilskrevne renter eller detaljerede daglige renter |
| **Afskrivning** | Ved hændelse | Fordring der mister retskraft |
| **Tilbagesend** | Ved hændelse | Returnering af fordring til fordringshaver |

## Afregning

Den månedlige afregning udsendes den **sidste hverdag i hver måned** og indeholder:

- Skyldner-identifikation (CPR/CVR)
- Dækningsbeløb
- Dækningsdato
- Fordringsreference

Betaling sker til fordringshaveres NemKonto.

## Udligning

Den daglige udligningsmeddelelse sendes, når der er saldobevægelser på en fordring. Bruges til at holde styr på saldi for nedskrivning.

!!! note "Udligning eller allokering"
    En fordringshaver modtager enten udligning **eller** allokering -- ikke begge. Valget foretages ved aftaleindgåelse.

## Allokering

Allokeringsmeddelelsen ligner udligning, men viser desuden opdelingen mellem:

- Afdrag på hovedstol
- Dækning af renter

Denne underretning er kun tilgængelig system-til-system.

## Renter

Rentemeddelelsen viser tilskrevne inddrivelsesrenter. Kan modtages som:

- Månedlig opgørelse
- Detaljeret daglig opgørelse (kun system-til-system)

## Afskrivning

Afskrivningsmeddelelsen sendes, når en fordring mister retskraft. Typiske årsager:

- Forældelse
- Konkurs
- Dødsbo
- Gældssanering

## Tilbagesend/Returnering

Sendes når fordringshaveren anmoder om returnering af en fordring, eller Gældsstyrelsen returnerer en fordring af anden årsag.

## Hentning af underretninger

- **Portal**: Underretninger kan ses i Fordringshaverportalen under "Underretninger"
- **System-til-system**: Underretninger kan hentes via API

!!! warning "Opbevaring"
    Underretninger ældre end **3 måneder** kan ikke hentes. Sørg for at hente og arkivere underretninger løbende.
