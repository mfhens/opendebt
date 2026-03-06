package dk.ufst.opendebt.payment.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import dk.ufst.opendebt.common.dto.DebtDto;

class DebtServiceClientTest {

  private MockRestServiceServer server;
  private DebtServiceClient client;

  @BeforeEach
  void setUp() {
    RestClient.Builder builder = RestClient.builder();
    server = MockRestServiceServer.bindTo(builder).build();
    client = new DebtServiceClient(builder, "http://localhost:8081");
  }

  @Test
  void findByOcrLine_callsDebtServiceEndpoint() {
    server
        .expect(requestTo("http://localhost:8081/debt-service/api/v1/debts/by-ocr?ocrLine=OCR-123"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(
            withSuccess(
                """
                [{
                  "id": "11111111-1111-1111-1111-111111111111",
                  "debtorId": "22222222-2222-2222-2222-222222222222",
                  "creditorId": "33333333-3333-3333-3333-333333333333",
                  "debtTypeCode": "600",
                  "principalAmount": 1000.00,
                  "outstandingBalance": 800.00,
                  "ocrLine": "OCR-123"
                }]
                """,
                MediaType.APPLICATION_JSON));

    List<DebtDto> result = client.findByOcrLine("OCR-123");

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getOcrLine()).isEqualTo("OCR-123");
    assertThat(result.get(0).getOutstandingBalance()).isEqualByComparingTo("800.0");
    server.verify();
  }

  @Test
  void writeDown_callsDebtServiceEndpoint() {
    UUID debtId = UUID.randomUUID();
    server
        .expect(
            requestTo(
                "http://localhost:8081/debt-service/api/v1/debts/"
                    + debtId
                    + "/write-down?amount=100"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(
            withSuccess(
                ("""
                {
                  "id": "%s",
                  "debtorId": "22222222-2222-2222-2222-222222222222",
                  "creditorId": "33333333-3333-3333-3333-333333333333",
                  "debtTypeCode": "600",
                  "principalAmount": 1000.00,
                  "outstandingBalance": 900.00,
                  "ocrLine": "OCR-123"
                }
                """
                    .formatted(debtId)),
                MediaType.APPLICATION_JSON));

    DebtDto result = client.writeDown(debtId, new BigDecimal("100"));

    assertThat(result.getId()).isEqualTo(debtId);
    assertThat(result.getOutstandingBalance()).isEqualByComparingTo("900.0");
    server.verify();
  }
}
