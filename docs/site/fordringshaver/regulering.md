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

### Årsagskode for nedskrivning

Når du foretager en nedskrivning i portalen, skal du vælge én af følgende årsagskoder:

| Kode | Dansk betegnelse | Hvornår bruges den? |
|------|-----------------|---------------------|
| `NED_INDBETALING` | Indbetaling modtaget direkte | Skyldneren har betalt direkte til fordringshaveren, og beløbet skal fratrækkes fordringen. |
| `NED_FEJL_OVERSENDELSE` | Fejl ved oversendelse | Fordringen er oversendt med et forkert beløb, og der korrigeres for oversendelsesfejlen. |
| `NED_GRUNDLAG_AENDRET` | Grundlag ændret | Det juridiske eller faktiske grundlag for fordringen er ændret, f.eks. ved klagenævnsafgørelse. |

Årsagskoderne er fastsat i gæld.bekendtg. § 7 stk. 2. Valget er obligatorisk — nedskrivning kan ikke indsendes uden en årsagskode.

!!! note "RIM-interne koder ikke tilgængelige"
    Koderne DINDB, OMPL og AFSK er interne RIM-koder (G.A.2.3.4.4) og kan ikke vælges i fordringshaverportalen. De håndteres udelukkende af PSRM/RIM internt.

### Tilbagedateret nedskrivning

Hvis du angiver en virkningsdato der ligger før dags dato, viser portalen automatisk en vejledning om, at nedskrivningen er tilbagedateret. En tilbagedateret nedskrivning kan påvirke allerede beregnede inddrivelsesrenter og posteringer.

### Tværgående indefrysningsadvisering

Når nedskrivningen er godkendt og kvitteringssiden vises, kan der fremgå en advisering om, at nedskrivningen har udløst en indefrysning i et andet system (GIL § 18 k). Adviseringen vises kun, når debt-service returnerer `crossSystemRetroactiveApplies = true` i kvitteringssvaret.

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
