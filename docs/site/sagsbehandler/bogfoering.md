# Bogfoering og tidslinje

OpenDebt bruger dobbelt bogholderi (double-entry bookkeeping) til at spore alle finansielle transaktioner med fuld audit trail.

## Tidslinjevisning

For hver fordring kan du se en komplet tidslinje med:

| Hændelse | Beskrivelse |
|----------|-------------|
| Registrering | Fordringen blev oprettet |
| Tilstandsændring | Livscyklustilstand ændret (f.eks. REGISTRERET -> RESTANCE) |
| Dækning | Betaling modtaget og allokeret |
| Rentetilskrivning | Inddrivelsesrenter tilskrevet |
| Regulering | Opskrivning, nedskrivning eller tilbagekald |
| Inddrivelsesskridt | Iværksat, afsluttet eller annulleret |
| Indsigelse | Registreret, afgjort |

## Bogføringsposter

Alle finansielle hændelser registreres som bogføringsposter med:

- **Debet-konto** og **kredit-konto**
- Beløb
- Valutadato
- Posteringstidspunkt
- Reference til udløsende hændelse
- Sagsbehandler/system der posterede

## Dækningsrækkefølge (GIL § 4)

Når en betaling modtages, allokeres den efter den lovpligtige dækningsrækkefølge i GIL § 4. Rækkefølgen gælder pr. skyldner og bestemmer hvilke fordringer og komponenter der dækkes først.

### De 5 prioritetskategorier (GIL § 4 stk. 1–4)

| Prioritet | Kategori | Kode |
|-----------|---------|------|
| 1 | Inddrivelsesrenter | `INDDRIVELSESRENTER` |
| 2 | Opkrævningsrenter | `OPKRAEVNINGSRENTER` |
| 3 | Gebyrer | `GEBYRER` |
| 4 | Afdrag (restgæld/hovedstol) | `AFDRAG` |
| 5 | Andre komponenter | `ANDRE` |

Inden for samme kategori gælder FIFO-rækkefølge: den ældste fordring dækkes først.

### Rentekomponenter (6 underpositioner, GIL § 4 stk. 1–4)

Inddrivelsesrenter fordeles yderligere på seks underpositioner jf. GIL § 4 stk. 1–4, herunder renter med og uden fordringshaver-særstilling, renter før tilbageførsel, og PSRM-interne renter.

### Inddrivelsesindsatstype (GIL § 10b og Retsplejelovens § 507)

Fordelingen af overskud i stk. 3 afhænger af hvilken inddrivelsesindsats der er iværksat: lønindeholdelse, udlæg, begge eller ingen.

### Dækningsvisning i portalen

Du kan se den aktuelle dækningsrækkefølge for en skyldner under:

**Skyldner → Dækningsrækkefølge**

Visningen viser en tabel med fordringens placering, kategori, rentekomponent og det beløb der vil dækkes ved næste betaling.

Du kan også køre en **simulering** med et hypotetisk beløb for at se den forventede allokering uden at gemme noget.

### Bogføringsspor

Når en betaling faktisk allokeres, oprettes en `daekning_record` pr. fordringkomponent. Denne post er skrivebeskyttet og kan ikke ændres — den udgør auditesporet for betalingsallokeringen. Posten fremgår af betalingstidslinjen.

## Storno

Hvis en transaktion skal reverseres (f.eks. ved krydsende handlinger), sker det via storno:

- Den originale postering bevares i historikken
- En modsatrettet storno-postering oprettes
- En ny korrekt postering oprettes

Alle storno-posteringer indeholder metadata om den originale postering, så den fulde historik er sporbar.

## Krydsende handlinger

Krydsende handlinger opstår, når en betaling med en valutadato der ligger forud for den seneste renteberegning modtages. I sådanne tilfælde:

1. Berørte renteposteringer storneres
2. Hele tidslinjen genberegnes fra valutadatoen
3. Nye korrekte posteringer oprettes

Dette sikrer, at bogføringen altid er korrekt i forhold til den faktiske kronologi.

## Audit

Alle handlinger logges til audit trail og CLS (Central Logging Service) med:

- Hvem (sagsbehandler-ID eller system)
- Hvad (handling)
- Hvornår (tidspunkt)
- Hvilken fordring/sag
- Resultat
