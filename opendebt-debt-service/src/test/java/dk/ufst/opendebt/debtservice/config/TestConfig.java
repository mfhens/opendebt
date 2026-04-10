package dk.ufst.opendebt.debtservice.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.UUID;

import org.kie.api.runtime.KieContainer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import dk.ufst.opendebt.common.audit.cls.ClsAuditClient;
import dk.ufst.opendebt.debtservice.client.CaseServiceClient;
import dk.ufst.opendebt.debtservice.client.CreditorServiceClient;
import dk.ufst.opendebt.debtservice.client.ValidateActionRequest;
import dk.ufst.opendebt.debtservice.client.ValidateActionResponse;
import dk.ufst.rules.model.InterestCalculationRequest;
import dk.ufst.rules.model.InterestCalculationResult;
import dk.ufst.rules.service.RulesService;

@TestConfiguration
public class TestConfig {

  /**
   * Provide a mock KieContainer so that DroolsAutoConfiguration does not attempt to compile DRL
   * files at test startup (avoids classloader/KieBase conflicts in unit- and Cucumber tests).
   */
  @Bean
  @Primary
  public KieContainer kieContainer() {
    return mock(KieContainer.class);
  }

  /**
   * Provide a mock RulesService that computes interest using the caller-supplied annualRate and
   * principalAmount (mirrors the DRL formula: principal * rate / 365 per day). This allows Cucumber
   * batch-accrual tests to produce real journal entries while avoiding the full Drools KieContainer
   * startup cost.
   */
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
}
