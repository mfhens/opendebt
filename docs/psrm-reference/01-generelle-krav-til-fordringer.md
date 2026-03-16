---
source: https://gaeldst.dk/fordringshaver/find-vejledning/generelle-krav-til-fordringer
fetched: 2026-03-16
---

# Generelle krav til fordringer

Vejledningen gennemgår de generelle krav til overdragelse af fordringer. Specifikke krav og stamdatapraksis findes i det individuelle aftalegrundlag.

## Generelle krav til overdragelse af fordringer

### 1. Sædvanlig rykkerprocedure

Fordringer må først overdrages efter udløbet af sidste rettidige betalingsdato og efter forgæves "sædvanlig rykkerprocedure". PSRM afviser fordringer hvor SRB endnu ikke er udløbet.

### 2. Skriftlig underretning af skyldner

Skyldner skal have skriftlig underretning inden overdragelse. Kan ske som led i opkrævning/rykkerskrivelse.

Krav:
- Skal gives inden overdragelse
- Skal være skriftlig
- Skal gives af fordringshaver eller den der opkræver

Flere skyldnere = individuel underretning til alle. Alle medhæftere skal indberettes samtidig.

**Undtagelser:** Ikke muligt at underrette (fx ikke tilmeldt folkeregister + fritaget Digital Post), udlæg uden underretning, konkursbegæring ved økonomisk kriminalitet, arrestation.

### 3. Hovedfordringer adskilt fra renter og lignende ydelser

"Rente og lignende ydelse" = rente (kredit/mora), provisioner, gebyrer (rykker, PBS). Omfatter IKKE inddrivelsesomkostninger.

Hovedfordringer og renter/gebyrer skal overdrages som selvstændige fordringer i korrekte fordringstyper.

### 4. Renter relateret til hovedfordring

Underfordringer (renter) skal overdrages med reference til den hovedfordring de er beregnet af.

**Allerede indfriet hovedfordring:** Indsend hovedfordring med saldo 0 kr. (0-fordring), derefter renter relateret til 0-fordringen.

### 5. Fordringer overdrages enkeltvis

To+ fordringer må IKKE slås sammen. Gælder både hoved- og underfordringer. Fordringer med forskelligt lovgrundlag må ikke sammenblandes (påvirker forældelsesfrist).

### 6. Stamdata skal overholde krav

| Stamdatafelt | Obl/Valgfri | Definition |
|---|---|---|
| Beløb | Obligatorisk | Fordringens restgæld ved overdragelse |
| Hovedstol | Obligatorisk | Fordringens oprindelige pålydende |
| Fordringshaver | Obligatorisk | Kreditor for fordringen |
| Fordringshaver Reference | Obligatorisk | Unikt referencenummer (sagsnummer + evt. løbenummer) |
| Fordringsart | Obligatorisk | INDR (inddrivelse) eller MODR (kun modregning). Kun INDR i PSRM |
| Fordringstype (kode) | Obligatorisk | Kode der beskriver retligt grundlag (fx PSRESTS = restskat) |
| HovedfordringsID | Obligatorisk | Knytter relateret fordring til hovedfordring |
| Beskrivelse | Afhængig | Fritekst max 100 tegn. Medtages i breve til skyldner. Må IKKE indeholde CPR, navn, adresse på andre end skyldner |
| Fordringsperiode | Afhængig | Start- og sluttidspunkt. Perioder defineret af lovgivning |
| Stiftelsesdato | Afhængig | Tidspunkt for retsstiftende begivenhed |
| Forfaldsdato | Afhængig | Tidligste tidspunkt fordring kan kræves betalt |
| Sidste rettidige betalingsdato (SRB) | Afhængig | Seneste betalingstidspunkt uden misligholdelse. Henstand/betalingsordning ændrer SRB. Rykkerskrivelser udskyder IKKE SRB |
| Identifikation af skyldner | Obligatorisk | CPR, CVR/SE eller AKR nummer |
| Forældelsesdato | Obligatorisk | Sidste dag fordringen er retskraftig. Skal afspejle aktuel dato ved oversendelse |
| Domsdato | Valgfri | Dato for domsafsigelse (kun ved forældelseslovens §5 stk 1 nr 3) |
| Forligsdato | Valgfri | Dato for forligsaftale |
| Rentevalg | Valgfri | Renteregel, rentesatskode, rentesats |
| Fordringsnote | Valgfri | Sagsrelevante bemærkninger til Gældsstyrelsen |
| Fordringsdokumenter | Valgfri | Dokumentation for fordringens eksistens |
| Kundenote | Valgfri | Information om skyldner |
| P-nummer | Valgfri | Produktionsenhed/lokation |
| Bobehandling | Valgfri (S2S) / Obligatorisk (portal) | Om fordring er omfattet af bobehandling |

### 7. GDPR krav

Beskrivelsesfeltet må IKKE indeholde personoplysninger (CPR, navne, adresser) på andre end skyldner. Maks. første 6 cifre af CPR for andre personer hvis påkrævet af aftalegrundlag.

## Når fordring er modtaget

System returnerer kvittering med:
- Tildelt fordrings-ID
- Eventuelle hæftelsesforhold, AKR-nummer
- Slutstatus: **UDFØRT**, **AFVIST** eller **HØRING**
