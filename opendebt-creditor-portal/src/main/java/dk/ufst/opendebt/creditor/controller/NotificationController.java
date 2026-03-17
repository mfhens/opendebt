package dk.ufst.opendebt.creditor.controller;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpSession;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import dk.ufst.opendebt.creditor.client.CreditorServiceClient;
import dk.ufst.opendebt.creditor.client.NotificationServiceClient;
import dk.ufst.opendebt.creditor.dto.CreditorAgreementDto;
import dk.ufst.opendebt.creditor.dto.NotificationSearchDto;
import dk.ufst.opendebt.creditor.dto.NotificationSearchResultDto;
import dk.ufst.opendebt.creditor.dto.NotificationType;
import dk.ufst.opendebt.creditor.service.PortalSessionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller for the notification search and download page (petition 035). Requires CREDITOR_VIEWER
 * or CREDITOR_EDITOR role for access.
 */
@Slf4j
@Controller
@RequestMapping("/underretninger")
@RequiredArgsConstructor
public class NotificationController {

  private final CreditorServiceClient creditorServiceClient;
  private final NotificationServiceClient notificationServiceClient;
  private final MessageSource messageSource;
  private final PortalSessionService portalSessionService;

  /** GET /underretninger — displays the notification search page. */
  @GetMapping
  public String showSearchPage(Model model, HttpSession session) {
    UUID actingCreditor = portalSessionService.resolveActingCreditor(null, session);
    if (actingCreditor == null) {
      return "redirect:/demo-login";
    }

    model.addAttribute("currentPage", "notifications");
    addActingCreditorToModel(model, session);

    List<NotificationType> availableTypes = resolveAvailableTypes(actingCreditor);
    model.addAttribute("availableTypes", availableTypes);

    if (!model.containsAttribute("searchForm")) {
      model.addAttribute("searchForm", new NotificationSearchDto());
    }

    return "notifications/search";
  }

  /**
   * POST /underretninger/search — performs the notification search and returns results fragment.
   */
  @PostMapping("/search")
  public String searchNotifications(
      @ModelAttribute("searchForm") NotificationSearchDto searchForm,
      Model model,
      HttpSession session) {
    UUID actingCreditor = portalSessionService.resolveActingCreditor(null, session);
    if (actingCreditor == null) {
      return "redirect:/demo-login";
    }

    model.addAttribute("currentPage", "notifications");
    addActingCreditorToModel(model, session);

    List<NotificationType> availableTypes = resolveAvailableTypes(actingCreditor);
    model.addAttribute("availableTypes", availableTypes);
    model.addAttribute("searchForm", searchForm);
    model.addAttribute("searchPerformed", true);

    try {
      NotificationSearchResultDto result =
          notificationServiceClient.searchNotifications(
              actingCreditor,
              searchForm.getDateFrom(),
              searchForm.getDateTo(),
              searchForm.getNotificationTypes());

      if (result != null) {
        model.addAttribute("searchResult", result);
      } else {
        model.addAttribute(
            "searchResult", NotificationSearchResultDto.builder().matchingCount(0).build());
        model.addAttribute(
            "backendWarning",
            messageSource.getMessage(
                "notifications.backend.unavailable", null, LocaleContextHolder.getLocale()));
      }
    } catch (Exception ex) {
      log.error("Notification search failed: {}", ex.getMessage());
      model.addAttribute(
          "searchResult", NotificationSearchResultDto.builder().matchingCount(0).build());
      model.addAttribute(
          "backendError",
          messageSource.getMessage(
              "notifications.search.error", null, LocaleContextHolder.getLocale()));
    }

    return "notifications/search";
  }

  /** HTMX endpoint that returns the search results fragment. Used for partial page updates. */
  @PostMapping("/search-fragment")
  public String searchNotificationsFragment(
      @ModelAttribute("searchForm") NotificationSearchDto searchForm,
      Model model,
      HttpSession session) {
    UUID actingCreditor = portalSessionService.resolveActingCreditor(null, session);
    if (actingCreditor == null) {
      model.addAttribute(
          "searchResult", NotificationSearchResultDto.builder().matchingCount(0).build());
      return "notifications/fragments/results :: notificationResults";
    }

    model.addAttribute("searchForm", searchForm);
    model.addAttribute("searchPerformed", true);

    try {
      NotificationSearchResultDto result =
          notificationServiceClient.searchNotifications(
              actingCreditor,
              searchForm.getDateFrom(),
              searchForm.getDateTo(),
              searchForm.getNotificationTypes());

      if (result != null) {
        model.addAttribute("searchResult", result);
      } else {
        model.addAttribute(
            "searchResult", NotificationSearchResultDto.builder().matchingCount(0).build());
        model.addAttribute(
            "backendWarning",
            messageSource.getMessage(
                "notifications.backend.unavailable", null, LocaleContextHolder.getLocale()));
      }
    } catch (Exception ex) {
      log.error("Notification search failed: {}", ex.getMessage());
      model.addAttribute(
          "searchResult", NotificationSearchResultDto.builder().matchingCount(0).build());
      model.addAttribute(
          "backendError",
          messageSource.getMessage(
              "notifications.search.error", null, LocaleContextHolder.getLocale()));
    }

    return "notifications/fragments/results :: notificationResults";
  }

  /** POST /underretninger/download — downloads matching notifications as a zip file. */
  @PostMapping("/download")
  public Object downloadNotifications(
      @ModelAttribute("searchForm") NotificationSearchDto searchForm,
      Model model,
      HttpSession session) {
    UUID actingCreditor = portalSessionService.resolveActingCreditor(null, session);
    if (actingCreditor == null) {
      return "redirect:/demo-login";
    }

    if (!searchForm.isFormatPdf() && !searchForm.isFormatXml()) {
      return handleDownloadError(model, session, searchForm, "notifications.download.noformat");
    }

    try {
      byte[] zipBytes =
          notificationServiceClient.downloadNotifications(
              actingCreditor,
              searchForm.getDateFrom(),
              searchForm.getDateTo(),
              searchForm.getNotificationTypes(),
              searchForm.isFormatPdf(),
              searchForm.isFormatXml());

      if (zipBytes != null && zipBytes.length > 0) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDisposition(
            ContentDisposition.attachment().filename("underretninger.zip").build());
        return ResponseEntity.ok().headers(headers).body(zipBytes);
      }

      return handleDownloadError(model, session, searchForm, "notifications.backend.unavailable");
    } catch (Exception ex) {
      log.error("Notification download failed: {}", ex.getMessage());
      return handleDownloadError(model, session, searchForm, "notifications.download.error");
    }
  }

  private String handleDownloadError(
      Model model, HttpSession session, NotificationSearchDto searchForm, String messageKey) {
    model.addAttribute("currentPage", "notifications");
    addActingCreditorToModel(model, session);
    UUID actingCreditor = portalSessionService.resolveActingCreditor(null, session);
    List<NotificationType> availableTypes = resolveAvailableTypes(actingCreditor);
    model.addAttribute("availableTypes", availableTypes);
    model.addAttribute("searchForm", searchForm);
    model.addAttribute("searchPerformed", true);
    model.addAttribute(
        "searchResult", NotificationSearchResultDto.builder().matchingCount(0).build());
    model.addAttribute(
        "downloadError",
        messageSource.getMessage(messageKey, null, LocaleContextHolder.getLocale()));
    return "notifications/search";
  }

  /**
   * Resolves which notification types are available for the given creditor based on their
   * agreement. Falls back to all types if agreement is unavailable or has no type restrictions.
   */
  private List<NotificationType> resolveAvailableTypes(UUID creditorOrgId) {
    List<NotificationType> allTypes = Arrays.asList(NotificationType.values());

    try {
      CreditorAgreementDto agreement = creditorServiceClient.getCreditorAgreement(creditorOrgId);

      if (agreement == null
          || agreement.getEnabledNotificationTypes() == null
          || agreement.getEnabledNotificationTypes().isEmpty()) {
        // No restriction configured — show all types
        return allTypes;
      }

      return allTypes.stream()
          .filter(type -> agreement.getEnabledNotificationTypes().contains(type.name()))
          .collect(Collectors.toList());
    } catch (Exception ex) {
      log.warn("Failed to resolve notification types from agreement: {}", ex.getMessage());
      return allTypes;
    }
  }

  private void addActingCreditorToModel(Model model, HttpSession session) {
    UUID actingCreditor = portalSessionService.resolveActingCreditor(null, session);
    UUID representedCreditor = portalSessionService.getRepresentedCreditor(session);
    model.addAttribute("actingCreditorOrgId", actingCreditor);
    if (representedCreditor != null) {
      model.addAttribute("representedCreditorOrgId", representedCreditor);
    }
  }
}
