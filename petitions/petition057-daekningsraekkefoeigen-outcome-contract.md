# Petition 057 Outcome Contract

## Petition reference

**Petition 057:** Dækningsrækkefølge — GIL § 4 payment application order (G.A.2.3.2)  
**Legal basis:** GIL § 4 stk. 1–4, GIL § 6a stk. 1 og stk. 12, GIL § 9 stk. 1 og stk. 3,
GIL § 10b, Gæld.bekendtg. § 4 stk. 3, Retsplejelovens § 507, Lov nr. 288/2022  
**G.A. snapshot:** v3.16 (2026-03-28)  
**Depended on by:** Petition 059 (forældelse), petition 062 (pro-rata distribution)

---

## Observable outcomes by functional requirement

### FR-1: Priority categories — GIL § 4, stk. 1 rule engine

**Preconditions**
- A debtor has outstanding fordringer belonging to more than one priority category.
- A payment is received that covers part of the total outstanding balance.

**Trigger**
- Payment application is initiated for the debtor.

**Expected rule engine behaviour**
- The rule engine applies the payment exclusively to category-1 fordringer (rimelige
  omkostninger ved udenretlig inddrivelse i udlandet) until they are fully covered before
  touching any category-2 fordring.
- Only after all category-2 fordringer (bøder, tvangsbøder og tilbagebetalingskrav) are
  fully covered does any category-3 fordring (underholdsbidrag) receive a dækning.
- Within category 3, privatretlige underholdsbidrag are covered before offentlige
  underholdsbidrag.
- Only after all three higher categories are exhausted does the payment reach category-4
  fordringer (andre fordringer).
- No category-4 fordring receives a non-zero dækning if any category-1, -2, or -3 fordring
  remains outstanding.

**Expected API response**
- Each position in the `/daekningsraekkefoelge` response carries `prioritetKategori` set to
  one of: `RIMELIGE_OMKOSTNINGER`, `BOEDER_TVANGSBOEEDER_TILBAGEBETALING`,
  `UNDERHOLDSBIDRAG_PRIVATRETLIG`, `UNDERHOLDSBIDRAG_OFFENTLIG`, `ANDRE_FORDRINGER`.
- The `gilParagraf` field is set to `"GIL § 4, stk. 1, nr. <1|2|3|4>"` for each position.

**Failure conditions (FR-1)**
- A category-4 fordring receives dækning while a category-1, -2, or -3 fordring remains
  outstanding.
- Privatretlige underholdsbidrag are not covered before offentlige within category 3.
- The `prioritetKategori` field is absent or wrong in the API response.
- Tvangsbøder are placed in category 4 instead of category 2 (lov nr. 288/2022 violation).

---

### FR-2: Within-category FIFO ordering — GIL § 4, stk. 2

**Preconditions**
- A debtor has two or more fordringer in the same priority category with different
  modtagelsesdatoer.

**Trigger**
- Payment application is initiated; the payment amount covers only part of the category.

**Expected rule engine behaviour**
- The fordring with the earliest modtagelsesdato is covered first.
- For fordringer received before 1 September 2013, the `legacyModtagelsesdato` stored at
  migration is used as the sort key, not the overdragelse API timestamp.
- The ordered list returned by the `GET` endpoint reflects this FIFO order.
- The `fifoSortKey` field in the API response contains the ISO-8601 date used for ordering.

**Failure conditions (FR-2)**
- A later-received fordring is covered before an earlier-received one in the same category.
- Pre-2013 fordringer are sorted by overdragelse API timestamp instead of
  `legacyModtagelsesdato`.
- The `fifoSortKey` field is absent from the API response.

---

### FR-3: Interest ordering within each fordring — Gæld.bekendtg. § 4, stk. 3

**Preconditions**
- A fordring has outstanding interest components (opkrævningsrenter, inddrivelsesrenter)
  as well as outstanding principal (Hovedfordring).
- A payment is received that covers only part of the fordring.

**Trigger**
- Payment application reaches this fordring in the ordered list.

**Expected rule engine behaviour**
- Opkrævningsrenter (sub-position 1) are fully covered before any inddrivelsesrenter are
  touched.
- Inddrivelsesrenter sub-positions 2 through 5 are covered in ascending sub-position order.
- Within each sub-position, earlier accrual periods are covered before later periods.
- The Hovedfordring (principal) receives no dækning until all interest sub-positions are
  exhausted.
- A line-item allocation record is produced for each component type that receives dækning,
  referencing its sub-position number and legal basis.

**Expected API response**
- The simulation endpoint (`POST .../simulate`) returns one line item per cost component
  that would receive dækning, with `komponent` set to one of:
  `OPKRAEVANINGSRENTER`, `INDDRIVELSESRENTER_FORDRINGSHAVER_STK3`,
  `INDDRIVELSESRENTER_FOER_TILBAGEFOERSEL`, `INDDRIVELSESRENTER_STK1`,
  `OEVRIGE_RENTER_PSRM`, `HOVEDFORDRING`.

**Failure conditions (FR-3)**
- Principal receives dækning while any interest component of the same fordring remains
  outstanding.
- Interest sub-positions are applied in the wrong order.
- A line-item record with `komponent = "HOVEDFORDRING"` precedes a record with any rente
  komponent for the same fordring.
- The `komponent` field is absent from the API response.

---

### FR-4: Inddrivelsesindsats rule — GIL § 4, stk. 3

**Preconditions**
- A payment is received as a result of an inddrivelsesindsats (e.g., lønindeholdelse or
  udlæg realisation).
- The payment record carries the `inddrivelsesindsatsType` field.

**Trigger**
- Payment application is initiated with a non-null `inddrivelsesindsatsType`.

**Expected rule engine behaviour — general inddrivelsesindsats**
- The payment first covers all fordringer associated with the triggering inddrivelsesindsats,
  in the normal GIL § 4, stk. 1 priority order among those indsats-fordringer.
- Surplus after the indsats-fordringer are covered is applied to other fordringer coverable
  by the same inddrivelsesindsats type.

**Expected rule engine behaviour — udlæg exception**
- When `inddrivelsesindsatsType = "UDLAEG"`, any surplus remaining after the udlæg-fordringer
  are fully covered is NOT applied to other fordringer.
- The dækning run terminates after the udlæg-fordringer are exhausted; the surplus is
  retained in the payment float and flagged as `udlaegSurplus = true`.

**Expected audit log behaviour**
- The dækning log entry for each inddrivelsesindsats-triggered allocation carries
  `gilParagraf = "GIL § 4, stk. 3"` and `inddrivelsesindsatsType` for traceability.

**Failure conditions (FR-4)**
- A surplus from an udlæg payment is applied to non-udlæg fordringer.
- The rule engine ignores `inddrivelsesindsatsType` and applies the normal GIL § 4, stk. 1
  ordering without the indsats-first rule.
- The `gilParagraf` on the dækning log entry is missing or references the wrong paragraph.

---

### FR-5: Opskrivningsfordring positioning

**Preconditions**
- A debtor has a fordring ("parent") with one or more associated opskrivningsfordringer.

**Trigger**
- Dækningsrækkefølge is computed for the debtor (application or query).

**Expected rule engine behaviour**
- Each opskrivningsfordring appears in the ordered list immediately after its parent fordring,
  not at the end of the priority category.
- If the parent fordring is already fully covered (saldo = 0), the opskrivningsfordring
  appears at the position immediately after where the parent would appear if it were still
  active (same FIFO sort key as the parent).
- When a parent fordring has multiple opskrivningsfordringer, they are ordered among
  themselves by ascending modtagelsesdato (FIFO on the opskrivningsfordring's own receipt
  date).
- Inddrivelsesrenter accrued on an opskrivningsfordring are covered before its principal,
  following the same FR-3 interest sequence.

**Expected API response**
- Each opskrivningsfordring entry in the response carries `opskrivningAfFordringId` set to
  the parent fordring's UUID.
- The position of the opskrivningsfordring is numerically immediately after the parent
  (consecutive rank).

**Failure conditions (FR-5)**
- An opskrivningsfordring appears at the bottom of its priority category instead of after
  its parent.
- Multiple opskrivningsfordringer for the same parent are not ordered by FIFO on their own
  modtagelsesdato.
- The `opskrivningAfFordringId` field is absent from the API response entry.
- Inddrivelsesrenter of an opskrivningsfordring are not covered before its principal.

---

### FR-6: Timing — GIL § 4, stk. 4

**Preconditions**
- A payment is received at time T1 (betalingstidspunkt).
- A new fordring arrives at time T2, where T1 < T2 < T3 (application time).

**Trigger**
- Payment application is executed at T3.

**Expected rule engine behaviour**
- The fordring that arrived at T2 is included in the ordering at application time T3,
  even though it post-dates the betalingstidspunkt T1.
- The dækning takes legal effect from T1 (betalingstidspunkt), not T3.

**Expected dækning record**
- `applicationTimestamp` = T3 (when the rule engine ran).
- `betalingstidspunkt` = T1 (when the debtor lost control of the funds).
- Both fields are present on every `DaekningRecord` entity.

**Failure conditions (FR-6)**
- The ordering snapshots the fordringer at receipt time T1, excluding fordringer that
  arrive between T1 and T3.
- The dækning record stores only the application timestamp, not the betalingstidspunkt.
- The `betalingstidspunkt` field is missing from CLS audit log entries.

---

### FR-7: Payment application API

**Preconditions**
- The caller holds OAuth2 scope `payment-service:read` and a role granting access to the
  debtor.

**Expected GET /debtors/{debtorId}/daekningsraekkefoelge behaviour**
- Returns HTTP 200 with an ordered array of positions for the debtor's active fordringer.
- Each position includes: `fordringId`, `fordringshaverId`, `prioritetKategori`, `komponent`,
  `tilbaestaaendeBeloeb`, `modtagelsesdato`, `fifoSortKey`, `gilParagraf`.
- Optional `asOf` query parameter returns the ordering as it would have been at that date.
- Returns HTTP 404 if the debtor does not exist.
- Returns HTTP 403 if the caller lacks access to the debtor.

**Expected POST /debtors/{debtorId}/daekningsraekkefoelge/simulate behaviour**
- Accepts a JSON body with `beloeb` (payment amount to simulate) and optional
  `inddrivelsesindsatsType`.
- Returns HTTP 200 with the same ordered array as the `GET`, augmented with `daekningBeloeb`
  and `fullyCovers` per position.
- Does not persist any changes to the database.
- Returns HTTP 422 if `beloeb` is zero or negative.

**Failure conditions (FR-7)**
- Either endpoint is missing `gilParagraf` from the response.
- `simulate` persists changes to the database.
- `simulate` returns HTTP 201 (Created) instead of HTTP 200.
- The `GET` endpoint ignores the `asOf` parameter and always returns the current state.

---

### FR-8: Sagsbehandler portal — dækningsrækkefølge view

**Preconditions**
- The sagsbehandler is authenticated with a role that grants access to the debtor's case.

**Trigger**
- The sagsbehandler navigates to the dækningsrækkefølge view from the debtor's case overview.

**Expected portal behaviour**
- The view displays a numbered list of positions, ordered as returned by the API (FR-7).
- Each row shows: rank, fordring reference, fordringshaver name, GIL § 4 priority category
  label (translated Danish), cost component type, outstanding amount, and modtagelsesdato.
- Opskrivningsfordring rows include a visual indicator linking them to their parent fordring.
- The view is read-only; no dækning actions can be initiated from this view.
- The view is reachable via the debtor's case overview page.

**Failure conditions (FR-8)**
- The priority category is displayed as a code (`ANDRE_FORDRINGER`) instead of a translated
  Danish label (`Andre fordringer`).
- Opskrivningsfordring rows do not indicate their parent fordring.
- The view allows any form of dækning or payment initiation.
- The view is not accessible from the debtor case overview.

---

## Acceptance criteria

**AC-1:** When a debtor has fordringer in all four priority categories and a partial payment
is applied, category-1 fordringer are fully covered before any category-2 fordring is
touched; category-2 before category-3; category-3 before category-4 (FR-1). *(Gherkin
scenario: priority ordering across all four categories)*

**AC-2:** Tvangsbøder are classified in priority category 2 (GIL § 10b / lov nr. 288/2022),
not category 4 (FR-1). *(Gherkin scenario: tvangsbøder classified as bøder-category)*

**AC-3:** Within a priority category, the fordring with the earlier modtagelsesdato is
covered first (FR-2). *(Gherkin scenario: FIFO within same category)*

**AC-4:** For a fordring received before 1 September 2013, the `legacyModtagelsesdato`
field is used as the FIFO sort key (FR-2). *(Gherkin scenario: pre-2013 legacy
modtagelsesdato)*

**AC-5:** A partial payment covering only part of a fordring is applied to opkrævningsrenter
before any inddivelsesrenter, and to all interest sub-positions before the Hovedfordring
(FR-3). *(Gherkin scenario: interest before principal)*

**AC-6:** The interest coverage sequence within a fordring follows sub-positions 1–5 in
ascending order, and the principal is only touched after all five sub-positions are
exhausted (FR-3). *(Gherkin scenario: full interest sub-position sequence)*

**AC-7:** A payment from a lønindeholdelse inddrivelsesindsats covers the
indsats-fordringer first, then surplus covers other lønindeholdelse-coverable fordringer
(FR-4). *(Gherkin scenario: lønindeholdelse indsats — surplus to same-type fordringer)*

**AC-8:** A surplus from an udlæg payment does not flow to non-udlæg fordringer; the
surplus is flagged `udlaegSurplus = true` (FR-4). *(Gherkin scenario: udlæg exception)*

**AC-9:** An opskrivningsfordring appears immediately after its parent fordring in the
ordered list (FR-5). *(Gherkin scenario: opskrivningsfordring positioned after parent)*

**AC-10:** When a parent fordring is already fully covered, the opskrivningsfordring takes
the position it would have had if the parent were still active (FR-5). *(Gherkin scenario:
opskrivningsfordring after already-covered parent)*

**AC-11:** Multiple opskrivningsfordringer for the same parent fordring are ordered among
themselves by FIFO (FR-5). *(Gherkin scenario: FIFO between multiple opskrivningsfordringer)*

**AC-12:** A fordring that arrives after the betalingstidspunkt but before the application
timestamp is included in the ordering (FR-6). *(Gherkin scenario: late-arriving fordring
included at application time)*

**AC-13:** Every dækning record carries both `applicationTimestamp` and `betalingstidspunkt`;
the CLS audit log entry includes both fields and `gilParagraf` (FR-6, NFR-1, NFR-2).
*(Gherkin scenario: dækning logged with GIL § 4 reference)*

**AC-14:** `GET /debtors/{debtorId}/daekningsraekkefoelge` returns HTTP 200 with ordered
array; each position includes `gilParagraf`, `prioritetKategori`, `fifoSortKey`, and
`komponent` (FR-7). *(Gherkin scenario: API returns ordered list)*

**AC-15:** `POST .../simulate` returns HTTP 200 with projected `daekningBeloeb` per position
and does not persist any changes (FR-7). *(Gherkin scenario: simulate does not persist)*

**AC-16:** The sagsbehandler portal displays the ordered list with translated GIL § 4
category labels and a visual link from each opskrivningsfordring row to its parent (FR-8).
*(Gherkin scenario: portal displays ordered list with GIL § 4 labels)*

**AC-17:** All new i18n labels for priority categories and cost component types are present
in both `messages_da.properties` and `messages_en_GB.properties`.
> **Verification method:** AC-17 is verified by the CI bundle-lint check (build fails if
> any key present in `messages_da.properties` is absent from `messages_en_GB.properties`
> and vice versa), not by a Gherkin scenario.

---

## Deliverables table with AC cross-references

| Deliverable | AC coverage |
|-------------|-------------|
| `DaekningsRaekkefoeigenService` | AC-1 through AC-12 |
| `PrioritetKategori` enum | AC-1, AC-2, AC-14, AC-16 |
| `RenteKomponent` enum | AC-5, AC-6, AC-14 |
| `DaekningRecord` entity | AC-13 |
| `DaekningsRaekkefoeigenController` (GET + POST) | AC-14, AC-15 |
| OpenAPI spec | AC-14, AC-15 |
| Sagsbehandler portal view template | AC-16 |
| Sagsbehandler portal view controller | AC-16 |
| Liquibase migration | AC-4 (legacyModtagelsesdato), AC-13 (DaekningRecord table) |
| DA + EN i18n message bundles | AC-17 |

---

## Failure conditions (summary)

- A lower-priority fordring receives dækning while a higher-priority fordring remains outstanding.
- Tvangsbøder are classified as category 4 instead of category 2.
- Within a category, a later-received fordring is covered before an earlier-received one.
- Pre-2013 fordringer are sorted by overdragelse API timestamp instead of `legacyModtagelsesdato`.
- Principal receives dækning while any interest component of the same fordring remains outstanding.
- Interest sub-positions are covered in the wrong order (e.g., sub-position 4 before sub-position 2).
- Surplus from an udlæg payment flows to non-udlæg fordringer.
- An opskrivningsfordring appears at the bottom of its priority category instead of after its parent.
- The dækning record is missing `betalingstidspunkt` or `applicationTimestamp`.
- A dækning event is not logged to CLS.
- A CLS log entry is missing `gilParagraf` or `prioritetKategori`.
- The `simulate` endpoint persists changes to the database.
- The portal displays priority category codes instead of translated Danish labels.
- Any AC-17 i18n key is missing from DA or EN message bundle.
- `behave --dry-run` fails on the feature file.

---

## Definition of done (outcome-contract view)

- The rule engine produces an identical ordered list for identical inputs on every run
  (determinism requirement — NFR-1).
- The CLS audit log for every dækning event includes `gilParagraf`, `prioritetKategori`,
  `fifoSortKey`, `betalingstidspunkt`, and `applicationTimestamp`.
- The `GET` endpoint returns an ordered list with all required fields for any active debtor.
- The `POST simulate` endpoint returns a projected allocation without side effects.
- The sagsbehandler portal renders the ordered list with translated labels and opskrivnings-
  fordring parent links.
- All acceptance criteria AC-1 through AC-16 are covered by at least one Gherkin scenario.
- AC-17 is verified by the CI bundle-lint check.
- `behave --dry-run` passes on `petitions/petition057-daekningsraekkefoeigen.feature`.
