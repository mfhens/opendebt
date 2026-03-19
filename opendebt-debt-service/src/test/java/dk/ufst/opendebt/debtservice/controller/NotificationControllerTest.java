package dk.ufst.opendebt.debtservice.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import dk.ufst.opendebt.debtservice.dto.IssueDemandRequest;
import dk.ufst.opendebt.debtservice.dto.NotificationDto;
import dk.ufst.opendebt.debtservice.service.NotificationService;

@WebMvcTest(
    controllers = NotificationController.class,
    properties = {
      "spring.autoconfigure.exclude="
          + "org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration,"
          + "org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration"
    })
class NotificationControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @MockBean private NotificationService notificationService;

  private static final UUID DEBT_ID = UUID.randomUUID();
  private static final UUID CREDITOR_ORG_ID = UUID.randomUUID();
  private static final UUID NOTIFICATION_ID = UUID.randomUUID();

  @Test
  void issueDemandForPayment_returns201() throws Exception {
    NotificationDto dto =
        NotificationDto.builder()
            .id(NOTIFICATION_ID)
            .type("PAAKRAV")
            .debtId(DEBT_ID)
            .senderCreditorOrgId(CREDITOR_ORG_ID)
            .recipientPersonId(UUID.randomUUID())
            .channel("DIGITAL_POST")
            .deliveryState("PENDING")
            .sentAt(Instant.now())
            .ocrLine("+71<ABCD1234EFGH5678+")
            .build();

    when(notificationService.issueDemandForPayment(eq(DEBT_ID), eq(CREDITOR_ORG_ID)))
        .thenReturn(dto);

    IssueDemandRequest request = new IssueDemandRequest();
    request.setCreditorOrgId(CREDITOR_ORG_ID);

    mockMvc
        .perform(
            post("/api/v1/debts/{debtId}/demand-for-payment", DEBT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.type").value("PAAKRAV"))
        .andExpect(jsonPath("$.ocrLine").value("+71<ABCD1234EFGH5678+"))
        .andExpect(jsonPath("$.deliveryState").value("PENDING"));
  }

  @Test
  void issueReminder_returns201() throws Exception {
    NotificationDto dto =
        NotificationDto.builder()
            .id(NOTIFICATION_ID)
            .type("RYKKER")
            .debtId(DEBT_ID)
            .senderCreditorOrgId(CREDITOR_ORG_ID)
            .recipientPersonId(UUID.randomUUID())
            .channel("DIGITAL_POST")
            .deliveryState("PENDING")
            .sentAt(Instant.now())
            .build();

    when(notificationService.issueReminder(eq(DEBT_ID), eq(CREDITOR_ORG_ID))).thenReturn(dto);

    IssueDemandRequest request = new IssueDemandRequest();
    request.setCreditorOrgId(CREDITOR_ORG_ID);

    mockMvc
        .perform(
            post("/api/v1/debts/{debtId}/reminder", DEBT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.type").value("RYKKER"));
  }

  @Test
  void getNotificationHistory_returnsOk() throws Exception {
    NotificationDto dto =
        NotificationDto.builder()
            .id(NOTIFICATION_ID)
            .type("PAAKRAV")
            .debtId(DEBT_ID)
            .deliveryState("SENT")
            .build();

    when(notificationService.getNotificationHistory(eq(DEBT_ID))).thenReturn(List.of(dto));

    mockMvc
        .perform(get("/api/v1/debts/{debtId}/notifications", DEBT_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].type").value("PAAKRAV"))
        .andExpect(jsonPath("$[0].deliveryState").value("SENT"));
  }

  @Test
  void getNotificationHistory_emptyList_returnsOk() throws Exception {
    when(notificationService.getNotificationHistory(eq(DEBT_ID))).thenReturn(List.of());

    mockMvc
        .perform(get("/api/v1/debts/{debtId}/notifications", DEBT_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$").isEmpty());
  }
}
