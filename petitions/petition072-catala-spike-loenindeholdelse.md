# Petition 072: Catala Compliance Spike — Lønindeholdelsesprocent Gæld.bekendtg. § 14 (companion til P062)

## Summary

Gennemfør et tidsafgrænset (2 arbejdsdage) research spike for at fastslå, om
[Catala](https://catala-lang.org/) kan fungere som et formelt compliance-specifikationslag
for beregningen af lønindeholdelsesprocenten i OpenDebt. Spiket koder
lønindeholdelsesprocent-formlen fra Gæld.bekendtg. § 14, stk. 2 — den samme lovmæssige
domæne som petition 062 — og producerer en Go/No-Go-anbefaling med evidens.

**Type:** Research spike — ingen produktionskode leveres.  
**Time box:** 2 arbejdsdage.  
**G.A.-snapshot:** v3.16 (2026-03-28).  
**Referencer:** Petition 062 (lønindeholdelse — fuld spec), G.A.3.1.2.5,
Gæld.bekendtg. § 14, stk. 2–3; GIL §§ 10, 10a; SKM2015.718.ØLR.  
**Afhænger af:** Petition 062 (P072 refererer til de samme Gherkin-scenarier som P062 producerer).  
**Companion til:** Petition 054 (Catala-spike for G.A.1.4.3/1.4.4 — etablerer Catala-rammen).

---

## Kontekst og motivation

Den juridiske vejledning (G.A. Inddrivelse) definerer de lovregler, som OpenDebt skal
implementere. I dag er disse regler repræsenteret som:

1. **Narrative petitioner** — menneskelig-læsbare krav med G.A.-referencer.
2. **Gherkin feature files** — eksekverbare scenarier, der udtrykker forventet portal- og
   backend-adfærd.
3. **Specifikationsdokumenter** — class-niveau implementeringskontrakter produceret af pipelinen.

Denne kæde fungerer, men har et hul: der er ingen *formel, eksekverbar repræsentation af loven
selv*. En juridisk regel kodet udelukkende i naturligt sprog og Gherkin kan stille og roligt
afvige fra G.A.-teksten. Fejl i denne oversættelse opdages sent — typisk i acceptancetest eller
ved juridisk review — og er dyre at rette.

**Catala** er et domænespecifikt sprog udviklet af Inria og det franske finansministerium
specielt til kodning af lovgivningstekster som eksekverbare programmer. Det er blevet anvendt til
at formalisere den franske skattelov og britiske velfærdsregler. Dets vigtigste egenskab er, at
hvert regelblok er *forankret til en specifik artikel* i kildelovgivningen, hvilket gør
kodningen bidirektionalt sporbar og reviderbar.

Hypotesen er: hvis Gæld.bekendtg. § 14, stk. 2 kan kodes i Catala uden tvetydighed, kan
Catala-programmet fungere som et *orakel* for OpenDebt-testsuiten — og generere autoritative
testcases, der opdager implementeringsfejl, som Gherkin alene ikke kan detektere.

### Hvorfor Gæld.bekendtg. § 14 og G.A.3.1.2?

Lønindeholdelsesprocent-beregningen er spikets primære fokus, fordi:

- **Formelkritiske fejl har direkte regulatoriske konsekvenser**: Over-indeholdelse krænker
  debitors beskyttelsesregler; under-indeholdelse sikrer ikke det lovpligtige afdragsbeløb.
- **Formlen indeholder præcisionsfælder**: Fast-punkt vs. floating-point aritmetik,
  nedrunding (floor) vs. standard-afrunding, og en denominator-nulguard for frikort-tilfældet.
- **P062 har 40+ Gherkin-scenarier** som Catala-kodningen skal sammenlignes med — dette giver
  et konkret sammenligningsgrundlag.
- **Spiket er en kompagnon til P054** (Catala for G.A.1.4.3/1.4.4) og bruger det samme
  Catala-toolchain, men i det beregningskritiske lønindeholdelses-domæne.

Disse er netop de regeltyper, hvor formelle verifikationsværktøjer giver størst nytte, og
hvor risikoen for fejlimplementering er højest.

### Domæne-termer

| Dansk | Engelsk / teknisk | Definition |
|-------|-------------------|------------|
| Catala | Catala | Open-source DSL til formalisering af lovtekster; kompilerer til OCaml/Python |
| Formalisering | Formalization | Kodning af juridiske regler i et maskineksekverbart sprog |
| Orakel | Oracle | En autoritativ eksekverbar specifikation brugt til at generere eller validere testcases |
| Lønindeholdelsesprocent | Wage garnishment rate | Den procentdel der indeholdes af debitors A-indkomst |
| Afdragsprocent | Instalment rate | Procentsats fastsat efter tabeltræk (Gæld.bekendtg. § 11, stk. 1) |
| Nettoindkomst | Net income | Årsindkomst efter AM-bidrag, opgjort efter § 11, stk. 1 |
| Fradragsbeløb | Tax deduction amount | Årligt skattekortfradrag fra eSkattekortet |
| Trækprocent | Withholding rate | Personlig skatteprocent fra debitors forskudsopgørelse |
| Nedrunding | Floor rounding | Altid rundet ned til nærmeste hele procent (SKM2015.718.ØLR) |
| Frikort | Tax exemption card | Skattefritagelse; særlig kanthåndtering i formlen |
| Reduceret sats | Reduced rate | GIL § 10a — reduceret lønindeholdelsesprocent for bestemte fordringtyper |
| eSkattekortet | Digital tax card | Kildesystem for trækprocent og fradragsbeløb |

---

## Juridisk grundlag

| Reference | Indhold relevant for spiket |
|-----------|-----------------------------|
| G.A.3.1.2.5 | Lønindeholdelsesprocenten — regler og beregningsmetoder (snapshot v3.16 2026-03-28) |
| Gæld.bekendtg. § 14, stk. 2 | Lønindeholdelse sker med en procentdel af A-indkomst reduceret med fradragsbeløbet |
| Gæld.bekendtg. § 14, stk. 3, 2.–4. pkt. | Omregningsformlen: afdragsprocent × nettoindkomst / (nettoindkomst − fradragsbeløb) / (1 − trækprocent/100) |
| Gæld.bekendtg. § 14, stk. 3, 5. pkt. | Nedrunding til nærmeste hele procent (floor) |
| Gæld.bekendtg. § 14, stk. 3, 9. pkt. | Maksimal samlet indeholdelsesprocent (trækprocent + lønindeholdelsesprocent ≤ 100 %) |
| Gæld.bekendtg. § 11, stk. 1 | Tabeltræk — afdragsprocent-opslag efter årsindkomstbracket |
| GIL § 10 | Lønindeholdelse — legal hjemmel og omfang |
| GIL § 10a | Reduceret lønindeholdelsesprocent |
| Kildeskattelovens § 48, stk. 5 | Cap-reglen: samlet indeholdelsesprocent ≤ 100 % |
| SKM2015.718.ØLR | Domspræcedens: lønindeholdelsesprocent skal nedrundes |
| SKM2009.7.SKAT | Administrativ praksis for lønindeholdelseseligibilitet |

G.A.-snapshot: v3.16 (2026-03-28).

---

## Funktionelle krav

### FR-1: Catala-kodning af kerneformlen (Gæld.bekendtg. § 14, stk. 3, 2.–4. pkt.)

Producér en Catala-kildefil (`catala/ga_3_1_2_loenindeholdelse_pct.catala_da`) der formelt
koder omregningsformlen for lønindeholdelsesprocenten:

```
lønindeholdelsesprocent = floor(
  (afdragsprocent × nettoindkomst)
  ──────────────────────────────────────────────────
  (nettoindkomst − fradragsbeløb) × (1 − trækprocent/100)
)
```

Hvor:
- `afdragsprocent` — output fra tabeltræk (§ 11, stk. 1); P071-parameter-afhængighed
- `nettoindkomst` — årsindkomst efter AM-bidrag, opgjort efter § 11, stk. 1
- `fradragsbeløb` — årligt skattekortfradrag fra eSkattekortet
- `trækprocent` — debitorens personlige skatteprocent fra eSkattekortet

**Konkret beregningseksempel** (G.A.3.1.2.5, 2026-satser):
- nettoindkomst = 400.000 kr., fradragsbeløb = 48.000 kr., trækprocent = 37 %, afdragsprocent = 10 %
- (10 × 400.000) / ((400.000 − 48.000) × (1 − 0,37)) = 4.000.000 / (352.000 × 0,63) = 4.000.000 / 221.760 = 18,04 % → **18 %** (nedrundes)

Catala-kodningen skal reproducere dette præcise resultat.

Hvert regelblok skal forankres til sin kildelovhjemlel i Catala-kilden via Catala's
artikelcitations-syntaks.

- **FR-1.1** Standardformlen: Gæld.bekendtg. § 14, stk. 3, 2.–4. pkt.
- **FR-1.2** Nedrunding (floor): Gæld.bekendtg. § 14, stk. 3, 5. pkt.
- **FR-1.3** Maksimumsbegrænsning (cap): samlet indeholdelsesprocent ≤ 100 % (Gæld.bekendtg. § 14,
  stk. 3, 9. pkt.; kildeskattelovens § 48, stk. 5)

### FR-2: Catala-kodning af frikort-kanttilfældet

Koden i `catala/ga_3_1_2_loenindeholdelse_pct.catala_da` skal kode vagtkonditionerne for
frikort-scenariet:

- **FR-2.1** Når `(nettoindkomst − fradragsbeløb) ≤ 0`: denominatoren nærmer sig nul →
  systemet falder tilbage til bruttoindkomst-beregning: `lønindeholdelsesprocent = afdragsprocent`
  (ingen skattekorrekturjustering er nødvendig, fordi arbejdsgiveren ikke trækker A-skat).
- **FR-2.2** Når `trækprocent = 0 %` (frikort uden skatteforpligtelse): standardformlen
  anvendes; denominatorfaktor = (1 − 0) = 1 (ingen ændring).
- **FR-2.3** Validering: kombinationen `(nettoindkomst − fradragsbeløb) ≤ 0` og
  `trækprocent > 0` skal trigge fallback, ikke division-med-nul-fejl.

Vagtkonditionerne skal kodes som separate Catala-regler med eksplicitte artikelankere.

### FR-3: Catala-kodning af reduceret sats og fordringtype-klassifikation

Koden skal kode klassifikationsprædikatet for reduceret lønindeholdelsesprocent (GIL § 10a):

- **FR-3.1** Hvilke fordringtyper kvalificerer til reduceret sats — klassifikationsprædikatet
  kodes som en Catala enumeration.
- **FR-3.2** Reduceret sats anvendes, når prædikatet er opfyldt og ikke-reduceret sats anvendes
  ellers.
- **FR-3.3** eSkattekortet-afhængighed: trækprocent injiceres som en parameter (ingen HTTP-kald
  i Catala) — CPR behandles som ephemært input og persisteres ikke.

### FR-4: Catala-testsuiten

Producér en Catala-testfil (`catala/tests/ga_loenindeholdelse_tests.catala_da`) med mindst 8 tests
der dækker alle regelgrene i FR-1 og FR-2:

- **FR-4.1** Standardformel med konkrete værdier: 10 % / 400.000 kr. / 48.000 kr. / 37 % → 18 %
- **FR-4.2** Nedrunding: resultat med decimaler rundes ned (ikke op)
- **FR-4.3** Frikort (trækprocent = 0 %): formel med denominator = 1
- **FR-4.4** Frikort-kant (nettoindkomst ≈ fradragsbeløb): fallback til afdragsprocent direkte
- **FR-4.5** Maksimumsbegrænsning: resultat over cap → cap anvendes (floor på det cappede resultat)
- **FR-4.6** Reduceret sats: kvalificerende fordringtype → reduceret procent
- **FR-4.7** Fast-punkt præcision: mellemresultater verificeres mod loveksemplet
- **FR-4.8** Parameter-afhængighed: afdragsprocent fra P071-output injiceres korrekt

Tests skal bruge Catala's built-in `Test`-modul og alle tests skal passere.

### FR-5: Sammenligningsrapport mod P062 Gherkin-scenarier

Producér en markdownrapport (`catala/SPIKE-REPORT-072.md`) der sammenligner:

1. **Dækning**: Hvilke af P062's Gherkin-scenarier (fra
   `petitions/petition062-loenindeholdelse-fuld-spec.feature`) er dækket af Catala-testsuiten?
   Hvilke er ikke — primært med fokus på de formelkritiske scenarier (FR-2 og FR-3 i P062)?
2. **Huller**: Afslørede Catala-kodningen regelgrene, der ikke er dækket af P062 Gherkin?
3. **Uoverensstemmelser**: Afslørede Catala-kodningen tilfælde, hvor P062-scenarier tilsyneladende
   modsiger G.A.-teksten?
4. **Kritiske diskrepanser**: Er nedrunding, fast-punkt-aritmetik, frikort-vagtbetingelsen eller
   cap-rækkefølgen implementeret forkert i P062's Gherkin?
5. **Indsatsskøn**: Hvor mange persondage kræves det at kode det fulde G.A.3.1.2-kapitel i
   Catala med samme præcisionsniveau?

### FR-6: Go/No-Go-anbefaling

Spikreapporten skal indeholde en eksplicit Go/No-Go-anbefaling med evidens:

- **Go-kriterier** (alle skal opfyldes):
  - § 14-formlen kodes uden tvetydighed i Catala
  - Nedrunding og fast-punkt-aritmetik kan udtrykkes i Catala
  - Frikort-vagtkondition kan udtrykkes rent i Catala
  - Det konkrete 18 %-eksempel reproduceres korrekt i Catala-tests
  - Mindst 1 uoverensstemmelse fundet vs. P062 Gherkin (demonstrerer værdi)
  - Catala kompilerer uden fejl
- **No-Go-kriterier** (et enkelt trigger No-Go):
  - Fast-punkt-aritmetik kræver eksternt bibliotek, der ikke er tilgængeligt i Catala
  - Frikort-kanttilfældet kan ikke udtrykkes rent uden workarounds
  - Indsats overstiger 4 persondage pr. G.A.-afsnit

---

## Ikke-funktionelle krav

- **NFR-1:** Catala-kilden skal kompilere uden fejl med Catala CLI
  (`catala ocaml` eller `catala python`-udtrækning).
- **NFR-2:** Catala-kilden skal skrives på dansk (`catala_da`) for at matche G.A.-kildesproget
  og understøtte tovejssporbarhed med den danske juridiske tekst.
- **NFR-3:** Alle G.A.-artikelcitationer i Catala-kilden skal referere til G.A.-snapshot
  v3.16 (2026-03-28) — samme version som P062.
- **NFR-4:** Ingen produktionskode, databasemigrationer, API-ændringer eller Spring Boot-moduler
  introduceres af dette spike.
- **NFR-5:** CPR-nummeret behandles som ephemært input i Catala-kodningen og persisteres aldrig;
  Person Registry UUID bruges som reference i alle entiteter.

---

## Leverancer

| # | Artefakt | Sti | Beskrivelse |
|---|----------|-----|-------------|
| D-1 | Catala-kilde — lønindeholdelsesprocent | `catala/ga_3_1_2_loenindeholdelse_pct.catala_da` | FR-1, FR-2 og FR-3 med artikelankere |
| D-2 | Catala-testfil | `catala/tests/ga_loenindeholdelse_tests.catala_da` | FR-4 testsuite (≥ 8 tests) |
| D-3 | Spikerapport | `catala/SPIKE-REPORT-072.md` | FR-5 sammenligning + FR-6 Go/No-Go |

---

## Kritiske diskrepanspunkter (højeste udnyttelsesværdi for spiket)

Spiket skal specifikt undersøge følgende fem implementeringsrisici, da disse er de mest
sandsynlige kilder til stille, konsekvente beregningsfejl:

| # | Diskrepanspunkt | Juridisk hjemmel | Fejltype |
|---|-----------------|------------------|----------|
| 1 | **Nedrunding vs. standard-afrunding** | Gæld.bekendtg. § 14, stk. 3, 5. pkt.; SKM2015.718.ØLR | Systemer bruger ofte round-half-up; loven kræver floor |
| 2 | **Fast-punkt vs. floating-point** | Matematisk præcision ved division | Floating-point-fejl kan rykke resultatet med ±1 % |
| 3 | **Frikort-vagtkondition mangler** | G.A.3.1.2.5 (fallback-beregning) | Denominator-nul-guard er ofte udeladt i implementationer |
| 4 | **Cap anvendes før vs. efter nedrunding** | Gæld.bekendtg. § 14, stk. 3, 9. pkt. | Rækkefølge er juridisk fastsat — nedrunding gælder på det cappede resultat |
| 5 | **Reduceret sats-klassifikation** | GIL § 10a | Hvilke fordringtyper kvalificerer — ofte en implicit antagelse |

---

## Forholdet til P071 (afdragsordninger)

`afdragsprocent` er et INPUT til P072-formlen. Catala-kodningen skal referere til eller importere
P071's tabeltræk-output som en parameter (dependency injection). P072 koder ikke selve tabeltræk-
opslaget — det er P071's ansvar.

---

## Afgrænsning

Følgende er eksplicit udelukket fra dette spike:

| Punkt | Årsag |
|-------|-------|
| Kodning af andre G.A.-afsnit end G.A.3.1.2.5 | Tidsafgrænset; scope valgt for tractability og P062-overlap |
| Kodning af betalingsevnevurdering (§ 18) | Separat formularedifferentieringsregelsæt; kan følge som FR-2.b |
| Runtime-integration med Spring Boot eller OpenDebt-tjenester | Spike kun; integration er en follow-on petition ved Go |
| CI-pipeline-integration for Catala-kompilering | Follow-on ved Go |
| Fuld G.A.3.1.2-kapiteldækning | Follow-on multi-petition-program ved Go |
| Varsel- og afgørelsesprocedure (G.A.3.1.2.4) | Ikke formelkritisk; dækket af P062 FR-6-FR-9 |
| eSkattekortet-HTTP-integration | Kræver runtime; CPR må ikke persisteres i spiket |
| Tværgående lønindeholdelse (G.A.3.1.2.1.2) | Koordinationslogik, ikke procentberegning |

---

## Definition of Done

- [ ] D-1 kompilerer uden fejl (`catala ocaml ga_3_1_2_loenindeholdelse_pct.catala_da` fra `catala/`-mappen)
- [ ] D-2 testsuite eksekverer med alle tests bestående
- [ ] D-3 spikerapport indeholder eksplicit Go/No-Go med evidens for hvert kriterium
- [ ] D-3 sammenligningstabel dækker alle P062 FR-2 og FR-3 Gherkin-scenarier
- [ ] Mindst én hul- eller uoverensstemmelsesfinding dokumenteret (eller eksplicit bekræftet som fraværende)
- [ ] Ingen produktionsfiler modificeret; ingen migrationer, API-specifikationer eller Java-kilde ændret

---

## Beslutningsgate

Dette petition afsluttes med en binær beslutning logget i `petitions/program-status.yaml`:

**Ved Go:** Bestil petition 073 til at etablere en Catala compliance-verifikationspipeline
(CI-integreret, der dækker et bredere sæt G.A.-afsnit). P062 Gherkin-scenarierne suppleres med
Catala-genererede cases som autoritative regressionstest.

**Ved No-Go:** Dokumenter blokeringsårsagen i `catala/SPIKE-REPORT-072.md` og luk udforskningen.
Den aktuelle petition + Gherkin + specs-pipeline forbliver det autoritative compliance-lag.
Ingen yderligere investering i Catala for lønindeholdelses-domænet uden ny evidens.
