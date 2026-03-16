---
source: https://gaeldst.dk/fordringshaver/find-vejledning/regulering-af-fordringer/opskriv-fordring
fetched: 2026-03-16
---

# Opskriv fordring

Opskrivningsfordring har samme stamdata som original, får samme fordrings-ID, og danner et **fordringskompleks**. Behandles i størst muligt omfang som én samlet fordring.

## Renter kan IKKE opskrives

Renter skal oversendes som ny relateret fordring med samme periode.

## Årsagskoder

### OpskrivRegulering
Opjuster fordringens saldo. Fx ved omposteringer der fjerner indbetaling fra opkrævningsfasen, eller fejlregistreret oprindeligt krav.

### NAOR (NedskrivningAnnulleretOpskrivningRegulering)
Annuller en fejlagtig opskrivning.

### FejlagtighovedstolIndberetning (ny aktionstype)
Ændrer den **oprindelige hovedstol** til højere beløb. Indberettet beløb er den nye hovedstol (ikke differencen). Påvirker IKKE saldo - brug OpskrivRegulering for saldo. Fordringens saldo kan ikke overstige hovedstol.

## Krav

- Oprindelig fordring og opskrivningsfordring skal kunne identificeres som to delfordringer
- Opskrivningsfordring får eget modtagelsestidspunkt
- Skriftlig underretning af skyldner påkrævet (rykket + underrettet)
- Inddrivelsesrenter beregnes selvstændigt fra 1. i måneden efter indsendelse
- Tilbagekald returnerer hele fordringskomplekset inkl. alle opskrivninger og renter
