# Hæftelse

Hæftelse definerer forholdet mellem en fordring og de skyldnere, der er ansvarlige for den.

## Hæftelsestyper

| Type | Dansk | Beskrivelse |
|------|-------|-------------|
| Enehæftelse | Enehæftelse | En enkelt skyldner er ansvarlig for hele fordringen |
| Solidarisk hæftelse | Solidarisk hæftelse | Flere skyldnere er hver især ansvarlige for hele fordringen |
| Delt hæftelse | Delt hæftelse | Flere skyldnere er ansvarlige for definerede andele |

## Solidarisk hæftelse

Ved solidarisk hæftelse:

- Hver skyldner hæfter for hele beløbet
- Gældsstyrelsen kan inddrive fra enhver af skyldnerne
- Sædvanlig rykkerprocedure og individuel underretning kræves for **hver** skyldner
- PSRM understøtter primært solidarisk hæftelse

## Tilføjelse af skyldner

For at tilføje en skyldner til en fordring:

1. Tilbagekald fordringen med årsagskode **HAFT**
2. Underret alle parter skriftligt (inkl. nye medhæftere)
3. Genindsend fordringen med opdaterede skyldneroplysninger

!!! warning "Dækninger bevares"
    Ved tilbagekald med HAFT bevares eksisterende dækninger. De tilbagebetales ikke. Inddrivelsesrenter returneres.

!!! note "Nyt modtagelsestidspunkt"
    Den genindsendte fordring får et nyt modtagelsestidspunkt og dermed ny plads i dækningsrækkefølgen.

## Fjernelse af skyldner

Kontakt Fordringshaversupport for at fjerne en skyldner fra en fordring.

## I/S-selskaber (Interessentskaber)

For interessentskaber skal der indsendes:

- CVR for selskabet
- CPR/CVR for alle hæftende interessenter på tidspunktet for fordringens opståen

## PEF (Personligt ejede firmaer)

- Indsend CVR, hvis firmaet er aktivt
- Indsend CPR, hvis firmaet er ophørt
- PSRM tilføjer automatisk CPR for PEF-ejeren efter indsendelse

## Inddrivelsesrenter ved hæftelsesændringer

- Kun de **oprindeligt rapporterede** skyldnere hæfter for inddrivelsesrenter
- Undtagelse: I/S-selskaber, hvor alle interessenter hæfter
- Brug fordringstypen REINDGI til genindsendelse af inddrivelsesrenter
