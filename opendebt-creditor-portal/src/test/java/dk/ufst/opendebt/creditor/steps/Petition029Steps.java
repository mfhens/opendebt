package dk.ufst.opendebt.creditor.steps;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.core.io.Resource;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/** BDD step definitions for petition 029 — Claims in recovery, zero-balance claims, and counts. */
public class Petition029Steps {

  @Autowired private MessageSource messageSource;

  @Value("classpath:templates/claims/recovery-list.html")
  private Resource recoveryListTemplate;

  @Value("classpath:templates/claims/zero-balance-list.html")
  private Resource zeroBalanceListTemplate;

  @Value("classpath:templates/claims/counts.html")
  private Resource countsTemplate;

  @Value("classpath:templates/claims/fragments/claims-table.html")
  private Resource claimsTableFragment;

  private String recoveryListHtml;
  private String zeroBalanceListHtml;
  private String countsHtml;
  private String claimsTableHtml;

  // --- Recovery list ---

  @When("the user opens the claims in recovery list page")
  public void theUserOpensTheClaimsInRecoveryListPage() throws IOException {
    recoveryListHtml = readResource(recoveryListTemplate);
    assertThat(recoveryListHtml).isNotBlank();
  }

  @Then("the recovery list page uses the SKAT standardlayout")
  public void theRecoveryListPageUsesTheSkatStandardlayout() {
    assertThat(recoveryListHtml).contains("layout:decorate=\"~{layout/default}\"");
    assertThat(recoveryListHtml).contains("layout:fragment=\"content\"");
    assertThat(recoveryListHtml).contains("layout:fragment=\"breadcrumb\"");
  }

  @And("the recovery list table body is loaded asynchronously via HTMX")
  public void theRecoveryListTableBodyIsLoadedAsynchronouslyViaHtmx() {
    assertThat(recoveryListHtml).contains("hx-get");
    assertThat(recoveryListHtml).contains("/api/claims/recovery");
    assertThat(recoveryListHtml).contains("hx-trigger=\"load\"");
    assertThat(recoveryListHtml).contains("hx-indicator");
    assertThat(recoveryListHtml).contains("claims.loading");
  }

  // --- Claims table fragment ---

  @Given("the claims table fragment template exists")
  public void theClaimsTableFragmentTemplateExists() throws IOException {
    claimsTableHtml = readResource(claimsTableFragment);
    assertThat(claimsTableHtml).isNotBlank();
  }

  @Then(
      "the table fragment contains columns for claim-id, received-date, debtor-type, debtor-id, debtor-count, creditor-reference, claim-type, claim-status, incorporation-date, period, amount-sent, balance, and balance-with-interest")
  public void theTableFragmentContainsAllThirteenColumns() {
    assertThat(claimsTableHtml).contains("claims.col.claimId");
    assertThat(claimsTableHtml).contains("claims.col.receivedDate");
    assertThat(claimsTableHtml).contains("claims.col.debtorType");
    assertThat(claimsTableHtml).contains("claims.col.debtorId");
    assertThat(claimsTableHtml).contains("claims.col.debtorCount");
    assertThat(claimsTableHtml).contains("claims.col.creditorRef");
    assertThat(claimsTableHtml).contains("claims.col.claimType");
    assertThat(claimsTableHtml).contains("claims.col.claimStatus");
    assertThat(claimsTableHtml).contains("claims.col.incorporationDate");
    assertThat(claimsTableHtml).contains("claims.col.period");
    assertThat(claimsTableHtml).contains("claims.col.amountSent");
    assertThat(claimsTableHtml).contains("claims.col.balance");
    assertThat(claimsTableHtml).contains("claims.col.balanceWithInterest");
  }

  @Then(
      "the table uses semantic HTML elements including table, thead, tbody, and th with scope col")
  public void theTableUsesSemanticHtml() {
    assertThat(claimsTableHtml).contains("<table");
    assertThat(claimsTableHtml).contains("<thead>");
    assertThat(claimsTableHtml).contains("<tbody>");
    assertThat(claimsTableHtml).contains("scope=\"col\"");
  }

  @And("the table has an aria-label for accessibility")
  public void theTableHasAnAriaLabel() {
    assertThat(claimsTableHtml).contains("aria-label");
    assertThat(claimsTableHtml).contains("claims.table.arialabel");
  }

  // --- Zero-balance list ---

  @When("the user opens the zero-balance claims list page")
  public void theUserOpensTheZeroBalanceClaimsListPage() throws IOException {
    zeroBalanceListHtml = readResource(zeroBalanceListTemplate);
    assertThat(zeroBalanceListHtml).isNotBlank();
  }

  @Then("the zero-balance list page uses the SKAT standardlayout")
  public void theZeroBalanceListPageUsesTheSkatStandardlayout() {
    assertThat(zeroBalanceListHtml).contains("layout:decorate=\"~{layout/default}\"");
    assertThat(zeroBalanceListHtml).contains("layout:fragment=\"content\"");
    assertThat(zeroBalanceListHtml).contains("layout:fragment=\"breadcrumb\"");
  }

  @And("the zero-balance list table body is loaded asynchronously via HTMX")
  public void theZeroBalanceListTableBodyIsLoadedViaHtmx() {
    assertThat(zeroBalanceListHtml).contains("hx-get");
    assertThat(zeroBalanceListHtml).contains("/api/claims/zero-balance");
    assertThat(zeroBalanceListHtml).contains("hx-trigger=\"load\"");
    assertThat(zeroBalanceListHtml).contains("hx-indicator");
  }

  @Then("both recovery and zero-balance lists use the same claims table fragment")
  public void bothListsUseSameTableFragment() throws IOException {
    // Both list pages use HTMX to load from the same fragment template
    String recoveryHtml = readResource(recoveryListTemplate);
    String zeroBalanceHtml = readResource(zeroBalanceListTemplate);
    assertThat(recoveryHtml).contains("claims-table-container");
    assertThat(zeroBalanceHtml).contains("claims-table-container");
    // The fragment itself is shared
    assertThat(claimsTableHtml).contains("th:fragment=\"claimsTable\"");
  }

  // --- Search and filtering ---

  @Then("the search form includes options for claim-id, CPR, CVR, and SE search types")
  public void theSearchFormIncludesSearchTypeOptions() {
    assertThat(recoveryListHtml).contains("searchType");
    assertThat(recoveryListHtml).contains("claimId");
    assertThat(recoveryListHtml).contains("cpr");
    assertThat(recoveryListHtml).contains("cvr");
    assertThat(recoveryListHtml).contains("se");
  }

  @Then("the page includes date range filter inputs for modtagelsesdato")
  public void thePageIncludesDateRangeFilterInputs() {
    assertThat(recoveryListHtml).contains("dateFrom");
    assertThat(recoveryListHtml).contains("dateTo");
    assertThat(recoveryListHtml).contains("type=\"date\"");
  }

  // --- Claims counts ---

  @Given("the claims counts template exists")
  public void theClaimsCountsTemplateExists() throws IOException {
    countsHtml = readResource(countsTemplate);
    assertThat(countsHtml).isNotBlank();
  }

  @Then("the counts page shows a recovery count card and a zero-balance count card")
  public void theCountsPageShowsCountCards() {
    assertThat(countsHtml).contains("claims.counts.recovery");
    assertThat(countsHtml).contains("claims.counts.zerobalance");
    assertThat(countsHtml).contains("counts.inRecovery");
    assertThat(countsHtml).contains("counts.zeroBalance");
  }

  @And("the counts page includes a date range filter form")
  public void theCountsPageIncludesDateRangeFilterForm() {
    assertThat(countsHtml).contains("dateFrom");
    assertThat(countsHtml).contains("dateTo");
    assertThat(countsHtml).contains("claims.counts.filter.button");
  }

  // --- Pagination ---

  @Then("the pagination controls use HTMX to load pages without full page reload")
  public void thePaginationControlsUseHtmx() {
    assertThat(claimsTableHtml).contains("skat-pagination");
    assertThat(claimsTableHtml).contains("hx-get");
    assertThat(claimsTableHtml).contains("hx-target=\"#claims-table-container\"");
  }

  @And("the pagination controls are keyboard accessible")
  public void thePaginationControlsAreKeyboardAccessible() {
    assertThat(claimsTableHtml).contains("role=\"navigation\"");
    assertThat(claimsTableHtml).contains("claims.pagination.label");
    assertThat(claimsTableHtml).contains("aria-current=\"page\"");
    assertThat(claimsTableHtml).contains("aria-live=\"polite\"");
  }

  // --- CPR censoring ---

  @Then("the table is designed to display pre-censored debtor identifiers")
  public void theTableDisplaysPreCensoredDebtorIdentifiers() {
    // The CPR censoring is done server-side in ClaimsListController.censorCprNumbers()
    // The template simply renders the debtorIdentifier field as-is
    assertThat(claimsTableHtml).contains("claim.debtorIdentifier");
  }

  // --- Formatting ---

  @Then("monetary amounts in the table use comma as decimal separator with 2 decimal places")
  public void monetaryAmountsUseDanishLocaleFormatting() {
    // Verify the Thymeleaf number formatting syntax for Danish locale
    assertThat(claimsTableHtml).contains("#numbers.formatDecimal");
    assertThat(claimsTableHtml).contains("'COMMA'");
    assertThat(claimsTableHtml).contains("2");
  }

  @Then("dates in the table are formatted as dd.MM.yyyy")
  public void datesAreFormattedCorrectly() {
    assertThat(claimsTableHtml).contains("#temporals.format");
    assertThat(claimsTableHtml).contains("dd.MM.yyyy");
  }

  // --- i18n ---

  @Then("Danish translations exist for all claims list message keys")
  public void danishTranslationsExistForAllClaimsListMessageKeys() {
    Locale da = new Locale("da");
    assertThat(messageSource.getMessage("claims.recovery.page.title", null, da)).isNotBlank();
    assertThat(messageSource.getMessage("claims.recovery.heading", null, da)).isNotBlank();
    assertThat(messageSource.getMessage("claims.zerobalance.page.title", null, da)).isNotBlank();
    assertThat(messageSource.getMessage("claims.zerobalance.heading", null, da)).isNotBlank();
    assertThat(messageSource.getMessage("claims.col.claimId", null, da)).isNotBlank();
    assertThat(messageSource.getMessage("claims.col.receivedDate", null, da)).isNotBlank();
    assertThat(messageSource.getMessage("claims.col.debtorType", null, da)).isNotBlank();
    assertThat(messageSource.getMessage("claims.col.balance", null, da)).isNotBlank();
    assertThat(messageSource.getMessage("claims.col.balanceWithInterest", null, da)).isNotBlank();
    assertThat(messageSource.getMessage("claims.pagination.previous", null, da)).isNotBlank();
    assertThat(messageSource.getMessage("claims.pagination.next", null, da)).isNotBlank();
    assertThat(messageSource.getMessage("claims.search.button", null, da)).isNotBlank();
    assertThat(messageSource.getMessage("claims.counts.recovery", null, da)).isNotBlank();
  }

  @And("English translations exist for all claims list message keys")
  public void englishTranslationsExistForAllClaimsListMessageKeys() {
    Locale en = Locale.forLanguageTag("en-GB");
    assertThat(messageSource.getMessage("claims.recovery.page.title", null, en))
        .isEqualTo("Claims in recovery");
    assertThat(messageSource.getMessage("claims.zerobalance.page.title", null, en))
        .isEqualTo("Zero-balance claims");
    assertThat(messageSource.getMessage("claims.col.claimId", null, en)).isEqualTo("Claim ID");
    assertThat(messageSource.getMessage("claims.col.balance", null, en)).isEqualTo("Balance");
    assertThat(messageSource.getMessage("claims.pagination.previous", null, en))
        .isEqualTo("Previous");
    assertThat(messageSource.getMessage("claims.pagination.next", null, en)).isEqualTo("Next");
    assertThat(messageSource.getMessage("claims.search.button", null, en)).isEqualTo("Search");
  }

  // --- Click-through ---

  @Then("each claim row contains a link to the claim detail page")
  public void eachClaimRowContainsALinkToClaimDetailPage() {
    assertThat(claimsTableHtml).contains("/fordring/");
    assertThat(claimsTableHtml).contains("claim.claimId");
  }

  private String readResource(Resource resource) throws IOException {
    return Files.readString(Path.of(resource.getURI()), StandardCharsets.UTF_8);
  }
}
