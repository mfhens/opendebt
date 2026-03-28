# Petition 053: Fordringshaverportal — Opskrivning og nedskrivning (fuld G.A.-komplient specifikation)

## Summary

The Fordringshaverportal shall implement a complete, legally compliant workflow for creditor-submitted
write-ups (opskrivninger) and write-downs (nedskrivninger) on claims under inddrivelse. This petition
supersedes the rudimentary specification in petition 034 and grounds every requirement directly in
G.A.1.4.3, G.A.1.4.4, and gældsinddrivelsesbekendtgørelsens § 7.

**Supersedes:** Petition 034 (opskrivning og nedskrivning — basic portal flow).
**References:** Petition 003 (fordring lifecycle), Petition 008 (creditor agreement permissions),
Petition 012 (BFF), Petition 030 (claim detail), Petition 031 (fordringer i høring).

---

## Context and motivation

After a fordring is accepted for inddrivelse, the fordringshaver may need to adjust its amount:

- **Opskrivning (G.A.1.4.3):** The fordring was reported with too small an amount at overdragelse.
  The fordringshaver submits a separate opskrivningsfordring to cover the difference.
- **Nedskrivning (G.A.1.4.4):** The fordring must be reduced because (a) the fordringshaver has
  received a payment directly, (b) part of the fordring was sent to inddrivelse in error, or
  (c) the opkrævningsgrundlag (assessment basis) has changed.

The current implementation (petition 034) models the basic form flow but does not enforce the
legal rules that govern when and how these adjustments may be submitted. This petition defines
those rules and the corresponding portal behaviour.

---

## Legal basis / G.A. references

| Reference | Content |
|---|---|
| G.A.1.4.3 | Opskrivninger af fordringer — definition, PSRM procedure, høring timing rule, annulleret nedskrivning |
| G.A.1.4.4 | Nedskrivning af fordringer — three valid grounds, retroactive reassignment obligation, cross-system suspension, rentegodtgørelse alternative |
| G.A.2.3.4.4 | Interne opskrivninger — RIM-internal corrections; **not** fordringshaver actions |
| G.A.2.4.4.2.1 | Tillægsfrist på 6 måneder ved interne opskrivninger (forældelsesfrist) |
| Gæld.bekendtg. § 7, stk. 1 | Opskrivningsfordring modtagelsestidspunkt (inkl. undtagelser for høring og annulleret nedskrivning) |
| Gæld.bekendtg. § 7, stk. 2 | Nedskrivning — tre gyldige grundlag |
| GIL § 18 k | RIM-suspension ved retroaktiv nedskrivning over systemgrænser |
| GIL § 18 l | Rentegodtgørelse som alternativ til fuld retroaktiv tidslinjegenberegning |

---

## PSRM Reference Context

In PSRM, fordringshaver-submitted adjustments are sent as separate fordringer (opskrivningsfordring)
or as nedskrivning messages, not as in-place mutations of the original fordring's balance. PSRM
distinguishes:

- **Opskrivningsfordring:** A new fordring submitted on top of the existing one, with the same
  obligationId. It is subject to the same dækningsrækkefølge as other fordringer from the same
  fordringshaver (G.A.1.4.3, gæld.bekendtg. § 7).
- **Rentefordring:** Used specifically when the item being opskrevet is an opkrævningsrente.
  A rentefordring carries the same renteperiode as the original, not a new accrual start date
  (G.A.1.4.3, 3. pkt.).
- **Nedskrivning:** Sent as a balance reduction message with a virkningsdato. If retroactive,
  triggers PSRM's dækning reassignment and interest recalculation logic (G.A.1.4.4).

**Interne opskrivninger (G.A.2.3.4.4) are NOT fordringshaver portal actions.** They are
RIM-internal corrections triggered by dækning reversal (e.g. dækningsløs indbetaling, omplacering
af dækninger, tilbageførsel af afskrivning). The fordringshaver portal must not expose these as
selectable actions and must not use their PSRM codes (DINDB, OMPL, AFSK) in fordringshaver-facing
forms.

---

## Functional requirements

### FR-1: Nedskrivning — controlled reason selection

The nedskrivning form shall present the reason for the nedskrivning as a **controlled dropdown**,
not free text. The valid reasons are exactly those defined by gæld.bekendtg. § 7, stk. 2:

| Reason code | Danish label | G.A. / legal reference |
|---|---|---|
| `NED_INDBETALING` | Direkte indbetaling til fordringshaver | G.A.1.4.4, gæld.bekendtg. § 7 stk. 2 nr. 1 |
| `NED_FEJL_OVERSENDELSE` | Fejlagtig oversendelse til inddrivelse | G.A.1.4.4, gæld.bekendtg. § 7 stk. 2 nr. 2 |
| `NED_GRUNDLAG_AENDRET` | Opkrævningsgrundlaget har ændret sig | G.A.1.4.4, gæld.bekendtg. § 7 stk. 2 nr. 3 |

The selected reason code shall be forwarded to debt-service as part of the adjustment message.
A missing or unrecognised reason code shall cause form rejection before submission.

### FR-2: Opskrivning — opkrævningsrente exception (G.A.1.4.3, 3. pkt.)

When the fordring being opskrevet has claim category `RENTE` (opkrævningsrente), the portal
shall **reject the OPSKRIVNING_REGULERING path** with a user-facing message explaining that
opkrævningsrenter cannot be opskrevet via opskrivningsfordring.

The message shall instruct the fordringshaver to instead submit a **ny rentefordring** with the
same renteperiode as the original claim. The portal claim creation flow (petition 002) shall be
used for this purpose; the adjustment form must redirect the user accordingly.

The backend (debt-service) shall enforce this constraint independently of the portal.

### FR-3: Opskrivning — høring timing banner (G.A.1.4.3, gæld.bekendtg. § 7 stk. 1, 4. pkt.)

When the fordringshaver navigates to the adjustment form for a claim whose lifecycle state is
`HOERING`, the portal shall display a persistent informational banner before the form:

> *"Denne fordring er i høring (afventer bekræftelse eller rettelse hos fordringshaver). En
> opskrivning indsendt nu anses først for modtaget hos restanceinddrivelsesmyndigheden, når
> høringen er afgjort og registreret. Rentetilskrivning på det opskrevne beløb begynder på
> det tidspunkt — ikke på indsendelstidspunktet."*

The banner shall appear for both write-up and write-down operations. The form shall remain
submittable; the banner is informational only. The corresponding timing rule shall also be
enforced in the backend.

### FR-4: Retroactive nedskrivning — user guidance (G.A.1.4.4)

When the fordringshaver enters a `virkningsdato` that is earlier than the current date (retroactive
nedskrivning), the form shall display an inline advisory after the virkningsdato field:

> *"Virkningsdato er i fortiden. Gældsstyrelsen er forpligtet til at omfordele alle dækninger
> foretaget efter denne dato og genberegne renter for den berørte periode. Behandlingen kan tage
> ekstra tid. Se G.A.1.4.4."*

The advisory shall not block submission. Debt-service shall log retroactive nedskrivninger
separately for operational monitoring.

### FR-5: Annulleret nedskrivning — backdated opskrivning (G.A.1.4.3, gæld.bekendtg. § 7 stk. 1, 5. pkt.)

When an opskrivning is submitted to reverse (annullere) a nedskrivning that was incorrectly
made while the fordring was under inddrivelse, the opskrivningsfordring is treated as received at
**the same time as the original fordring** (backdated), not at the time of the opskrivning
submission. This is the `OPSKRIVNING_OMGJORT_NEDSKRIVNING_REGULERING` type.

The portal shall inform the fordringshaver of this backdating effect in the type description:

> *"Opskrivning af annulleret nedskrivning: opskrivningsfordringen anses for modtaget på samme
> tidspunkt som den fordring, der fejlagtigt blev nedskrevet (G.A.1.4.3)."*

### FR-6: Cross-system retroactive nedskrivning — suspension advisory (G.A.1.4.4, GIL § 18 k)

When a nedskrivning's virkningsdato predates the fordring's PSRM registration date (i.e., the
fordring was previously registered in another inddrivelsessystem such as DMI), the portal shall
display an advisory on the receipt page:

> *"Restanceinddrivelsesmyndigheden kan suspendere inddrivelsen af denne fordring midlertidigt,
> mens korrektionen behandles, hvis korrektionen ikke kan gennemføres straks (GIL § 18 k).
> Suspensionen ophæves automatisk, når korrektionen er registreret."*

### FR-7: Scope exclusion — interne opskrivninger are not fordringshaver actions

The portal shall **not** expose reason codes or action types that correspond to RIM-internal
opskrivninger (G.A.2.3.4.4):

- `DINDB` (dækningsløs indbetaling) — triggered by bank rejection, not fordringshaver
- `OMPL` (omplacering af dækninger) — triggered by RIM's internal correction
- `AFSK` (tilbageførsel af afskrivning) — triggered by court reversal of gældssanering

If these codes are currently exposed in `WriteUpReasonCode`, they shall be removed. Fordringshaver
opskrivning types are fully captured by the `ClaimAdjustmentType` enum (FR-1 to FR-6 above).

### FR-8: Update type availability per creditor agreement (retained from petition 034)

Available update types shall remain governed by the creditor agreement's `grantedAdjustmentPermissions`
flags. FR-1 through FR-7 add legal constraints on top of the permission-based filtering already
implemented; they do not replace it.

### FR-9: Backend enforcement independent of portal

All constraints defined in FR-1 through FR-6 shall be enforced at the debt-service API layer,
independently of the portal validation. The portal validation is a UX aid; the backend is the
enforcement point.

---

## Non-functional requirements

- All new reason codes (FR-1) shall be internationalised via Spring message bundles (petition 021).
- The høring banner (FR-3) and retroactive advisory (FR-4) shall meet WCAG 2.1 AA (petition 014):
  use `role="status"` for the informational banner, `aria-live="polite"` for inline advisories.
- All adjustment submissions shall continue to be logged to the audit log (CLS) regardless of
  whether they succeed or fail.

---

## Constraints

- **G.A.1.4.3 / gæld.bekendtg. § 7 stk. 1:** An opskrivningsfordring is considered received at
  the time of registration in the modtagelsessystem — except (a) when the related claim is in
  høring (received when høring resolves) and (b) when the opskrivning reverses an incorrectly
  made nedskrivning (received at same time as original fordring).

- **G.A.1.4.4 / gæld.bekendtg. § 7 stk. 2:** A nedskrivning is only valid on three grounds.
  Submissions without a recognised ground code shall be rejected at both portal and backend.

- **G.A.1.4.3 rentefordring rule:** Opkrævningsrente adjustments must use rentefordring, not
  opskrivningsfordring. The portal redirects; the backend enforces.

- **G.A.2.3.4.4 scope exclusion:** Interne opskrivninger (DINDB/OMPL/AFSK) are RIM-internal
  operations. They are not initiated by fordringshavere and must not appear in the portal.

- **GIL § 18 l / G.A.1.4.4 rentegodtgørelse:** RIM may, at its discretion, apply a prospective
  virkningsdato and pay the debtor a rentegodtgørelse instead of executing full retroactive
  timeline replay. This is a Gældsstyrelsen-internal decision; the portal does not expose this
  choice. Implementation tracked in TB-039.

- **Lønindeholdelse scope (G.A.3.1.2.1.3):** An opskrivningsfordring submitted via the portal is
  not automatically included in an existing lønindeholdelsesafgørelse. A new afgørelse is required.
  The portal must display this limitation at the confirmation step for opskrivning submissions
  (retained from petition 034).

---

## Out of scope

- Interne opskrivninger (G.A.2.3.4.4) — RIM-internal operations, not portal actions
- Rentegodtgørelse decision by Gældsstyrelsen (GIL § 18 l) — tracked in TB-039
- Full retroactive timeline replay in debt-service/payment-service — tracked in TB-038
- Tilbagekald (withdrawal) flow — existing separate flow
- Genindsendelse (resubmission) flow — existing separate flow
