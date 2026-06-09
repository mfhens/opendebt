package dk.ufst.opendebt.debtservice.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.kie.api.runtime.KieContainer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import dk.ufst.opendebt.common.audit.cls.ClsAuditClient;
import dk.ufst.opendebt.debtservice.client.CaseServiceClient;
import dk.ufst.opendebt.debtservice.client.CreditorDisplayClient;
import dk.ufst.opendebt.debtservice.client.CreditorServiceClient;
import dk.ufst.opendebt.debtservice.client.ValidateActionRequest;
import dk.ufst.opendebt.debtservice.client.ValidateActionResponse;
import dk.ufst.opendebt.debtservice.dto.ClaimValidationResult;
import dk.ufst.opendebt.debtservice.limitation.client.LimitationObjectionWorkflowClient;
import dk.ufst.opendebt.debtservice.limitation.client.WageGarnishmentFactClient;
import dk.ufst.opendebt.debtservice.limitation.client.dto.CreateObjectionWorkflowRequest;
import dk.ufst.opendebt.debtservice.limitation.client.dto.ObjectionDecisionRequest;
import dk.ufst.opendebt.debtservice.limitation.client.dto.ObjectionWorkflowResult;
import dk.ufst.opendebt.debtservice.limitation.client.dto.WageGarnishmentLimitationFacts;
import dk.ufst.opendebt.debtservice.section50.client.PaymentCoverageOrderClient;
import dk.ufst.opendebt.debtservice.service.ClaimValidationService;
import dk.ufst.rules.model.InterestCalculationRequest;
import dk.ufst.rules.model.InterestCalculationResult;
import dk.ufst.rules.service.RulesService;

@TestConfiguration
public class TestConfig {

  @Bean
  @Primary
  public KieContainer kieContainer() {
    return mock(KieContainer.class);
  }

  @Bean
  @Primary
  public RulesService rulesService() {
    RulesService mockService = mock(RulesService.class);
    when(mockService.calculateInterest(any(InterestCalculationRequest.class)))
        .thenAnswer(
            inv -> {
              InterestCalculationRequest req = inv.getArgument(0);
              if (req.getAnnualRate() == null
                  || req.getPrincipalAmount() == null
                  || req.getAnnualRate().signum() == 0) {
                return InterestCalculationResult.builder().interestAmount(BigDecimal.ZERO).build();
              }
              BigDecimal daily =
                  req.getPrincipalAmount()
                      .multiply(req.getAnnualRate())
                      .divide(new BigDecimal("365"), 2, java.math.RoundingMode.HALF_UP);
              return InterestCalculationResult.builder().interestAmount(daily).build();
            });
    return mockService;
  }

  @Bean
  @Primary
  public MutableClock limitationClock() {
    return new MutableClock(Instant.parse("2024-01-01T00:00:00Z"), ZoneId.of("UTC"));
  }

  @Bean
  @Primary
  public ClaimValidationService claimValidationService() {
    return (claim, context) -> {
      ClaimValidationResult result = ClaimValidationResult.builder().build();

      if (claim.getDebtTypeCode() == null || claim.getDebtTypeCode().isBlank()) {
        result
            .getErrors()
            .add(
                ClaimValidationResult.ValidationError.builder()
                    .ruleId("Rule151")
                    .errorCode("TYPE_AGREEMENT_MISSING")
                    .description("Fordringstype er paakraevet")
                    .build());
      }

      if (claim.getClaimArt() != null
          && !claim.getClaimArt().isBlank()
          && !("INDR".equals(claim.getClaimArt().toUpperCase(Locale.ROOT))
              || "MODR".equals(claim.getClaimArt().toUpperCase(Locale.ROOT)))) {
        result
            .getErrors()
            .add(
                ClaimValidationResult.ValidationError.builder()
                    .ruleId("Rule411")
                    .errorCode("FORDRING_TYPE_ERROR")
                    .description(
                        "Fordringsart skal vaere inddrivelse (INDR) eller modregning (MODR)")
                    .build());
      }

      if (claim.getPeriodFrom() != null
          && claim.getPeriodTo() != null
          && claim.getPeriodFrom().isAfter(claim.getPeriodTo())) {
        result
            .getErrors()
            .add(
                ClaimValidationResult.ValidationError.builder()
                    .ruleId("Rule569")
                    .errorCode("PERIODE_TIL_EFTER_PERIODE_FRA")
                    .description("Periodens startdato kan ikke vaere efter slutdato")
                    .build());
      }

      return result;
    };
  }

  @Bean
  @Primary
  public CreditorServiceClient creditorServiceClient() {
    CreditorServiceClient mockClient = mock(CreditorServiceClient.class);
    when(mockClient.validateAction(any(UUID.class), any(ValidateActionRequest.class)))
        .thenReturn(ValidateActionResponse.builder().allowed(true).build());
    when(mockClient.isCreditorAllowedToCreateClaim(any(UUID.class))).thenReturn(true);
    when(mockClient.isCreditorAllowedToUpdateClaim(any(UUID.class))).thenReturn(true);
    return mockClient;
  }

  @Bean
  @Primary
  public CreditorDisplayClient creditorDisplayClient() {
    CreditorDisplayClient mockClient = mock(CreditorDisplayClient.class);
    when(mockClient.getDisplayName(any(UUID.class)))
        .thenAnswer(invocation -> "Creditor-" + invocation.getArgument(0, UUID.class).toString());
    return mockClient;
  }

  @Bean
  @Primary
  public ClsAuditClient clsAuditClient() {
    ClsAuditClient mockClient = mock(ClsAuditClient.class);
    when(mockClient.isEnabled()).thenReturn(false);
    return mockClient;
  }

  @Bean
  @Primary
  public CaseServiceClient caseServiceClient() {
    CaseServiceClient mockClient = mock(CaseServiceClient.class);
    CaseServiceClient.CaseAssignmentResult result = new CaseServiceClient.CaseAssignmentResult();
    result.setCaseId(UUID.randomUUID());
    result.setCaseNumber("CASE-TEST-001");
    result.setNewCase(true);
    when(mockClient.assignDebtToCase(any(String.class), any(String.class))).thenReturn(result);
    return mockClient;
  }

  @Bean
  @Primary
  public LimitationObjectionWorkflowClient limitationObjectionWorkflowClient() {
    LimitationObjectionWorkflowClient mockClient = mock(LimitationObjectionWorkflowClient.class);
    when(mockClient.createWorkflow(any(CreateObjectionWorkflowRequest.class)))
        .thenAnswer(
            invocation ->
                ObjectionWorkflowResult.builder()
                    .indsigelsesId(UUID.randomUUID())
                    .workflowCaseId(UUID.randomUUID())
                    .workflowStatus("REGISTERED")
                    .build());
    when(mockClient.recordDecision(any(UUID.class), any(ObjectionDecisionRequest.class)))
        .thenAnswer(
            invocation -> {
              UUID indsigelsesId = invocation.getArgument(0);
              ObjectionDecisionRequest request = invocation.getArgument(1);
              return ObjectionWorkflowResult.builder()
                  .indsigelsesId(indsigelsesId)
                  .workflowCaseId(UUID.randomUUID())
                  .workflowStatus(request.getOutcome())
                  .authoritativeOutcome(request.getOutcome())
                  .decidedAt(java.time.Instant.now())
                  .build();
            });
    return mockClient;
  }

  @Bean
  @Primary
  public WageGarnishmentFactClient wageGarnishmentFactClient() {
    WageGarnishmentFactClient mockClient = mock(WageGarnishmentFactClient.class);
    when(mockClient.getFacts(any(UUID.class)))
        .thenReturn(
            WageGarnishmentLimitationFacts.builder()
                .decisionRegistered(false)
                .coveredFordringIds(List.of())
                .build());
    return mockClient;
  }

  @Bean
  @Primary
  public PaymentCoverageOrderClient paymentCoverageOrderClient() {
    PaymentCoverageOrderClient mockClient = mock(PaymentCoverageOrderClient.class);
    when(mockClient.orderPrincipalClaimIds(any(UUID.class), any(), any(List.class)))
        .thenAnswer(invocation -> List.copyOf(invocation.getArgument(2)));
    return mockClient;
  }

  public static class MutableClock extends Clock {

    private final AtomicReference<Instant> instant;
    private final ZoneId zone;

    public MutableClock(Instant initialInstant, ZoneId zone) {
      this.instant = new AtomicReference<>(initialInstant);
      this.zone = zone;
    }

    @Override
    public ZoneId getZone() {
      return zone;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return new MutableClock(instant.get(), zone);
    }

    @Override
    public Instant instant() {
      return instant.get();
    }

    public void setInstant(Instant newInstant) {
      instant.set(newInstant);
    }

    public void setDate(LocalDate date) {
      setInstant(date.atStartOfDay(zone).toInstant());
    }
  }
}
