# Petition 021 Outcome Contract

## Acceptance criteria

1. All user-facing text in every OpenDebt portal is externalized to message bundle files, not hardcoded in templates.
2. Danish (da-DK) is the default and fallback locale across all portals.
3. English (en-GB) translations exist for all message keys in every portal.
4. A language selector drop-down is visible in the header of every portal, listing all configured languages by their native name.
5. Switching language via the drop-down persists the choice for the session and re-renders the page in the selected language.
6. The HTML `lang` attribute reflects the active locale on every page.
7. Date, number, and currency formatting respects the active locale.
8. Adding a new supported language requires only a configuration change, a new message properties file, and redeployment -- no code changes.
9. Missing translations for a non-default locale fall back to Danish rather than showing raw message keys.
10. All accessibility labels (aria-label, aria-describedby, skip-link text) are internationalized.
11. The Fordringshaverportal has zero hardcoded Danish text remaining in its Thymeleaf templates (all replaced with `#{...}` message expressions).
12. A `translator-droid` exists and can be used to generate message bundles for new locales.

## Definition of done

- Every portal template uses `#{...}` message expressions exclusively for user-visible text.
- `messages_da.properties` and `messages_en_GB.properties` exist for each portal with complete key coverage.
- The language selector is rendered from the `opendebt.i18n.supported-locales` configuration.
- Switching to English renders all text in English; switching back to Danish renders all text in Danish.
- The `translator-droid` is registered in `.factory/droids/` and documented.
- Every acceptance criterion is covered by at least one Gherkin scenario.

## Failure conditions

- Any portal page displays hardcoded text that is not in a message bundle.
- Switching language does not persist across page navigation within the session.
- The language selector is not visible or not functional.
- Adding a new language requires Java code changes.
- Missing translations display raw message keys to the user.
- Accessibility labels remain hardcoded and do not change with language.
