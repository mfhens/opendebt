package dk.ufst.opendebt.debtservice.service.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import dk.ufst.opendebt.common.audit.cls.ClsAuditClient;
import dk.ufst.opendebt.common.dto.DebtDto;
import dk.ufst.opendebt.debtservice.client.CaseServiceClient;
import dk.ufst.opendebt.debtservice.client.CreditorServiceClient;
import dk.ufst.opendebt.debtservice.client.ValidateActionResponse;
import dk.ufst.opendebt.debtservice.dto.ClaimSubmissionResponse;
import dk.ufst.opendebt.debtservice.repository.DebtRepository;
import dk.ufst.opendebt.debtservice.service.ClaimSubmissionService;
import dk.ufst.opendebt.debtservice.service.ClaimValidationContext;

@SpringBootTest
@ActiveProfiles("test")
class ClaimSubmissionDroolsIntegrationTest {

  @Autowired private ClaimSubmissionService claimSubmissionService;
  @Autowired private DebtRepository debtRepository;

  @MockBean private CreditorServiceClient creditorServiceClient;
  @MockBean private CaseServiceClient caseServiceClient;
  @MockBean private ClsAuditClient clsAuditClient;

  @BeforeEach
  void setUp() {
    debtRepository.deleteAll();
    when(creditorServiceClient.validateAction(any(UUID.class), any()))
        .thenReturn(ValidateActionResponse.builder().allowed(true).build());
    when(clsAuditClient.isEnabled()).thenReturn(false);

    CaseServiceClient.CaseAssignmentResult assignment =
        new CaseServiceClient.CaseAssignmentResult();
    assignment.setCaseId(UUID.randomUUID());
    when(caseServiceClient.assignDebtToCase(any(String.class), any(String.class)))
        .thenReturn(assignment);
  }

  @Test
  @DisplayName("Portal claim submission persists debt when the Drools fordring rules accept it")
  void portalSubmissionAcceptedByDroolsCreatesDebt() {
    ClaimSubmissionResponse response =
        claimSubmissionService.submitClaim(validClaim(), ClaimValidationContext.portal());

    assertThat(response.getOutcome()).isEqualTo(ClaimSubmissionResponse.Outcome.UDFOERT);
    assertThat(response.getClaimId()).isNotNull();
    assertThat(response.getErrors()).isEmpty();
    assertThat(debtRepository.count()).isEqualTo(1);
    assertThat(debtRepository.findById(response.getClaimId())).isPresent();
  }

  @Test
  @DisplayName("System-to-system claim submission returns AFVIST when Drools rejects the claim")
  void systemToSystemSubmissionRejectedByDroolsDoesNotCreateDebt() {
    DebtDto invalidClaim = validClaim();
    invalidClaim.setClaimArt("INVALID");

    ClaimSubmissionResponse response =
        claimSubmissionService.submitClaim(invalidClaim, ClaimValidationContext.systemToSystem());

    assertThat(response.getOutcome()).isEqualTo(ClaimSubmissionResponse.Outcome.AFVIST);
    assertThat(response.getClaimId()).isNull();
    assertThat(response.getErrors())
        .extracting(error -> error.getRuleId() + ":" + error.getErrorCode())
        .contains("Rule411:FORDRING_TYPE_ERROR");
    assertThat(debtRepository.count()).isZero();
  }

  private DebtDto validClaim() {
    return DebtDto.builder()
        .debtorId(UUID.randomUUID().toString())
        .creditorId(UUID.randomUUID().toString())
        .debtTypeCode("HF01")
        .claimArt("INDR")
        .principalAmount(new BigDecimal("5000.00"))
        .dueDate(LocalDate.now().plusDays(30))
        .paymentDeadline(LocalDate.now().plusDays(14))
        .periodFrom(LocalDate.of(2024, 1, 1))
        .periodTo(LocalDate.of(2024, 12, 31))
        .inceptionDate(LocalDate.of(2024, 1, 1))
        .limitationDate(LocalDate.now().plusYears(2))
        .creditorReference("TB-058-REF")
        .description("Validated claim")
        .build();
  }
}
