# Regulering af fordringer

Regulering dækker over ændringer til en eksisterende fordring: opskrivning, nedskrivning og tilbagekald.

## Opskrivning

En opskrivning øger fordringens hovedstol. Det kan f.eks. skyldes:

- Yderligere krav der er tilkommet
- Korrektion af det oprindelige beløb
- Tilskrevne renter fra fordringshaveren

**Krav:** Skyldneren skal underrettes skriftligt inden opskrivning.

## Nedskrivning

En nedskrivning reducerer fordringens hovedstol. Det kan f.eks. skyldes:

- Delvis betaling modtaget direkte af fordringshaveren
- Korrektion af det oprindelige beløb
- Aftalt eftergivelse

## Tilbagekald

Fordringshaveren kan tilbagekalde en fordring der er under inddrivelse. Ved tilbagekald skal der angives en årsagskode:

### Årsagskode KLAG

Anvendes når skyldneren har en klage med opsættende virkning.

- Virkningsdato sættes automatisk til bogføringsdato
- Dækninger (betalinger) bevares -- de tilbagebetales **ikke**
- Inddrivelsesrenter beregnes op til virkningsdato og returneres
- Fordringen låses efter tilbagekald
- Kan genindsedes via "Genindsend fordring"

### Årsagskode HENS

Anvendes når fordringshaveren giver skyldneren henstand.

- Fungerer identisk med KLAG
- **Ikke tilgængelig** for statsrefusions-fordringer
- Kan genindsedes via "Genindsend fordring"

### Årsagskode FEJL

Anvendes når fordringen er indsendt ved en fejl.

- Alle dækninger ophæves/reverseres
- Inddrivelsesrenter nulstilles fra modtagelsestidspunktet
- Fordringen låses permanent
- **Kan IKKE genindsedes** -- en ny fordring skal oprettes

### Årsagskode ANDET

Anvendes for øvrige tilbagekaldsårsager.

## Genindsendelse

Efter et tilbagekald med årsagskode KLAG eller HENS kan fordringen genindsedes:

1. Den oprindelige fordrings-ID skal angives
2. Visse stamdata skal matche den originale fordring: stiftelsesdato, forfaldsdato og periode
3. Den genindsendte fordring får et **nyt fordrings-ID**
4. Forældelsesfrist skal være den nyeste tilgængelige
5. Fordringer tilbagekaldt med FEJL kan **ikke** genindsedes

!!! warning "Nyt modtagelsestidspunkt"
    En genindsendt fordring får en ny plads i dækningsrækkefølgen (nyt modtagelsestidspunkt).
