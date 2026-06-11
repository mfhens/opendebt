package dk.ufst.opendebt.debtservice.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import dk.ufst.opendebt.debtservice.entity.ModregningEvent;
import dk.ufst.opendebt.debtservice.entity.NotificationOutboxEntity;
import dk.ufst.opendebt.debtservice.repository.NotificationOutboxRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ModregningNotificationOutboxWriter {

  private final NotificationOutboxRepository notificationOutboxRepository;
  private final ObjectMapper objectMapper;

  public void write(ModregningEvent event, List<FordringCoverageDto> coverages) {
    String payload;
    try {
      payload =
          objectMapper.writeValueAsString(
              Map.of(
                  "debtorPersonId", event.getDebtorPersonId().toString(),
                  "eventId", event.getId().toString(),
                  "decisionReference", event.getDecisionReference(),
                  "lineageReference", event.getLineageReference(),
                  "decisionDate", event.getDecisionDate().toString(),
                  "tierBreakdown",
                      Map.of(
                          "tier1Amount", event.getTier1Amount(),
                          "tier2Amount", event.getTier2Amount(),
                          "tier3Amount", event.getTier3Amount()),
                  "coverageCount", coverages.size()));
    } catch (JsonProcessingException e) {
      payload = "{\"eventId\":\"" + event.getId() + "\"}";
    }
    notificationOutboxRepository.save(
        NotificationOutboxEntity.builder()
            .modregningEventId(event.getId())
            .debtorPersonId(event.getDebtorPersonId())
            .payload(payload)
            .build());
  }
}
