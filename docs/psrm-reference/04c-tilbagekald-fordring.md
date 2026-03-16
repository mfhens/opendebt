---
source: https://gaeldst.dk/fordringshaver/find-vejledning/regulering-af-fordringer/tilbagekald-fordring
fetched: 2026-03-16
---

# Tilbagekald fordring

Tilbagekald af hovedfordring medfører automatisk tilbagekald af relaterede fordringer. Relaterede fordringer kan tilbagekaldes selvstændigt.

Fordringer med statsrefusion kan som udgangspunkt IKKE tilbagekaldes (undtaget: FEJL, KLAG).

## Årsagskoder

### BORD (Betalingsordning)
- Betalingsordning indgået med skyldner efter indsendelse
- Ingen virkningsdato (sættes automatisk til bogføringsdato)
- Dækninger hæves IKKE
- Inddrivelsesrenter beregnes til virkningsdato, bogføres ved tilbagekald
- Fordring låses; kan efterfølgende kun tilbagekaldes med FEJL
- Ikke for statsrefusion-fordringer

### BORT (Bortfald)
- For fordringstyper omfattet af bortfaldsregler (tvangsbøder etc.)
- Kræver virkningsdato
- Dækninger FØR virkningsdato fastholdes
- Restsaldo + inddrivelsesrenter returneres per virkningsdato
- Fordring låses; kan efterfølgende kun tilbagekaldes med FEJL

### FEJL
- Fordring er forkert eller burde ikke være oversendt
- Tilbagekalder fordringens **oprindelige saldo**
- Alle inddrivelsesrenter **nulstilles** fra modtagelsestidspunktet
- Alle dækninger **ophæves**
- Kan medføre negativ afregning (udlignes i kommende afregninger)
- Fordring låses permanent
- Kan IKKE bruge GenindsendFordring - skal oprettes som ny fordring

### HENS (Henstand)
- Fordringshaver giver skyldner henstand
- Virker som KLAG
- Ikke for statsrefusion-fordringer

### KLAG (Klage)
- Skyldner har klaget med opsættende virkning
- Ingen virkningsdato (sættes automatisk)
- Dækninger hæves IKKE
- Inddrivelsesrenter til virkningsdato, returneres ved tilbagekald
- Fordring låses
- Efter afgørelse: FEJL (skyldner får medhold) eller GenindsendFordring

## GenindsendFordring

- Angiv oprindeligt fordrings-ID
- Visse stamdata skal matche original (stiftelsesdato, forfaldsdato, periode)
- Får tildelt nyt fordrings-ID
- FEJL-tilbagekaldte fordringer kan IKKE genindsendes
- Genindsendete renter: kun relateret til oprindelig fordring; hovedstol = restsaldo ved genindsendelse
- Forældelsesdato: altid nyeste (fra underretning eller egen afbrydelse)

## Oversigt

| Årsagskode | Virkningsdato | Dækninger | Renter | Kan genindsendes |
|---|---|---|---|---|
| BORD | Auto (bogføring) | Fastholdes | Til virkningsdato | Kun via FEJL |
| BORT | Obligatorisk | Fastholdes (før dato) | Til virkningsdato | Kun via FEJL |
| FEJL | Modtagelsesdato | Ophæves alle | Nulstilles alle | Nej - ny fordring |
| HENS | Auto | Fastholdes | Til virkningsdato | Ja (GenindsendFordring) |
| KLAG | Auto | Fastholdes | Til virkningsdato | Ja (GenindsendFordring) |
