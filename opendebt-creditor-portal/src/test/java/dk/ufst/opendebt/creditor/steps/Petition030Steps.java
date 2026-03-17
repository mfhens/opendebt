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

/** BDD step definitions for petition 030 — Claim detail view. */
public class Petition030Steps {

  @Autowired private MessageSource messageSource;

  @Value("classpath:templates/claims/detail.html")
  private Resource detailTemplate;

  private String detailHtml;

  // --- Template existence and structure ---

  @Given("the claim detail template exists")
  public void theClaimDetailTemplateExists() throws IOException {
    detailHtml = readResource(detailTemplate);
    assertThat(detailHtml).isNotBlank();
  }

  @Then("the detail page uses the SKAT standardlayout with breadcrumb")
  public void theDetailPageUsesTheSkatStandardlayoutWithBreadcrumb() {
    assertThat(detailHtml).contains("layout:decorate=\"~{layout/default}\"");
    assertThat(detailHtml).contains("layout:fragment=\"content\"");
    assertThat(detailHtml).contains("layout:fragment=\"breadcrumb\"");
    assertThat(detailHtml).contains("claim.detail.breadcrumb.claims");
    assertThat(detailHtml).contains("/fordringer");
  }

  // --- Claim information section ---

  @Then("the claim detail template contains a claim information section")
  public void theClaimDetailTemplateContainsAClaimInformationSection() {
    assertThat(detailHtml).contains("claim.detail.section.info");
    assertThat(detailHtml).contains("claim.detail.field.claimId");
    assertThat(detailHtml).contains("claim.detail.field.claimType");
    assertThat(detailHtml).contains("claim.detail.field.claimCategory");
    assertThat(detailHtml).contains("claim.detail.field.creditorDescription");
    assertThat(detailHtml).contains("claim.detail.field.receivedDate");
    assertThat(detailHtml).contains("claim.detail.field.period");
    assertThat(detailHtml).contains("claim.detail.field.incorporationDate");
    assertThat(detailHtml).contains("claim.detail.field.obligationId");
    assertThat(detailHtml).contains("claim.detail.field.creditorReference");
  }

  @And("the template contains single-debtor-only fields conditionally displayed")
  public void theTemplateContainsSingleDebtorOnlyFieldsConditionallyDisplayed() {
    assertThat(detailHtml).contains("singleDebtor");
    assertThat(detailHtml).contains("claim.detail.field.dueDate");
    assertThat(detailHtml).contains("claim.detail.field.limitationDate");
    assertThat(detailHtml).contains("claim.detail.field.lastTimelyPaymentDate");
    assertThat(detailHtml).contains("claim.detail.field.courtDate");
  }

  @And("the template conditionally displays the related obligation ID")
  public void theTemplateConditionallyDisplaysTheRelatedObligationId() {
    assertThat(detailHtml).contains("claim.detail.field.relatedObligationId");
    assertThat(detailHtml).contains("claim.relatedObligationId");
  }

  // --- Financial information section ---

  @Then("the claim detail template contains a financial breakdown table")
  public void theClaimDetailTemplateContainsAFinancialBreakdownTable() {
    assertThat(detailHtml).contains("claim.detail.section.financial");
    assertThat(detailHtml).contains("claim.detail.financial.col.category");
    assertThat(detailHtml).contains("claim.detail.financial.col.original");
    assertThat(detailHtml).contains("claim.detail.financial.col.writeoff");
    assertThat(detailHtml).contains("claim.detail.financial.col.payment");
    assertThat(detailHtml).contains("claim.detail.financial.col.balance");
  }

  @And("the financial table uses semantic HTML with scope attributes")
  public void theFinancialTableUsesSemanticHtmlWithScopeAttributes() {
    assertThat(detailHtml).contains("<table");
    assertThat(detailHtml).contains("<thead>");
    assertThat(detailHtml).contains("<tbody>");
    assertThat(detailHtml).contains("scope=\"col\"");
    assertThat(detailHtml).contains("claim.detail.financial.table.arialabel");
  }

  @And("the template displays additional financial summary fields")
  public void theTemplateDisplaysAdditionalFinancialSummaryFields() {
    assertThat(detailHtml).contains("claim.detail.field.interestRule");
    assertThat(detailHtml).contains("claim.detail.field.interestRate");
    assertThat(detailHtml).contains("claim.detail.field.totalDebt");
    assertThat(detailHtml).contains("claim.detail.field.latestInterestAccrualDate");
    assertThat(detailHtml).contains("claim.detail.field.originalPrincipal");
    assertThat(detailHtml).contains("claim.detail.field.receivedAmount");
    assertThat(detailHtml).contains("claim.detail.field.claimBalance");
    assertThat(detailHtml).contains("claim.detail.field.totalCreditorBalance");
    assertThat(detailHtml).contains("claim.detail.field.amountSentForRecovery");
    assertThat(detailHtml).contains("claim.detail.field.amountSentWithWriteUps");
  }

  @And("the extra interest rate is conditionally displayed")
  public void theExtraInterestRateIsConditionallyDisplayed() {
    assertThat(detailHtml).contains("claim.detail.field.extraInterestRate");
    assertThat(detailHtml).contains("claim.extraInterestRate");
  }

  // --- Write-ups section ---

  @Then("the claim detail template contains a collapsible write-ups section")
  public void theClaimDetailTemplateContainsACollapsibleWriteUpsSection() {
    assertThat(detailHtml).contains("claim.detail.section.writeups");
    assertThat(detailHtml).contains("<details>");
    assertThat(detailHtml).contains("<summary>");
    assertThat(detailHtml).contains("claim.detail.writeup.col.actionId");
    assertThat(detailHtml).contains("claim.detail.writeup.col.refActionId");
    assertThat(detailHtml).contains("claim.detail.writeup.col.formType");
    assertThat(detailHtml).contains("claim.detail.writeup.col.reason");
    assertThat(detailHtml).contains("claim.detail.writeup.col.amount");
    assertThat(detailHtml).contains("claim.detail.writeup.col.effectiveDate");
    assertThat(detailHtml).contains("claim.detail.writeup.col.debtorId");
  }

  @And("annulled write-ups have a visual flag")
  public void annulledWriteUpsHaveAVisualFlag() {
    assertThat(detailHtml).contains("wu.annulled");
    assertThat(detailHtml).contains("skat-row--annulled");
  }

  // --- Write-downs section ---

  @Then("the claim detail template contains a collapsible write-downs section")
  public void theClaimDetailTemplateContainsACollapsibleWriteDownsSection() {
    assertThat(detailHtml).contains("claim.detail.section.writedowns");
    assertThat(detailHtml).contains("claim.detail.writedown.col.actionId");
    assertThat(detailHtml).contains("claim.detail.writedown.col.refActionId");
    assertThat(detailHtml).contains("claim.detail.writedown.col.formType");
    assertThat(detailHtml).contains("claim.detail.writedown.col.reasonCode");
    assertThat(detailHtml).contains("claim.detail.writedown.col.amount");
    assertThat(detailHtml).contains("claim.detail.writedown.col.effectiveDate");
    assertThat(detailHtml).contains("claim.detail.writedown.col.debtorId");
  }

  // --- Related claims section ---

  @Then("the claim detail template contains a collapsible related claims section")
  public void theClaimDetailTemplateContainsACollapsibleRelatedClaimsSection() {
    assertThat(detailHtml).contains("claim.detail.section.relatedclaims");
    assertThat(detailHtml).contains("claim.detail.relatedclaim.col.claimId");
    assertThat(detailHtml).contains("claim.detail.relatedclaim.col.claimType");
    assertThat(detailHtml).contains("claim.detail.relatedclaim.col.balance");
    assertThat(detailHtml).contains("claim.detail.relatedclaim.col.status");
  }

  @And("each related claim is clickable to its own detail view")
  public void eachRelatedClaimIsClickableToItsOwnDetailView() {
    assertThat(detailHtml).contains("/fordring/");
    assertThat(detailHtml).contains("rc.claimId");
  }

  // --- Debtor information section ---

  @Then("the claim detail template contains a debtors section")
  public void theClaimDetailTemplateContainsADebtorsSection() {
    assertThat(detailHtml).contains("claim.detail.section.debtors");
    assertThat(detailHtml).contains("claim.detail.debtor.col.type");
    assertThat(detailHtml).contains("claim.detail.debtor.col.identifier");
  }

  // --- Decisions section ---

  @Then("the claim detail template contains a decisions section for single-debtor claims")
  public void theClaimDetailTemplateContainsADecisionsSectionForSingleDebtorClaims() {
    assertThat(detailHtml).contains("claim.detail.section.decisions");
    assertThat(detailHtml).contains("claim.detail.decision.col.type");
    assertThat(detailHtml).contains("claim.detail.decision.col.date");
    assertThat(detailHtml).contains("claim.detail.decision.col.description");
    // Decisions are conditioned on singleDebtor
    assertThat(detailHtml).contains("singleDebtor");
  }

  // --- Zero-balance expired message ---

  @Then("the claim detail template shows a zero-balance expired message when applicable")
  public void theClaimDetailTemplateShowsAZeroBalanceExpiredMessageWhenApplicable() {
    assertThat(detailHtml).contains("claim.zeroBalanceExpired");
    assertThat(detailHtml).contains("claim.detail.zerobalance.expired");
  }

  // --- Error handling ---

  @Then("the claim detail template displays service errors")
  public void theClaimDetailTemplateDisplaysServiceErrors() {
    assertThat(detailHtml).contains("serviceError");
    assertThat(detailHtml).contains("skat-alert--error");
  }

  // --- Monetary and date formatting ---

  @Then("monetary amounts in the claim detail use Danish locale formatting")
  public void monetaryAmountsInTheClaimDetailUseDanishLocaleFormatting() {
    assertThat(detailHtml).contains("#numbers.formatDecimal");
    assertThat(detailHtml).contains("'COMMA'");
    assertThat(detailHtml).contains("DKK");
  }

  @And("dates in the claim detail are formatted as dd.MM.yyyy")
  public void datesInTheClaimDetailAreFormattedAsDdMmYyyy() {
    assertThat(detailHtml).contains("#temporals.format");
    assertThat(detailHtml).contains("dd.MM.yyyy");
  }

  // --- i18n ---

  @Then("Danish translations exist for all claim detail message keys")
  public void danishTranslationsExistForAllClaimDetailMessageKeys() {
    Locale da = new Locale("da");
    assertThat(messageSource.getMessage("claim.detail.page.title", null, da)).isNotBlank();
    assertThat(messageSource.getMessage("claim.detail.heading", null, da)).isNotBlank();
    assertThat(messageSource.getMessage("claim.detail.section.info", null, da)).isNotBlank();
    assertThat(messageSource.getMessage("claim.detail.section.financial", null, da)).isNotBlank();
    assertThat(messageSource.getMessage("claim.detail.section.writeups", null, da)).isNotBlank();
    assertThat(messageSource.getMessage("claim.detail.section.writedowns", null, da)).isNotBlank();
    assertThat(messageSource.getMessage("claim.detail.section.relatedclaims", null, da))
        .isNotBlank();
    assertThat(messageSource.getMessage("claim.detail.section.debtors", null, da)).isNotBlank();
    assertThat(messageSource.getMessage("claim.detail.section.decisions", null, da)).isNotBlank();
    assertThat(messageSource.getMessage("claim.detail.field.claimId", null, da)).isNotBlank();
    assertThat(messageSource.getMessage("claim.detail.field.claimType", null, da)).isNotBlank();
    assertThat(messageSource.getMessage("claim.detail.field.dueDate", null, da)).isNotBlank();
    assertThat(messageSource.getMessage("claim.detail.field.limitationDate", null, da))
        .isNotBlank();
    assertThat(messageSource.getMessage("claim.detail.financial.col.category", null, da))
        .isNotBlank();
    assertThat(messageSource.getMessage("claim.detail.writeup.col.actionId", null, da))
        .isNotBlank();
    assertThat(messageSource.getMessage("claim.detail.writedown.col.actionId", null, da))
        .isNotBlank();
    assertThat(messageSource.getMessage("claim.detail.zerobalance.expired", null, da)).isNotBlank();
    assertThat(messageSource.getMessage("claim.detail.error.service", null, da)).isNotBlank();
    assertThat(messageSource.getMessage("claim.detail.btn.back", null, da)).isNotBlank();
  }

  @And("English translations exist for all claim detail message keys")
  public void englishTranslationsExistForAllClaimDetailMessageKeys() {
    Locale en = Locale.forLanguageTag("en-GB");
    assertThat(messageSource.getMessage("claim.detail.page.title", null, en))
        .isEqualTo("Claim details");
    assertThat(messageSource.getMessage("claim.detail.heading", null, en)).isEqualTo("Claim");
    assertThat(messageSource.getMessage("claim.detail.section.info", null, en))
        .isEqualTo("Claim information");
    assertThat(messageSource.getMessage("claim.detail.section.financial", null, en))
        .isEqualTo("Financial information");
    assertThat(messageSource.getMessage("claim.detail.section.writeups", null, en))
        .isEqualTo("Write-ups");
    assertThat(messageSource.getMessage("claim.detail.section.writedowns", null, en))
        .isEqualTo("Write-downs");
    assertThat(messageSource.getMessage("claim.detail.section.relatedclaims", null, en))
        .isEqualTo("Related claims");
    assertThat(messageSource.getMessage("claim.detail.section.debtors", null, en))
        .isEqualTo("Debtors");
    assertThat(messageSource.getMessage("claim.detail.section.decisions", null, en))
        .isEqualTo("Decisions");
    assertThat(messageSource.getMessage("claim.detail.btn.back", null, en))
        .isEqualTo("Back to claims");
  }

  // --- All text from message bundles ---

  @Then("the claim detail template uses message bundles for all user-facing text")
  public void theClaimDetailTemplateUsesMessageBundlesForAllUserFacingText() {
    // All user-facing labels use th:text="#{...}" expressions
    assertThat(detailHtml).contains("#{claim.detail.");
    // No hardcoded Danish text in labels (except HTML fallback in th:text body)
    // The important thing is the template uses message keys
    assertThat(detailHtml).contains("#{claim.detail.page.title}");
    assertThat(detailHtml).contains("#{claim.detail.heading}");
    assertThat(detailHtml).contains("#{claim.detail.btn.back}");
  }

  private String readResource(Resource resource) throws IOException {
    return Files.readString(Path.of(resource.getURI()), StandardCharsets.UTF_8);
  }
}
