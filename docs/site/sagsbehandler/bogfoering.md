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

## Dækningsrækkefølge

Når en betaling modtages, allokeres den efter dækningsrækkefølgen:

1. **Inddrivelsesrenter** dækkes først
2. **Hovedstol** dækkes derefter

Dette fremgår af tidslinjen som separate bogføringsposter.

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
