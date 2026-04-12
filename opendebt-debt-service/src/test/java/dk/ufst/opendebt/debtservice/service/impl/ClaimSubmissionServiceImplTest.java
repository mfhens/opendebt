package dk.ufst.opendebt.debtservice.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dk.ufst.opendebt.common.dto.DebtDto;
import dk.ufst.opendebt.debtservice.client.CaseServiceClient;
import dk.ufst.opendebt.debtservice.dto.ClaimSubmissionResponse;
import dk.ufst.opendebt.debtservice.dto.ClaimValidationResult;
import dk.ufst.opendebt.debtservice.service.ClaimValidationContext;
import dk.ufst.opendebt.debtservice.service.ClaimValidationService;
import dk.ufst.opendebt.debtservice.service.DebtService;

@ExtendWith(MockitoExtension.class)
class ClaimSubmissionServiceImplTest {

  @Mock private ClaimValidationService validationService;
  @Mock private DebtService debtService;
  @Mock private CaseServiceClient caseServiceClient;

  private ClaimSubmissionServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new ClaimSubmissionServiceImpl(validationService, debtService, caseServiceClient);
  }

  private DebtDto buildClaim() {
    return DebtDto.builder()
        .debtorId("1234567890")
        .creditorId("12345678")
        .debtTypeCode("300")
        .principalAmount(new BigDecimal("5000"))
        .dueDate(LocalDate.now().minusMonths(1))
        .build();
  }

  @Test
  void submitClaim_invalidClaim_returnsAfvist() {
    DebtDto claim = buildClaim();
    ClaimValidationResult invalid =
        ClaimValidationResult.builder()
            .errors(
                List.of(
                    ClaimValidationResult.ValidationError.builder()
                        .ruleId("R1")
                        .errorCode("MISSING_DUE_DATE")
                        .description("Due date is required")
                        .build()))
            .build();
    when(validationService.validate(eq(claim), any(ClaimValidationContext.class)))
        .thenReturn(invalid);

    ClaimSubmissionResponse response = service.submitClaim(claim);

    assertThat(response.getOutcome()).isEqualTo(ClaimSubmissionResponse.Outcome.AFVIST);
    assertThat(response.getErrors()).hasSize(1);
    verify(debtService, never()).createDebt(any());
  }

  @Test
  void submitClaim_validClaim_withSuccessfulCaseAssignment_returnsUdfoert() {
    DebtDto claim = buildClaim();
    ClaimValidationResult valid = ClaimValidationResult.builder().build();
    UUID createdId = UUID.randomUUID();
    UUID caseId = UUID.randomUUID();
    DebtDto created = DebtDto.builder().id(createdId).debtorId("1234567890").build();

    CaseServiceClient.CaseAssignmentResult assignment =
        new CaseServiceClient.CaseAssignmentResult();
    assignment.setCaseId(caseId);

    when(validationService.validate(eq(claim), any(ClaimValidationContext.class)))
        .thenReturn(valid);
    when(debtService.createDebt(claim)).thenReturn(created);
    when(caseServiceClient.assignDebtToCase(createdId.toString(), "1234567890"))
        .thenReturn(assignment);

    ClaimSubmissionResponse response = service.submitClaim(claim);

    assertThat(response.getOutcome()).isEqualTo(ClaimSubmissionResponse.Outcome.UDFOERT);
    assertThat(response.getClaimId()).isEqualTo(createdId);
    assertThat(response.getCaseId()).isEqualTo(caseId);
  }
}
