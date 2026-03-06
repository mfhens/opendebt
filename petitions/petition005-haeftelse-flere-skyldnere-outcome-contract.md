# Petition 005 Outcome Contract

## Acceptance criteria

1. OpenDebt represents the relation between `fordring` and `skyldner` through an explicit `hæftelse` object.
2. OpenDebt supports one `fordring` with one liable party through `enehæftelse`.
3. OpenDebt supports one `fordring` with multiple jointly liable parties through `solidarisk hæftelse`.
4. OpenDebt supports one `fordring` with multiple parties responsible for separate shares through `delt hæftelse`.
5. Each `hæftelse` links exactly one `fordring` and one `skyldner`.
6. A `delt hæftelse` preserves the relevant share or amount per liable relation.
7. Communication and collection handling can resolve the relevant liable parties from the registered `hæftelser`.

## Definition of done

- Single-liable and multiple-liable claim structures are testable.
- Liability type is testable.
- The share-preservation requirement for `delt hæftelse` is testable.
- Downstream use of liable parties in communication and collection handling is testable at a high level.
- Every acceptance criterion is covered by at least one Gherkin scenario.

## Failure conditions

- OpenDebt still assumes exactly one `skyldner` per `fordring`.
- Liability type cannot be distinguished.
- `Delt hæftelse` loses the defined portion per liable party.
- Downstream communication or collection logic cannot determine all relevant liable parties.
