package dk.ufst.opendebt.citizen.controller;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.context.MessageSource;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import dk.ufst.opendebt.citizen.client.CitizenDebtClient;
import dk.ufst.opendebt.citizen.config.CitizenLinksProperties;
import dk.ufst.opendebt.citizen.config.CitizenOidcUser;
import dk.ufst.opendebt.citizen.dto.CitizenDebtItem;
import dk.ufst.opendebt.citizen.dto.CitizenDebtSummaryResponse;
import dk.ufst.opendebt.citizen.dto.CitizenEffectiveInterestRate;
import dk.ufst.opendebt.citizen.dto.DebtOverviewPageViewModel;
import dk.ufst.opendebt.citizen.dto.DebtOverviewRowViewModel;
import dk.ufst.opendebt.citizen.exception.DebtOverviewServiceUnavailableException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
public class DebtOverviewController {

  private final CitizenDebtClient citizenDebtClient;
  private final CitizenLinksProperties citizenLinks;
  private final MessageSource messageSource;

  @GetMapping("/min-gaeld")
  public String debtOverview(
      @AuthenticationPrincipal CitizenOidcUser citizenUser,
      @SessionAttribute(name = "person_id", required = false) Object sessionPersonId,
      @RequestParam(name = "page", defaultValue = "0") int page,
      Model model,
      Locale locale) {
    int requestedPage = Math.max(page, 0);
    UUID personId = resolvePersonId(citizenUser, sessionPersonId);

    if (personId == null) {
      log.warn("Authenticated citizen session is missing person_id");
      return "error/identity-not-found";
    }

    try {
      CitizenDebtSummaryResponse summary =
          citizenDebtClient.getDebtSummary(personId, requestedPage);
      model.addAttribute("pageView", toPageViewModel(summary, locale));
    } catch (DebtOverviewServiceUnavailableException exception) {
      log.warn("Debt overview unavailable: {}", exception.getMessage());
      model.addAttribute("pageView", serviceUnavailableViewModel(locale, requestedPage));
    }

    return "min-gaeld";
  }

  private DebtOverviewPageViewModel toPageViewModel(
      CitizenDebtSummaryResponse summary, Locale locale) {
    List<DebtOverviewRowViewModel> rows =
        summary.debts().stream().map(debt -> toRowViewModel(debt, locale)).toList();
    int currentPage = Math.max(summary.pageNumber(), 0);
    int totalPages = Math.max(summary.totalPages(), 0);

    return DebtOverviewPageViewModel.builder()
        .totalOutstandingAmount(formatCurrency(summary.totalOutstandingAmount(), locale))
        .totalDebtCount(summary.totalDebtCount())
        .debts(rows)
        .effectiveInterestRateNotes(
            summary.effectiveInterestRates().stream()
                .map(rate -> toInterestRateNote(rate, locale))
                .toList())
        .noDebt(rows.isEmpty())
        .serviceUnavailable(false)
        .paymentInfoUrl(citizenLinks.getPaymentInfo())
        .debtPdfUrl(citizenLinks.getDebtPdf())
        .phoneNumber(citizenLinks.getPhoneNumber())
        .phoneInternational(citizenLinks.getPhoneInternational())
        .landingPageUrl(buildLandingPageUrl())
        .currentPage(currentPage)
        .totalPages(totalPages)
        .hasPrevious(currentPage > 0)
        .hasNext(totalPages > currentPage + 1)
        .previousPage(currentPage > 0 ? currentPage - 1 : null)
        .nextPage(totalPages > currentPage + 1 ? currentPage + 1 : null)
        .build();
  }

  private DebtOverviewPageViewModel serviceUnavailableViewModel(Locale locale, int requestedPage) {
    return DebtOverviewPageViewModel.builder()
        .totalOutstandingAmount(formatCurrency(BigDecimal.ZERO, locale))
        .totalDebtCount(0)
        .debts(List.of())
        .effectiveInterestRateNotes(List.of())
        .noDebt(false)
        .serviceUnavailable(true)
        .paymentInfoUrl(citizenLinks.getPaymentInfo())
        .debtPdfUrl(citizenLinks.getDebtPdf())
        .phoneNumber(citizenLinks.getPhoneNumber())
        .phoneInternational(citizenLinks.getPhoneInternational())
        .landingPageUrl(buildLandingPageUrl())
        .currentPage(requestedPage)
        .totalPages(0)
        .hasPrevious(false)
        .hasNext(false)
        .previousPage(null)
        .nextPage(null)
        .build();
  }

  private DebtOverviewRowViewModel toRowViewModel(CitizenDebtItem debt, Locale locale) {
    return DebtOverviewRowViewModel.builder()
        .debtId(debt.debtId())
        .debtTypeName(firstNonBlank(debt.debtTypeName(), humanizeCode(debt.debtTypeCode())))
        .creditorDisplayName(firstNonBlank(debt.creditorDisplayName(), "—"))
        .principalAmount(formatCurrency(debt.principalAmount(), locale))
        .outstandingAmount(formatCurrency(debt.outstandingAmount(), locale))
        .interestAmount(formatCurrency(debt.interestAmount(), locale))
        .dueDate(formatDate(debt.dueDate(), locale))
        .statusLabel(localizeStatus(debt.citizenStatus(), locale))
        .statusDetail(buildStatusDetail(debt, locale))
        .interestDetail(buildInterestDetail(debt, locale))
        .build();
  }

  private String buildStatusDetail(CitizenDebtItem debt, Locale locale) {
    if (hasText(debt.writtenOffReasonCode())) {
      return message(
          "debt.overview.writeoff." + debt.writtenOffReasonCode(),
          humanizeCode(debt.writtenOffReasonCode()),
          locale);
    }
    if (hasText(debt.statusReasonCode())) {
      return message(
          "debt.overview.status.reason." + debt.statusReasonCode(),
          humanizeCode(debt.statusReasonCode()),
          locale);
    }
    return null;
  }

  private String buildInterestDetail(CitizenDebtItem debt, Locale locale) {
    if ("PAUSED".equalsIgnoreCase(debt.interestAccrualState())
        && hasText(debt.interestPauseReasonCode())) {
      return message(
          "debt.overview.interest.pause." + debt.interestPauseReasonCode(),
          humanizeCode(debt.interestPauseReasonCode()),
          locale);
    }
    if (debt.currentInterestRate() != null) {
      return message(
          "debt.overview.interest.current-rate.row",
          new Object[] {formatPercent(debt.currentInterestRate(), locale)},
          locale);
    }
    return null;
  }

  private String toInterestRateNote(CitizenEffectiveInterestRate rate, Locale locale) {
    return message(
        "debt.overview.interest.current-rate",
        new Object[] {
          humanizeCode(rate.interestRuleCode()),
          formatDate(rate.validFrom(), locale),
          formatPercent(rate.annualRate(), locale)
        },
        locale);
  }

  private String localizeStatus(String citizenStatus, Locale locale) {
    return message("debt.overview.status." + citizenStatus, humanizeCode(citizenStatus), locale);
  }

  private String message(String key, Locale locale) {
    return message(key, key, locale);
  }

  private String message(String key, String fallback, Locale locale) {
    return messageSource.getMessage(key, null, fallback, locale);
  }

  private String message(String key, Object[] arguments, Locale locale) {
    return messageSource.getMessage(key, arguments, key, locale);
  }

  private String formatCurrency(BigDecimal amount, Locale locale) {
    NumberFormat numberFormat = NumberFormat.getCurrencyInstance(locale);
    numberFormat.setCurrency(java.util.Currency.getInstance("DKK"));
    return numberFormat.format(amount == null ? BigDecimal.ZERO : amount);
  }

  private String formatPercent(BigDecimal rate, Locale locale) {
    NumberFormat numberFormat = NumberFormat.getNumberInstance(locale);
    numberFormat.setMinimumFractionDigits(2);
    numberFormat.setMaximumFractionDigits(2);
    return numberFormat.format(rate == null ? BigDecimal.ZERO : rate) + "%";
  }

  private String formatDate(LocalDate date, Locale locale) {
    if (date == null) {
      return "—";
    }
    return DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).localizedBy(locale).format(date);
  }

  private String buildLandingPageUrl() {
    try {
      return ServletUriComponentsBuilder.fromCurrentContextPath().path("/").toUriString();
    } catch (IllegalStateException ex) {
      return "/";
    }
  }

  private UUID resolvePersonId(CitizenOidcUser citizenUser, Object sessionPersonId) {
    if (citizenUser != null && citizenUser.getPersonId() != null) {
      return citizenUser.getPersonId();
    }
    if (sessionPersonId instanceof UUID personId) {
      return personId;
    }
    if (sessionPersonId instanceof String personIdText && hasText(personIdText)) {
      try {
        return UUID.fromString(personIdText);
      } catch (IllegalArgumentException ex) {
        log.warn("Unable to parse session person_id as UUID");
      }
    }
    return null;
  }

  private String firstNonBlank(String preferred, String fallback) {
    return hasText(preferred) ? preferred : fallback;
  }

  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

  private String humanizeCode(String code) {
    if (!hasText(code)) {
      return "—";
    }
    return List.of(code.split("_")).stream()
        .map(String::toLowerCase)
        .map(value -> Character.toUpperCase(value.charAt(0)) + value.substring(1))
        .reduce((left, right) -> left + " " + right)
        .orElse(code);
  }
}
