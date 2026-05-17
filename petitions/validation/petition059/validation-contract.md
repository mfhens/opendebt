# Validation Contract — petition059

## VAL-P059-001: FR-1.1 ForaeldelseRecord oprettes ved accept til inddrivelse

**Source**: `petitions/petition059-foraeldelse.feature` — Scenario: "FR-1.1 ForaeldelseRecord oprettes ved accept til inddrivelse"  
**Description**: When a claim is accepted into recovery, the limitation read surface exposes an initialized limitation record for that claim.  
**Pass criteria**:
- The claim can be read through the limitation surface after acceptance.
- The read result shows status `ACTIVE`, the supplied retsgrundlag, and empty afbrydelse/tillaegsfrist histories.
**Fail criteria**: Any missing record or any initialized field deviates from the pass criteria above.  
**Required evidence**: network

## VAL-P059-002: FR-1.2 GET returnerer komplet ForaeldelseStatusDto

**Source**: `petitions/petition059-foraeldelse.feature` — Scenario: "FR-1.2 GET returnerer komplet ForaeldelseStatusDto"  
**Description**: A limitation read returns the full observable DTO for a claim with prior afbrydelse and tillægsfrist history.  
**Pass criteria**:
- The response is HTTP 200.
- The response contains `fordringId`, `currentFristExpires`, `udskydelseDato`, `isInUdskydelse`, `retsgrundlag`, `afbrydelseHistory`, `tillaegsfristHistory`, and `status`.
- The returned `currentFristExpires` matches the latest visible state for the scenario.
**Fail criteria**: Any required field is absent, the call fails, or the visible expiry state is wrong.  
**Required evidence**: network

## VAL-P059-003: FR-1.2b GET bevarer propagated metadata i afbrydelseHistory

**Source**: `petitions/petition059-foraeldelse.feature` — Scenario: "FR-1.2b GET bevarer propagated metadata i afbrydelseHistory"  
**Description**: A limitation read preserves propagated history metadata for a claim-complex event.  
**Pass criteria**:
- The response is HTTP 200.
- The returned `afbrydelseHistory` contains `sourceFordringId`, `targetFordringId`, and `propagationReason` for the propagated event.
**Fail criteria**: Propagated metadata is absent or differs from the scenario expectation.  
**Required evidence**: network

## VAL-P059-004: FR-1.3 3-årig forældelsesfrist gælder uden afbrydelse og uden udskydelse

**Source**: `petitions/petition059-foraeldelse.feature` — Scenario: "FR-1.3 3-årig forældelsesfrist gælder uden afbrydelse og uden udskydelse"  
**Description**: A claim with no interruption and no postponement shows the ordinary three-year expiry.  
**Pass criteria**:
- The observable expiry date is exactly three years from the registration date in the scenario.
- Status remains `ACTIVE` and afbrydelse history remains empty.
**Fail criteria**: Any different expiry date, non-empty history, or different status is observed.  
**Required evidence**: network

## VAL-P059-005: FR-1.3b PSRM-fordring med særligt retsgrundlag har stadig 3-årig basisfrist før udlæg

**Source**: `petitions/petition059-foraeldelse.feature` — Scenario: "FR-1.3b PSRM-fordring med særligt retsgrundlag har stadig 3-årig basisfrist før udlæg"  
**Description**: A PSRM claim with special legal basis still shows the ordinary three-year base period before any udlæg event.  
**Pass criteria**:
- The observable expiry date is the same three-year base date required by the scenario.
- Status remains `ACTIVE`.
**Fail criteria**: Any 10-year or other non-base-period result is observed before udlæg.  
**Required evidence**: network

## VAL-P059-006: FR-1.4 API returnerer 404 for ukendt fordringId

**Source**: `petitions/petition059-foraeldelse.feature` — Scenario: "FR-1.4 API returnerer 404 for ukendt fordringId"  
**Description**: Reading limitation state for an unknown claim fails with not found.  
**Pass criteria**:
- The response is HTTP 404.
**Fail criteria**: Any non-404 response is observed.  
**Required evidence**: network

## VAL-P059-007: FR-2.1 PSRM-fordring fra 19-11-2015 har udskydelse til 20-11-2021

**Source**: `petitions/petition059-foraeldelse.feature` — Scenario: "FR-2.1 PSRM-fordring fra 19-11-2015 har udskydelse til 20-11-2021"  
**Description**: A PSRM claim in the statutory range shows the PSRM postponement date and floor expiry.  
**Pass criteria**:
- `udskydelseDato` is `2021-11-20`.
- `isInUdskydelse` is true at the scenario time.
- The visible expiry is not earlier than `2024-11-21`.
**Fail criteria**: Any missing or different postponement date, wrong boolean, or earlier expiry is observed.  
**Required evidence**: network

## VAL-P059-008: FR-2.1b PSRM-fordring registreret dagen før grænsen får ingen PSRM-udskydelse

**Source**: `petitions/petition059-foraeldelse.feature` — Scenario: "FR-2.1b PSRM-fordring registreret dagen før grænsen får ingen PSRM-udskydelse"  
**Description**: A PSRM claim registered before the statutory threshold shows no PSRM postponement.  
**Pass criteria**:
- `udskydelseDato` is null.
- `isInUdskydelse` is false.
**Fail criteria**: Any system-specific PSRM postponement is observed for the pre-threshold date.  
**Required evidence**: network

## VAL-P059-009: FR-2.1c PSRM-fordring registreret på grænsedatoen får PSRM-udskydelse

**Source**: `petitions/petition059-foraeldelse.feature` — Scenario: "FR-2.1c PSRM-fordring registreret på grænsedatoen får PSRM-udskydelse"  
**Description**: A PSRM claim on the exact threshold date receives the statutory PSRM postponement.  
**Pass criteria**:
- `udskydelseDato` is `2021-11-20`.
- `isInUdskydelse` is true at the scenario time.
- The visible expiry is not earlier than `2024-11-21`.
**Fail criteria**: Any absence of PSRM postponement on the threshold date is observed.  
**Required evidence**: network

## VAL-P059-010: FR-2.2 DMI-fordring registreret 2024-01-01 har udskydelse til 20-11-2027

**Source**: `petitions/petition059-foraeldelse.feature` — Scenario: "FR-2.2 DMI-fordring registreret 2024-01-01 har udskydelse til 20-11-2027"  
**Description**: A DMI/SAP38 claim on the statutory threshold date receives the DMI postponement.  
**Pass criteria**:
- `udskydelseDato` is `2027-11-20`.
- `isInUdskydelse` is true at the scenario time.
- The visible expiry is not earlier than `2030-11-21`.
**Fail criteria**: Any absence of DMI postponement on the threshold date or earlier expiry is observed.  
**Required evidence**: network

## VAL-P059-011: FR-2.2b DMI-fordring registreret dagen før grænsen får ingen DMI-udskydelse

**Source**: `petitions/petition059-foraeldelse.feature` — Scenario: "FR-2.2b DMI-fordring registreret dagen før grænsen får ingen DMI-udskydelse"  
**Description**: A DMI/SAP38 claim before the statutory threshold shows no DMI-specific postponement.  
**Pass criteria**:
- `udskydelseDato` is null.
- `isInUdskydelse` is false.
**Fail criteria**: Any DMI-specific postponement is observed for the pre-threshold date.  
**Required evidence**: network

## VAL-P059-012: FR-2.3 Udskydelsesdato er uforanderlig og ændres ikke af efterfølgende afbrydelse

**Source**: `petitions/petition059-foraeldelse.feature` — Scenario: "FR-2.3 Udskydelsesdato er uforanderlig og ændres ikke af efterfølgende afbrydelse"  
**Description**: A later afbrydelse recalculates expiry without mutating the stored postponement date.  
**Pass criteria**:
- `udskydelseDato` remains the original value after the afbrydelse.
- The visible expiry reflects the new afbrydelse date rule.
**Fail criteria**: Any mutation of `udskydelseDato` is observed.  
**Required evidence**: network

## VAL-P059-013: FR-2.3b isInUdskydelse skifter ved udskydelsesdatoens grænse

**Source**: `petitions/petition059-foraeldelse.feature` — Scenario Outline: "FR-2.3b isInUdskydelse skifter ved udskydelsesdatoens grænse"  
**Description**: The postponement indicator flips exactly at the threshold date for the full example matrix.  
**Pass criteria**:
- For every example row, `isInUdskydelse` is true before `udskydelseDato`.
- For every example row, `isInUdskydelse` is false on `udskydelseDato` and after it.
**Fail criteria**: Any example row produces a different boolean outcome than the matrix specifies.  
**Required evidence**: network

## VAL-P059-014: FR-2.4 Fordring uden udskydelsesregel får ingen udskydelsesdato

**Source**: `petitions/petition059-foraeldelse.feature` — Scenario: "FR-2.4 Fordring uden udskydelsesregel får ingen udskydelsesdato"  
**Description**: A claim outside the statutory postponement ranges shows no postponement date.  
**Pass criteria**:
- `udskydelseDato` is null.
- `isInUdskydelse` is false.
**Fail criteria**: Any system-specific postponement is observed outside the legal ranges.  
**Required evidence**: network

## VAL-P059-015: FR-3.1 BEROSTILLELSE afbryder forældelsesfrist for PSRM-fordring

**Source**: `petitions/petition059-foraeldelse.feature` — Scenario: "FR-3.1 BEROSTILLELSE afbryder forældelsesfrist for PSRM-fordring"  
**Description**: Registering berostillelse produces a new three-year expiry and visible legal reference.  
**Pass criteria**:
- The response is HTTP 201.
- `currentFristExpires` becomes the scenario date plus three years.
- The visible afbrydelse history carries legal reference `GIL § 18a, stk. 8`.
**Fail criteria**: Any different expiry, missing legal reference, or failed request is observed.  
**Required evidence**: network

## VAL-P059-016: FR-3.2 Lønindeholdelsesvarsel alene afbryder ikke forældelsesfrist

**Source**: `petitions/petition059-foraeldelse.feature` — Scenario: "FR-3.2 Lønindeholdelsesvarsel alene afbryder ikke forældelsesfrist"  
**Description**: A wage-garnishment warning without confirmed decision is rejected and leaves visible state unchanged.  
**Pass criteria**:
- The response is HTTP 422.
- The problem detail states that varsel alone does not interrupt limitation.
- `currentFristExpires` remains unchanged.
**Fail criteria**: Any accepted command or changed visible expiry is observed.  
**Required evidence**: network

## VAL-P059-017: FR-3.2b Manglende afgoerelseRegistreret for lønindeholdelse returnerer HTTP 422

**Source**: `petitions/petition059-foraeldelse.feature` — Scenario: "FR-3.2b Manglende afgoerelseRegistreret for lønindeholdelse returnerer HTTP 422"  
**Description**: A wage-garnishment interruption without the required decision flag is rejected.  
**Pass criteria**:
- The response is HTTP 422.
- The problem detail states that varsel alone does not interrupt limitation.
- `currentFristExpires` remains unchanged.
**Fail criteria**: Any accepted command or changed visible expiry is observed.  
**Required evidence**: network

## VAL-P059-018: FR-3.3 Afgørelse om lønindeholdelse afbryder ved underretning til debitor

**Source**: `petitions/petition059-foraeldelse.feature` — Scenario: "FR-3.3 Afgørelse om lønindeholdelse afbryder ved underretning til debitor"  
**Description**: A confirmed wage-garnishment decision interrupts from the notification date.  
**Pass criteria**:
- The response is HTTP 201.
- `currentFristExpires` becomes the notified date plus three years.
- The visible legal reference reflects the wage-garnishment interruption basis in the scenario.
**Fail criteria**: Any different start date, expiry, or legal reference is observed.  
**Required evidence**: network

## VAL-P059-019: FR-3.4 Lønindeholdelse afbryder alle fordringer omfattet af samme afgørelse

**Source**: `petitions/petition059-foraeldelse.feature` — Scenario: "FR-3.4 Lønindeholdelse afbryder alle fordringer omfattet af samme afgørelse"  
**Description**: One wage-garnishment decision updates all covered claims to the same new expiry.  
**Pass criteria**:
- Every claim covered by the decision shows the same new expiry date.
**Fail criteria**: Any covered claim remains unchanged or shows a different visible expiry.  
**Required evidence**: network

## VAL-P059-020: FR-3.5 Lønindeholdelse inaktiv i 1 år medfører at ny forældelsesfrist begynder

**Source**: `petitions/petition059-foraeldelse.feature` — Scenario: "FR-3.5 Lønindeholdelse inaktiv i 1 år medfører at ny forældelsesfrist begynder"  
**Description**: One year of inactivity triggers a new visible limitation period from the inactivity anniversary.  
**Pass criteria**:
- The new visible limitation period starts from the inactivity anniversary date in the scenario.
- `currentFristExpires` matches the scenario outcome.
**Fail criteria**: Any start date or expiry different from the scenario is observed.  
**Required evidence**: network

## VAL-P059-021: FR-3.6 Udlæg på fordring med almindeligt retsgrundlag sætter ny 3-årig frist

**Source**: `petitions/petition059-foraeldelse.feature` — Scenario: "FR-3.6 Udlæg på fordring med almindeligt retsgrundlag sætter ny 3-årig frist"  
**Description**: Attachment on an ordinary-basis claim produces a new three-year visible expiry.  
**Pass criteria**:
- The response is HTTP 201.
- `currentFristExpires` becomes the event date plus three years.
**Fail criteria**: Any different visible expiry is observed.  
**Required evidence**: network

## VAL-P059-022: FR-3.7 Udlæg på fordring med særligt retsgrundlag sætter ny 10-årig frist

**Source**: `petitions/petition059-foraeldelse.feature` — Scenario: "FR-3.7 Udlæg på fordring med særligt retsgrundlag sætter ny 10-årig frist"  
**Description**: Attachment on a special-basis claim produces a new ten-year visible expiry.  
**Pass criteria**:
- The response is HTTP 201.
- `currentFristExpires` becomes the event date plus ten years.
**Fail criteria**: Any shorter visible expiry is observed.  
**Required evidence**: network

## VAL-P059-023: FR-3.8 Forgæves udlæg har samme afbrydelseseffekt som succesfuldt udlæg

**Source**: `petitions/petition059-foraeldelse.feature` — Scenario Outline: "FR-3.8 Forgæves udlæg har samme afbrydelseseffekt som succesfuldt udlæg"  
**Description**: Successful and forgæves udlæg produce the same observable interruption outcome for the example matrix.  
**Pass criteria**:
- For every example row, the response is HTTP 201.
- For every example row, `currentFristExpires` matches the expected new expiry and the legal reference is `Forældelsesl. § 18, stk. 1`.
**Fail criteria**: Any example row produces a different visible outcome from the matrix.  
**Required evidence**: network

## VAL-P059-024: FR-3.9 Modregning afbryder forældelsesfrist

**Source**: `petitions/petition059-foraeldelse.feature` — Scenario: "FR-3.9 Modregning afbryder forældelsesfrist"  
**Description**: Set-off produces a new three-year visible expiry with the correct legal reference.  
**Pass criteria**:
- The response is HTTP 201.
- `currentFristExpires` becomes the event date plus three years.
- The visible legal reference is `Forældelsesl. § 18, stk. 4`.
**Fail criteria**: Any different visible expiry or legal reference is observed.  
**Required evidence**: network

## VAL-P059-025: FR-3.10 Ukendt afbrydelsestype returnerer HTTP 422

**Source**: `petitions/petition059-foraeldelse.feature` — Scenario: "FR-3.10 Ukendt afbrydelsestype returnerer HTTP 422"  
**Description**: An unknown interruption type is rejected as invalid input.  
**Pass criteria**:
- The response is HTTP 422.
**Fail criteria**: Any non-422 response is observed.  
**Required evidence**: network

## VAL-P059-026: FR-3.11 Manglende eventDate returnerer HTTP 422

**Source**: `petitions/petition059-foraeldelse.feature` — Scenario: "FR-3.11 Manglende eventDate returnerer HTTP 422"  
**Description**: A missing interruption date is rejected at the API boundary.  
**Pass criteria**:
- The response is HTTP 422.
**Fail criteria**: Any non-422 response is observed.  
**Required evidence**: network

## VAL-P059-027: FR-3.12 Ukendt fordringId ved afbrydelse returnerer HTTP 404

**Source**: `petitions/petition059-foraeldelse.feature` — Scenario: "FR-3.12 Ukendt fordringId ved afbrydelse returnerer HTTP 404"  
**Description**: Registering interruption for an unknown claim fails with not found.  
**Pass criteria**:
- The response is HTTP 404.
**Fail criteria**: Any non-404 response is observed.  
**Required evidence**: network

## VAL-P059-028: FR-4.1 Nyt fordringskompleks oprettes med initiale medlemmer

**Source**: `petitions/petition059-foraeldelse.feature` — Scenario: "FR-4.1 Nyt fordringskompleks oprettes med initiale medlemmer"  
**Description**: Creating a claim complex visibly returns a new complex and assigns the initial members.  
**Pass criteria**:
- A new `kompleksId` is returned or otherwise exposed.
- The supplied claims are visible as members of the created complex.
**Fail criteria**: Any missing complex identifier or member assignment is observed.  
**Required evidence**: network

## VAL-P059-029: FR-4.2 Medlem kan tilføjes og medlemslisten kan hentes

**Source**: `petitions/petition059-foraeldelse.feature` — Scenario: "FR-4.2 Medlem kan tilføjes og medlemslisten kan hentes"  
**Description**: A member add operation is visible through the subsequent member-list read.  
**Pass criteria**:
- The added claim is visible as a member of the named complex.
- The subsequent member-list read returns all expected members.
**Fail criteria**: Any missing added member or incorrect member list is observed.  
**Required evidence**: network

## VAL-P059-030: FR-4.3 Afbrydelse for én fordring i fordringskompleks propagerer til alle medlemmer med fuld metadata

**Source**: `petitions/petition059-foraeldelse.feature` — Scenario: "FR-4.3 Afbrydelse for én fordring i fordringskompleks propagerer til alle medlemmer med fuld metadata"  
**Description**: A claim-complex interruption visibly updates every member and preserves propagation metadata.  
**Pass criteria**:
- The response is HTTP 201.
- Every member shows the same new expiry date.
- The propagated entries expose `sourceFordringId`, `targetFordringId`, `propagationReason`, and legal reference `GIL § 18a, stk. 2`.
- Audit evidence exists for the propagated events named in the scenario.
**Fail criteria**: Any member is missing the propagated outcome or metadata.  
**Required evidence**: network

## VAL-P059-031: FR-4.4 Fordringskompleks-propagation er atomisk og ruller tilbage ved fejl

**Source**: `petitions/petition059-foraeldelse.feature` — Scenario: "FR-4.4 Fordringskompleks-propagation er atomisk og ruller tilbage ved fejl"  
**Description**: A propagation failure is observable as a failed request with no visible state change on any member.  
**Pass criteria**:
- The request fails.
- Every affected claim retains its original visible expiry state.
**Fail criteria**: Any partial member update is observable after the failure.  
**Required evidence**: network

## VAL-P059-032: FR-4.5 Modtagelse af tomt fordringskompleks udløser foreløbig afbrydelse

**Source**: `petitions/petition059-foraeldelse.feature` — Scenario: "FR-4.5 Modtagelse af tomt fordringskompleks udløser foreløbig afbrydelse"  
**Description**: Receiving an empty claim complex visibly records a provisional interruption and exposes it on read.  
**Pass criteria**:
- The claim shows an interruption with legal reference `GIL § 18a, stk. 7`.
- `currentFristExpires` becomes the receipt date plus three years.
- A subsequent read exposes that interruption in `afbrydelseHistory`.
**Fail criteria**: Any missing provisional interruption or missing read visibility is observed.  
**Required evidence**: network

## VAL-P059-033: FR-5.1 Intern opskrivning tilføjer 2-årig tillægsfrist til forældelsesfrist

**Source**: `petitions/petition059-foraeldelse.feature` — Scenario: "FR-5.1 Intern opskrivning tilføjer 2-årig tillægsfrist til forældelsesfrist"  
**Description**: Registering internal write-up visibly adds a two-year supplementary period and history entry.  
**Pass criteria**:
- The response is HTTP 201.
- `currentFristExpires` matches the scenario result.
- `tillaegsfristHistory` contains the visible type, applied date, extension years, new expiry, and legal reference.
- Audit evidence exists for the supplementary-period registration.
**Fail criteria**: Any missing history entry, wrong expiry, or missing audit evidence is observed.  
**Required evidence**: network

## VAL-P059-034: FR-5.2 Tillægsfrist beregnes fra max af currentFristExpires og appliedDate

**Source**: `petitions/petition059-foraeldelse.feature` — Scenario: "FR-5.2 Tillægsfrist beregnes fra max af currentFristExpires og appliedDate"  
**Description**: The visible supplementary-period result uses the later of the current expiry and the applied date.  
**Pass criteria**:
- `currentFristExpires` equals the scenario result that reflects the max-rule branch.
**Fail criteria**: Any result consistent only with naive appliedDate-plus-two-years logic is observed.  
**Required evidence**: network

## VAL-P059-035: FR-6.1 Debitors forældelsesindsigelse registreres og udløser sagsbehandlerworkflow

**Source**: `petitions/petition059-foraeldelse.feature` — Scenario: "FR-6.1 Debitors forældelsesindsigelse registreres og udløser sagsbehandlerworkflow"  
**Description**: Registering an objection visibly creates an objection identifier and moves the claim into pending review.  
**Pass criteria**:
- The response is HTTP 201.
- A unique `indsigelsesId` is returned.
- Claim status becomes `INDSIGELSE_PENDING`.
- Audit evidence exists and reflects server-derived identity.
**Fail criteria**: Any missing identifier, wrong status, or client-supplied identity acceptance is observed.  
**Required evidence**: network

## VAL-P059-036: FR-6.1b Offentlig indsigelsesregistrering afviser klientstyrede identitets- og skyldnerfelter

**Source**: `petitions/petition059-foraeldelse.feature` — Scenario: "FR-6.1b Offentlig indsigelsesregistrering afviser klientstyrede identitets- og skyldnerfelter"  
**Description**: The public objection registration surface rejects spoofed identity and debtor fields.  
**Pass criteria**:
- The request is rejected as invalid public command input.
- No objection is created.
- Claim status remains `ACTIVE`.
**Fail criteria**: Any accepted spoofed payload or status change is observed.  
**Required evidence**: network

## VAL-P059-037: FR-6.2 Gyldig forældelsesindsigelse fjerner fordringen fra inddrivelse

**Source**: `petitions/petition059-foraeldelse.feature` — Scenario: "FR-6.2 Gyldig forældelsesindsigelse fjerner fordringen fra inddrivelse"  
**Description**: A valid objection visibly marks the claim as prescribed and removes it from active recovery.  
**Pass criteria**:
- The response is HTTP 200.
- Claim status becomes `FORAELDET`.
- The claim is no longer visible as actively in recovery.
- Audit evidence exists with outcome `VALID` and server-derived identity.
**Fail criteria**: Any retained active state or missing valid-outcome evidence is observed.  
**Required evidence**: network

## VAL-P059-038: FR-6.3 Ugyldig forældelsesindsigelse returnerer fordring til aktiv inddrivelse

**Source**: `petitions/petition059-foraeldelse.feature` — Scenario: "FR-6.3 Ugyldig forældelsesindsigelse returnerer fordring til aktiv inddrivelse"  
**Description**: An invalid objection visibly returns the claim to active recovery and preserves the rationale.  
**Pass criteria**:
- The response is HTTP 200.
- Claim status becomes `ACTIVE`.
- The rejection rationale is visible on the objection.
- Audit evidence exists with outcome `INVALID` and server-derived identity.
**Fail criteria**: Any missing rationale, wrong status, or missing invalid-outcome evidence is observed.  
**Required evidence**: network

## VAL-P059-039: FR-6.3b Offentlig indsigelsesevaluering afviser klientstyrede identitets- og skyldnerfelter

**Source**: `petitions/petition059-foraeldelse.feature` — Scenario: "FR-6.3b Offentlig indsigelsesevaluering afviser klientstyrede identitets- og skyldnerfelter"  
**Description**: The public objection decision surface rejects spoofed identity and debtor fields.  
**Pass criteria**:
- The request is rejected as invalid public command input.
- Claim status does not change.
- The objection record is not updated.
**Fail criteria**: Any accepted spoofed payload or visible state mutation is observed.  
**Required evidence**: network

## VAL-P059-040: FR-7.1 Sagsbehandlerportalen viser forældelsesstatus med ISO-dato og udskydelse

**Source**: `petitions/petition059-foraeldelse.feature` — Scenario: "FR-7.1 Sagsbehandlerportalen viser forældelsesstatus med ISO-dato og udskydelse"  
**Description**: The caseworker portal shows the limitation panel with status, ISO expiry date, and postponement information.  
**Pass criteria**:
- The limitation panel is visible on the claim detail page.
- The visible status, expiry date, postponement date, and postponement-window indicator match the scenario.
**Fail criteria**: Any required panel element is absent or shows different visible values.  
**Required evidence**: screenshots, console_errors, network

## VAL-P059-041: FR-7.2 Sagsbehandlerportalen viser afbrydelseshistorik med resulting new frist

**Source**: `petitions/petition059-foraeldelse.feature` — Scenario: "FR-7.2 Sagsbehandlerportalen viser afbrydelseshistorik med resulting new frist"  
**Description**: The caseworker portal renders the visible interruption history table in chronological order.  
**Pass criteria**:
- The history table shows the expected row count.
- Each visible row contains type, date, legal reference, and resulting new expiry.
- The visible ordering is chronological.
**Fail criteria**: Any missing column, wrong row count, or non-chronological presentation is observed.  
**Required evidence**: screenshots, console_errors, network

## VAL-P059-042: FR-7.3 Sagsbehandlerportalen viser tillægsfristhistorik og fordringskompleks-medlemskab

**Source**: `petitions/petition059-foraeldelse.feature` — Scenario: "FR-7.3 Sagsbehandlerportalen viser tillægsfristhistorik og fordringskompleks-medlemskab"  
**Description**: The caseworker portal shows the claim-complex section, supplementary-period history, and propagated metadata.  
**Pass criteria**:
- The visible panel includes the claim-complex section and expected member list.
- The visible panel includes supplementary-period history with type, date, extension, and new expiry.
- The visible propagated event includes the source and target claim identifiers.
**Fail criteria**: Any missing section, member, history field, or propagated metadata is observed.  
**Required evidence**: screenshots, console_errors, network

## VAL-P059-043: FR-7.4 Sagsbehandler med skriveadgang ser knap til registrering af forældelsesindsigelse

**Source**: `petitions/petition059-foraeldelse.feature` — Scenario: "FR-7.4 Sagsbehandler med skriveadgang ser knap til registrering af forældelsesindsigelse"  
**Description**: A write-capable caseworker sees the objection-registration control for an active claim.  
**Pass criteria**:
- The visible page shows the button "Registrer forældelsesindsigelse".
**Fail criteria**: The button is absent for the write-capable active-state scenario.  
**Required evidence**: screenshots, console_errors, network

## VAL-P059-044: FR-7.5 Afventende indsigelse viser evalueringsformular med rationalefelt

**Source**: `petitions/petition059-foraeldelse.feature` — Scenario: "FR-7.5 Afventende indsigelse viser evalueringsformular med rationalefelt"  
**Description**: A pending objection shows the evaluation controls and hides the registration action.  
**Pass criteria**:
- The visible page shows the VALID and INVALID options plus a rationale field.
- The registration button is not visible.
**Fail criteria**: Any missing evaluation control or visible registration action is observed.  
**Required evidence**: screenshots, console_errors, network

## VAL-P059-045: FR-7.6 Forældet fordring viser udfald og rationale uden registreringsknap

**Source**: `petitions/petition059-foraeldelse.feature` — Scenario: "FR-7.6 Forældet fordring viser udfald og rationale uden registreringsknap"  
**Description**: A prescribed claim shows the visible outcome and rationale without the registration action.  
**Pass criteria**:
- The visible page shows the prescribed outcome and the expected rationale text.
- The registration button is not visible.
**Fail criteria**: Any missing visible outcome/rationale or visible registration action is observed.  
**Required evidence**: screenshots, console_errors, network

## VAL-P059-046: FR-7.7 Read-only caseworker ser panelet men ingen skrivehandlinger

**Source**: `petitions/petition059-foraeldelse.feature` — Scenario: "FR-7.7 Read-only caseworker ser panelet men ingen skrivehandlinger"  
**Description**: A read-only caseworker can inspect the panel but cannot trigger write actions.  
**Pass criteria**:
- The visible page shows the limitation panel.
- No visible controls allow interruption registration, objection registration, or objection evaluation.
**Fail criteria**: Any write action is visible or usable in the read-only scenario.  
**Required evidence**: screenshots, console_errors, network

## VAL-P059-047: NFR-3.1 Forældelsesdata og API-svar refererer kun skyldner via person_id

**Source**: `petitions/petition059-foraeldelse.feature` — Scenario: "NFR-3.1 Forældelsesdata og API-svar refererer kun skyldner via person_id"  
**Description**: The visible limitation API exposes no debtor PII and uses only an opaque debtor identifier if one is present.  
**Pass criteria**:
- No CPR, name, address, email, or phone appears in the visible API response body.
- If the visible API response includes a debtor reference, it is an opaque UUID only.
**Fail criteria**: Any visible PII field appears in the response, or any visible debtor reference is non-opaque.  
**Required evidence**: network

**Note**: The source package is ambiguous on whether `person_id` is a public DTO field. This validation therefore checks the observable no-PII contract and constrains any exposed debtor reference, but does not require a new public field beyond the current explicit DTO/OpenAPI surface.

## VAL-P059-048: NFR-2.1 Almindelig afbrydelsesregistrering logges til CLS med påkrævede felter

**Source**: `petitions/petition059-foraeldelse.feature` — Scenario: "NFR-2.1 Almindelig afbrydelsesregistrering logges til CLS med påkrævede felter"  
**Description**: A standard interruption registration emits a visible CLS audit payload with the required fields.  
**Pass criteria**:
- The interruption call succeeds.
- The CLS audit evidence contains actor identity, timestamp, fordringId, legal reference `Forældelsesl. § 18, stk. 1`, and the UDLAEG event description.
**Fail criteria**: Any required audit field is missing from the visible CLS payload.  
**Required evidence**: network

## VAL-P059-049: NFR-2.2 Øvrige auditerede hændelser skriver CLS-payload med påkrævede felter

**Source**: `petitions/petition059-foraeldelse.feature` — Scenario Outline: "NFR-2.2 Øvrige auditerede hændelser skriver CLS-payload med påkrævede felter"  
**Description**: The full audited-event matrix emits the required CLS payload fields for each event type in the examples.  
**Pass criteria**:
- For every example row, the visible CLS payload contains actor identity, timestamp, the specified fordringId, and the specified legal reference.
- For every example row, the visible event description matches the scenario matrix.
**Fail criteria**: Any example row is missing required CLS fields or shows a different event description.  
**Required evidence**: network
