# Sagsoversigt

Sagsoversigten giver dig overblik over alle sager der er tildelt dig eller dit team.

## Sagstilstande

| Tilstand | Betydning |
|----------|-----------|
| Ny | Sagen er nyoprettet og endnu ikke tildelt |
| Tildelt | Sagen er tildelt en sagsbehandler |
| Under behandling | Sagen er aktivt under behandling |
| Afventer | Sagen afventer ekstern handling (f.eks. skyldnersvar) |
| Afsluttet | Sagen er lukket |

## Sagstildeling

Sager oprettes automatisk, når en fordring indsendes og godkendes. Tildeling sker enten:

- **Automatisk**: Baseret på fordringstype og sagsbehandlerens kompetenceområde
- **Manuelt**: En leder kan omtildele sager

## Sagsvisning

For hver sag kan du se:

- **Sagsdata**: Sagsnummer, oprettelsesdato, tildelt sagsbehandler
- **Fordringer**: Alle fordringer knyttet til sagen
- **Skyldneroplysninger**: Identifikation via person-ID (CPR vises ikke direkte)
- **Tidslinje**: Kronologisk hændelsesstrøm med alle hændelsestyper — sagshændelser, betalingsposteringer, inddrivelsesskridt, korrespondance og indsigelser
- **Inddrivelsesskridt**: Igangværende og afsluttede inddrivelsesskridt

## Tidslinjen

Tidslinjen (fanen **Tidslinje** på sagsdetailsiden) samler hændelser fra case-service og payment-service i én kronologisk visning. Du kan filtrere efter:

- **Hændelseskategori**: CASE, GÆLD_LIVSCYKLUS, FINANSIEL, INDDRIVELSE, KORRESPONDANCE, INDSIGELSE, JOURNAL
- **Datointerval**: fra/til-dato
- **Fordring**: filtrer til en specifik fordring

Hændelser indlæses progressivt via **Indlæs flere**-knappen (HTMX-drevet). Filtrering nulstiller til side 1.

## OIO-Sag datamodel

Sager følger OIO-Sag-standarden for offentlig sagshåndtering, hvilket sikrer interoperabilitet med andre offentlige systemer.
