# Indsigelseshåndtering

Indsigelser er skyldnerens formelle protest mod en fordring. Som sagsbehandler er du ansvarlig for at vurdere og afgøre indsigelser.

## Registrering af indsigelse

En indsigelse registreres med:

- Den berørte fordring
- Skyldnerens identifikation
- Registreringstidspunkt
- Årsag/grundlag for indsigelsen

## Virkning af aktiv indsigelse

Når en indsigelse er aktiv:

- **Inddrivelse stoppes** for den pågældende fordring
- Fordringen markeres som blokeret/under indsigelse
- Ingen nye inddrivelsesskridt kan iværksættes
- Eksisterende inddrivelsesskridt sættes på pause

## Behandling

Vurder indsigelsen ud fra:

1. Er grundlaget gyldigt?
2. Er der dokumentation der understøtter indsigelsen?
3. Skal fordringshaveren høres?

## Afgørelse

En indsigelse afgøres med et af to udfald:

### Indsigelse godkendt

- Fordringen justeres eller ophæves
- Skyldneren underrettes om afgørelsen
- Eventuelle dækninger kan reverseres

### Indsigelse afvist

- Inddrivelse genoptages automatisk
- Skyldneren underrettes om afgørelsen og klagemuligheder
- Fordringens livscyklus fortsætter normalt

## KLAG-workflow

Ved klage med opsættende virkning:

1. Fordringshaveren tilbagekalder fordringen med årsagskode **KLAG**
2. Fordringen låses
3. Klagesagen behandles
4. Ved medhold (FEJL): Dækninger reverseres, renter nulstilles, fordringen låses permanent
5. Ved afvisning: Fordringen kan genindsedes via GenindsendFordring

## Audit trail

Alle handlinger i forbindelse med indsigelser registreres i audit trail:

- Registreringstidspunkt
- Sagsbehandler der registrerede
- Statusændringer
- Afgørelsestidspunkt og udfald
- Begrundelse
