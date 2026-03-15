package dk.ufst.opendebt.rules.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dk.ufst.opendebt.rules.model.*;

@ExtendWith(MockitoExtension.class)
class RulesServiceImplTest {

  @Mock private KieContainer kieContainer;
  @Mock private KieSession kieSession;
  @InjectMocks private RulesServiceImpl rulesService;

  @BeforeEach
  void setUp() {
    when(kieContainer.newKieSession()).thenReturn(kieSession);
  }

  @Test
  void evaluateReadiness_createsSessionAndFiresRules() {
    DebtReadinessRequest request =
        DebtReadinessRequest.builder()
            .debtId(UUID.randomUUID())
            .principalAmount(BigDecimal.valueOf(1000))
            .build();

    DebtReadinessResult result = rulesService.evaluateReadiness(request);

    assertThat(result).isNotNull();
    assertThat(result.getDebtId()).isEqualTo(request.getDebtId());
    assertThat(result.isReady()).isTrue();
    assertThat(result.getStatus())
        .isEqualTo(DebtReadinessResult.ReadinessStatus.READY_FOR_COLLECTION);

    verify(kieSession).setGlobal(eq("result"), any());
    verify(kieSession).insert(request);
    verify(kieSession).fireAllRules();
    verify(kieSession).dispose();
  }

  @Test
  void evaluateReadiness_disposesSessionOnException() {
    when(kieSession.fireAllRules()).thenThrow(new RuntimeException("error"));

    DebtReadinessRequest request = DebtReadinessRequest.builder().debtId(UUID.randomUUID()).build();

    try {
      rulesService.evaluateReadiness(request);
    } catch (RuntimeException e) {
      // expected
    }

    verify(kieSession).dispose();
  }

  @Test
  void calculateInterest_createsSessionAndFiresRules() {
    InterestCalculationRequest request =
        InterestCalculationRequest.builder()
            .debtTypeCode("TAX")
            .principalAmount(BigDecimal.valueOf(10000))
            .daysPastDue(30)
            .build();

    InterestCalculationResult result = rulesService.calculateInterest(request);

    assertThat(result).isNotNull();
    assertThat(result.getDaysCalculated()).isEqualTo(30);
    verify(kieSession).setGlobal(eq("result"), any());
    verify(kieSession).insert(request);
    verify(kieSession).fireAllRules();
    verify(kieSession).dispose();
  }

  @Test
  void determineCollectionPriority_createsSessionAndFiresRules() {
    UUID debtId = UUID.randomUUID();
    CollectionPriorityRequest request =
        CollectionPriorityRequest.builder().debtId(debtId).debtTypeCode("TAX").build();

    CollectionPriorityResult result = rulesService.determineCollectionPriority(request);

    assertThat(result).isNotNull();
    assertThat(result.getDebtId()).isEqualTo(debtId);
    assertThat(result.getPriorityRank()).isEqualTo(CollectionPriorityResult.PRIORITY_DEFAULT);
    verify(kieSession).fireAllRules();
    verify(kieSession).dispose();
  }

  @Test
  void sortByCollectionPriority_returnsSortedResults() {
    UUID id1 = UUID.randomUUID();
    UUID id2 = UUID.randomUUID();

    List<CollectionPriorityRequest> requests =
        List.of(
            CollectionPriorityRequest.builder().debtId(id1).debtTypeCode("OTHER").build(),
            CollectionPriorityRequest.builder().debtId(id2).debtTypeCode("TAX").build());

    List<CollectionPriorityResult> results = rulesService.sortByCollectionPriority(requests);

    assertThat(results).hasSize(2);
    // Both get default priority since rules don't fire (mocked)
    assertThat(results.get(0).getPriorityRank())
        .isLessThanOrEqualTo(results.get(1).getPriorityRank());
  }
}
