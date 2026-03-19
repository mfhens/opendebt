# M2M-integrationsvejledning

Denne vejledning er til tekniske teams der integrerer fordringshavernes IT-systemer direkte med OpenDebt via system-til-system (M2M) grænseflader.

## Integrationsmuligheder

OpenDebt tilbyder to M2M-kanaler:

| Kanal | Protokol | Autentificering | Anvendelse |
|-------|----------|-----------------|------------|
| REST API | HTTPS/JSON | OAuth2 (Keycloak) | Nye integrationer |
| SOAP | HTTPS/XML | OCES3-certifikat | Ældre systemer (EFI/DMI-kompatibilitet) |

## REST API-integration

### Autentificering

REST API'et bruger OAuth2 Client Credentials flow via Keycloak:

1. Anmod om et access token fra Keycloak token-endpointet
2. Inkluder token som `Authorization: Bearer <token>` header i alle kald
3. Tokens udløber efter konfigureret levetid (typisk 5 minutter)

```
POST /realms/opendebt/protocol/openid-connect/token
Content-Type: application/x-www-form-urlencoded

grant_type=client_credentials
&client_id=<dit-client-id>
&client_secret=<din-client-secret>
```

### Endepunktkatalog

Alle REST-endepunkter er tilgængelige via integration-gatewayen:

| Operation | Metode | Endepunkt |
|-----------|--------|-----------|
| Indsend fordring | `POST` | `/integration-gateway/api/v1/creditor-m2m/claims/submit` |
| Hent kvittering | `GET` | `/integration-gateway/api/v1/creditor-m2m/receipts/{id}` |
| Hent underretninger | `GET` | `/integration-gateway/api/v1/creditor-m2m/notifications` |

### Indsendelse af fordring (eksempel)

**Request:**

```http
POST /integration-gateway/api/v1/creditor-m2m/claims/submit
Content-Type: application/json
Authorization: Bearer <token>
X-Creditor-Identity: <system-identifikator>
X-Correlation-ID: <sporings-id>

{
  "debtorId": "0101901234",
  "creditorId": "12345678",
  "debtTypeCode": "SKAT",
  "principalAmount": 15000.00,
  "dueDate": "2026-03-01",
  "paymentDeadline": "2026-04-01",
  "externalReference": "SAG-2026-001",
  "ocrLine": "+71 0000001234567890+",
  "description": "Restskat 2025"
}
```

**Response (succes):**

```json
{
  "outcome": "ACCEPTED",
  "claimId": "a1b2c3d4-...",
  "caseId": "e5f6g7h8-...",
  "correlationId": "<sporings-id>"
}
```

**Response (afvisning):**

```json
{
  "outcome": "REJECTED",
  "errors": [
    "RULE_001: Hovedstol skal være positiv",
    "RULE_015: Fordringstype ikke tilladt for denne fordringshaver"
  ],
  "correlationId": "<sporings-id>"
}
```

### Fejlhåndtering

| HTTP-status | Betydning |
|-------------|-----------|
| 201 Created | Fordring accepteret eller sendt til høring |
| 403 Forbidden | Adgang nægtet (ugyldigt certifikat/token eller ingen adgang) |
| 422 Unprocessable | Fordring afvist pga. valideringsfejl |
| 502 Bad Gateway | Intern servicefejl -- prøv igen |

### Headers

| Header | Påkrævet | Beskrivelse |
|--------|----------|-------------|
| `X-Creditor-Identity` | Ja | Identifikator for det kaldende system |
| `X-Correlation-ID` | Nej | Sporings-ID for end-to-end-korrelation (genereres automatisk hvis udeladt) |
| `Authorization` | Ja | Bearer token fra Keycloak |

## SOAP-integration

SOAP-endepunkter er tilgængelige for bagudkompatibilitet med EFI/DMI-systemer.

### OCES3-autentificering

SOAP-kald autentificeres via OCES3-certifikater:

1. Installer et gyldigt OCES3-certifikat udstedt af en godkendt CA
2. Certifikatets subject mappes til en fordringshaver-identifikator
3. Certifikater verificeres ved hvert kald

### WSDL-endepunkter

| Navnerum | WSDL |
|----------|------|
| OIO (`urn:oio:skat:efi:ws:1.0.1`) | `/soap/oio?wsdl` |
| SKAT (`http://skat.dk/begrebsmodel/2009/01/15/`) | `/soap/skat?wsdl` |

### Operationer

Begge navnerum understøtter de samme tre operationer:

| Operation | Funktion |
|-----------|----------|
| `MFFordringIndberet_I` | Indberetning af fordring |
| `MFKvitteringHent_I` | Hentning af kvittering |
| `MFUnderretSamlingHent_I` | Hentning af underretningssamling |

## Testmiljø

Et testmiljø er tilgængeligt for integration:

- **REST**: Brug Keycloak testmiljø med test-credentials
- **SOAP**: Brug OCES3-testcertifikater
- Testmiljøet indeholder testdata og simulerer alle forretningsregler

Kontakt Fordringshaversupport for adgang til testmiljøet.

## Logning og audit

Alle M2M-kald logges til Central Logging Service (CLS) med:

- Tidspunkt
- Kaldende system
- Service og operation
- Korrelations-ID
- Succes/fejl-status
- Svartid
