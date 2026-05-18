package dk.ufst.opendebt.debtservice.limitation.service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import dk.ufst.opendebt.common.audit.cls.ClsAuditClient;
import dk.ufst.opendebt.common.audit.cls.ClsAuditEvent;
import dk.ufst.opendebt.debtservice.limitation.entity.AfbrydelseEvent;
import dk.ufst.opendebt.debtservice.limitation.entity.TillaegsfristEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class LimitationAuditPublisher {

  private final ClsAuditClient clsAuditClient;

  public void publishInterruption(AfbrydelseEvent event, String eventDescription) {
    ship(
        event.getFordringId(),
        event.getId(),
        eventDescription,
        event.getLegalReference(),
        Map.of(
            "fordringId", event.getFordringId().toString(),
            "type", event.getType().name(),
            "legalReference", event.getLegalReference(),
            "eventDescription", eventDescription));
  }

  public void publishSupplementary(TillaegsfristEvent event) {
    ship(
        event.getFordringId(),
        event.getId(),
        "tillægsfristregistrering",
        event.getLegalReference(),
        Map.of(
            "fordringId", event.getFordringId().toString(),
            "type", event.getType(),
            "legalReference", event.getLegalReference(),
            "eventDescription", "tillægsfristregistrering"));
  }

  public void publishObjectionRegistered(UUID fordringId, UUID indsigelsesId) {
    ship(
        fordringId,
        indsigelsesId,
        "indsigelsesregistrering",
        "G.A.2.4.6",
        Map.of(
            "fordringId",
            fordringId.toString(),
            "indsigelsesId",
            indsigelsesId.toString(),
            "legalReference",
            "G.A.2.4.6",
            "eventDescription",
            "indsigelsesregistrering"));
  }

  public void publishObjectionEvaluated(UUID fordringId, UUID indsigelsesId, String outcome) {
    ship(
        fordringId,
        indsigelsesId,
        "indsigelsesevaluering med " + outcome + " udfald",
        "G.A.2.4.6",
        Map.of(
            "fordringId",
            fordringId.toString(),
            "indsigelsesId",
            indsigelsesId.toString(),
            "outcome",
            outcome,
            "legalReference",
            "G.A.2.4.6",
            "eventDescription",
            "indsigelsesevaluering med " + outcome + " udfald"));
  }

  private void ship(
      UUID fordringId,
      UUID resourceId,
      String operation,
      String legalReference,
      Map<String, Object> values) {
    try {
      clsAuditClient.shipEvent(
          ClsAuditEvent.builder()
              .eventId(UUID.randomUUID())
              .timestamp(Instant.now())
              .serviceName("debt-service")
              .operation(operation)
              .resourceType("limitation")
              .resourceId(resourceId)
              .userId(currentActor())
              .newValues(values)
              .build());
    } catch (Exception ex) {
      log.warn(
          "Failed to ship limitation CLS audit for {} / {}: {}",
          fordringId,
          legalReference,
          ex.getMessage());
    }
  }

  // AIDEV-REFACTOR: currentActor() is duplicated in LimitationObjectionFacade; extract to
  // SecurityContextUtils
  private String currentActor() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || authentication.getName() == null
        || authentication.getName().isBlank()) {
      return "system";
    }
    return authentication.getName();
  }
}
