# Petition 021: Internationalization (i18n) infrastructure for all OpenDebt portals

## Summary

All OpenDebt user-facing portals shall support internationalization (i18n) with Danish (da-DK) as the primary language and a configurable set of additional languages identified by ISO 639-1 / BCP 47 codes. English (en-GB) shall be the first additional language. The language selection shall be available as a drop-down in the portal header, and the active language shall persist across the user session.

## Context and motivation

Gældsstyrelsen's public website (gaeldst.dk) already offers both Danish and English. OpenDebt serves citizens and creditors who may not read Danish fluently. International creditors, foreign debtors with Danish public debt, and EU cross-border collection scenarios all require English as a minimum second language.

The Fordringshaverportal (creditor portal) is already implemented with all user-facing text hardcoded in Danish. This petition requires extracting those strings into message bundles and providing English translations. The Skyldnerportal (citizen portal) and any future portals shall be built with i18n from the start.

Internationalization is a cross-cutting quality requirement that applies to every portal in the system.

## Functional requirements

1. All user-facing text in OpenDebt portals shall be externalized into locale-specific message bundles (e.g. `messages_da.properties`, `messages_en_GB.properties`).
2. Danish (da-DK) shall be the default and fallback language for all portals.
3. English (en-GB) shall be provided as the first additional supported language.
4. The set of supported languages shall be configurable via application configuration using ISO 639-1 language codes (with optional BCP 47 region subtags).
5. Each portal shall display a language selector (drop-down) in the header area, allowing the user to switch language at any time.
6. The selected language shall persist for the duration of the user session (cookie or session-based).
7. The language selector shall display the language name in its own language (e.g. "Dansk", "English").
8. All existing Fordringshaverportal templates shall be refactored to use message keys (`#{...}`) instead of hardcoded Danish text.
9. All existing Fordringshaverportal hardcoded Danish strings shall be extracted to `messages_da.properties` and translated to `messages_en_GB.properties`.
10. Date, number, and currency formatting shall respect the active locale.
11. The `lang` attribute on the HTML element shall reflect the active locale.
12. Accessibility labels (aria-label, aria-describedby) shall also be internationalized.

## Non-functional requirements

1. Adding a new language shall require only: (a) adding the ISO code to the configuration, (b) providing a `messages_<locale>.properties` file, and (c) redeploying the portal.
2. When a new language is added to the configuration or new pages are created, the `translator-droid` shall be used to generate translations for the new locale or translate new content into all supported locales.
3. Missing message keys for a non-default locale shall fall back to Danish (da-DK) rather than displaying raw key names.
4. The i18n infrastructure shall be implemented as a shared pattern in `opendebt-common` or as a shared Thymeleaf layout fragment so that all portals reuse the same mechanism.

## Technical approach

- Spring MessageSource with `ReloadableResourceBundleMessageSource` for message bundles.
- `LocaleChangeInterceptor` with a `lang` request parameter for language switching.
- `CookieLocaleResolver` or `SessionLocaleResolver` for persistence.
- Thymeleaf `#{...}` expressions for all user-facing text.
- Configuration property: `opendebt.i18n.supported-locales: da-DK,en-GB` (extendable).
- Language drop-down rendered from the supported-locales list, positioned in the layout header.

## Configuration example

```yaml
opendebt:
  i18n:
    default-locale: da-DK
    supported-locales:
      - da-DK
      - en-GB
```

## Constraints and assumptions

- This petition covers portals using Thymeleaf (ADR-0023). Backend API error messages and validation codes are out of scope (they use error codes, not human-readable text).
- Right-to-left (RTL) language support is out of scope for this petition.
- The translator-droid is a Factory custom droid that automates translation of message bundles.
- Content accuracy for legal/official text in translations should be reviewed by a domain expert before production use.

## Out of scope

- Backend REST API response message localization (error codes are language-neutral).
- RTL layout support.
- Machine translation quality assurance (human review is assumed for production).
- Content management system for translations.

## Affected portals

| Portal | Current state | Required work |
|--------|--------------|---------------|
| Fordringshaverportal (creditor-portal) | All text hardcoded in Danish, no i18n | Extract strings, create DA + EN-GB bundles, add language selector |
| Skyldnerportal (citizen-portal) | Skeleton, no UI | Build with i18n from the start |
| Future portals | N/A | Must use i18n infrastructure from day one |
