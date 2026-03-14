# Fordring Validation Rules DMN Export

This directory contains the extracted business rules from the Fordring Integration API in DMN (Decision Model and Notation) compatible formats.

## Files

### fordring-rules-decision-table.csv
A comprehensive CSV file containing all 100+ validation rules extracted from the Java rule classes. This format is suitable for:
- Import into DMN modeling tools (Camunda Modeler, Trisotech, Signavio)
- Review and analysis in spreadsheet applications
- Conversion to other DMN formats

**Columns:**
- `Rule ID` - Unique identifier matching the Java class name
- `Rule Name` - Descriptive name of the rule
- `Error Code` - The ErrorCode enum value returned on failure
- `Error Code Number` - Numeric error code
- `Description (Danish)` - Original Danish error description
- `Input: AktionKode` - Which action types trigger this rule
- `Input: Additional Conditions` - Additional business conditions checked
- `Output: Valid` - Always `false` (rules define failure conditions)
- `Hit Policy` - DMN hit policy (FIRST for most rules)

### fordring-validation-rules.dmn
A DMN 1.3 XML file containing a subset of the rules organized into decision tables:
- **Structure Validation** - Rules 403, 404, 406, 407, 412, 444, 447, 448, 458, 505
- **Currency Validation** - Rule 152
- **Art Type Validation** - Rule 411
- **Interest Rate Validation** - Rule 438
- **Date Validation** - Rules 464, 467, 548, 568
- **Claimant Permission Validation** - Rules 416, 419, 420, 421, 465, 466, 497, 501, 508, 511, 543
- **Genindsend Validation** - Rules 539, 540, 541, 542, 544

## Rule Categories

### Structure Rules (400-series)
Validate that required XML structures are present for each action type.

### Permission Rules (416, 419-421, 465-466, 497, 501, 508, 511, 543)
Validate claimant permissions from the ClaimantAgreementService.

### Data Validation Rules
- Currency must be DKK (Rule 152)
- Art type must be INDR or MODR (Rule 411)
- Interest rate must be non-negative (Rule 438)
- Dates must be valid (Rules 464, 467, 548, 568)

### Reference Validation Rules (469-477, 493-494, 502-506, 526-527)
Validate references to other actions and claims.

### Genindsend (Resubmit) Rules (539-544)
Validate claim resubmission logic.

## Action Types (AktionKode)
- `OPRETFORDRING` - Create claim
- `GENINDSENDFORDRING` - Resubmit claim
- `AENDRFORDRING` - Modify claim
- `NEDSKRIV` - Write down claim
- `TILBAGEKALD` - Withdraw claim
- `OPSKRIVNINGREGULERING` - Write up (regulation)
- `OPSKRIVNINGANNULLERETNEDSKRIVNINGINDBETALING` - Write up (cancelled write-down payment)
- `OPSKRIVNINGOMGJORTNEDSKRIVNINGREGULERING` - Write up (reversed write-down regulation)
- `NEDSKRIVNINGANNULLERETOPSKRIVNINGREGULERING` - Write down (cancelled write-up regulation)
- `NEDSKRIVNINGANNULLERETOPSKRIVNINGINDBETALING` - Write down (cancelled write-up payment)

## Notes
- Rules are extracted from Java classes in `dk.rim.is.api.rules` package
- Error codes are defined in `dk.rim.is.api.enums.ErrorCode`
- Some rules require runtime service calls (ClaimantAgreementService) and cannot be fully expressed as static decision tables
- The DMN XML is compatible with DMN 1.3 specification
