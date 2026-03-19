package dk.ufst.opendebt.debtservice.controller;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import dk.ufst.opendebt.debtservice.dto.IssueDemandRequest;
import dk.ufst.opendebt.debtservice.dto.NotificationDto;
import dk.ufst.opendebt.debtservice.service.NotificationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/debts/{debtId}")
@RequiredArgsConstructor
@Tag(
    name = "Notifications",
    description = "Debt notification operations (underretning, paakrav, rykker)")
public class NotificationController {

  private final NotificationService notificationService;

  @PostMapping("/demand-for-payment")
  @PreAuthorize("hasRole('CASEWORKER') or hasRole('ADMIN')")
  @Operation(summary = "Issue demand for payment (paakrav)")
  public ResponseEntity<NotificationDto> issueDemandForPayment(
      @PathVariable UUID debtId, @Valid @RequestBody IssueDemandRequest request) {
    NotificationDto notification =
        notificationService.issueDemandForPayment(debtId, request.getCreditorOrgId());
    return ResponseEntity.status(HttpStatus.CREATED).body(notification);
  }

  @PostMapping("/reminder")
  @PreAuthorize("hasRole('CASEWORKER') or hasRole('ADMIN')")
  @Operation(summary = "Issue reminder notice (rykker)")
  public ResponseEntity<NotificationDto> issueReminder(
      @PathVariable UUID debtId, @Valid @RequestBody IssueDemandRequest request) {
    NotificationDto notification =
        notificationService.issueReminder(debtId, request.getCreditorOrgId());
    return ResponseEntity.status(HttpStatus.CREATED).body(notification);
  }

  @GetMapping("/notifications")
  @PreAuthorize("hasRole('CASEWORKER') or hasRole('ADMIN')")
  @Operation(summary = "Get notification history for a debt")
  public ResponseEntity<List<NotificationDto>> getNotificationHistory(@PathVariable UUID debtId) {
    return ResponseEntity.ok(notificationService.getNotificationHistory(debtId));
  }
}
