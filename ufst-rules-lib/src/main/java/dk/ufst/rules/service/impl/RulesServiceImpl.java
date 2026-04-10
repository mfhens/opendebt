package dk.ufst.rules.service.impl;

import java.util.Comparator;
import java.util.List;

import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;

import dk.ufst.rules.model.CollectionPriorityRequest;
import dk.ufst.rules.model.CollectionPriorityResult;
import dk.ufst.rules.model.DebtReadinessRequest;
import dk.ufst.rules.model.DebtReadinessResult;
import dk.ufst.rules.model.InterestCalculationRequest;
import dk.ufst.rules.model.InterestCalculationResult;
import dk.ufst.rules.service.RulesService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class RulesServiceImpl implements RulesService {

  private static final String RESULT_GLOBAL = "result";

  private final KieContainer kieContainer;

  @Override
  public DebtReadinessResult evaluateReadiness(DebtReadinessRequest request) {
    log.debug("Evaluating readiness for debt: {}", request.getDebtId());

    DebtReadinessResult result =
        DebtReadinessResult.builder()
            .debtId(request.getDebtId())
            .ready(true)
            .status(DebtReadinessResult.ReadinessStatus.READY_FOR_COLLECTION)
            .build();

    KieSession kieSession = kieContainer.newKieSession();
    try {
      kieSession.setGlobal(RESULT_GLOBAL, result);
      kieSession.insert(request);
      kieSession.fireAllRules();
    } finally {
      kieSession.dispose();
    }

    log.info(
        "Readiness evaluation for debt {}: ready={}, status={}",
        request.getDebtId(),
        result.isReady(),
        result.getStatus());

    return result;
  }

  @Override
  public InterestCalculationResult calculateInterest(InterestCalculationRequest request) {
    log.debug("Calculating interest for debt type: {}", request.getDebtTypeCode());

    InterestCalculationResult result =
        InterestCalculationResult.builder().daysCalculated(request.getDaysPastDue()).build();

    KieSession kieSession = kieContainer.newKieSession();
    try {
      kieSession.setGlobal(RESULT_GLOBAL, result);
      kieSession.insert(request);
      kieSession.fireAllRules();
    } finally {
      kieSession.dispose();
    }

    return result;
  }

  @Override
  public CollectionPriorityResult determineCollectionPriority(CollectionPriorityRequest request) {
    log.debug("Determining collection priority for debt: {}", request.getDebtId());

    CollectionPriorityResult result =
        CollectionPriorityResult.builder()
            .debtId(request.getDebtId())
            .priorityRank(CollectionPriorityResult.PRIORITY_DEFAULT)
            .build();

    KieSession kieSession = kieContainer.newKieSession();
    try {
      kieSession.setGlobal(RESULT_GLOBAL, result);
      kieSession.insert(request);
      kieSession.fireAllRules();
    } finally {
      kieSession.dispose();
    }

    return result;
  }

  @Override
  public List<CollectionPriorityResult> sortByCollectionPriority(
      List<CollectionPriorityRequest> debts) {
    // GIL § 4 inter-claim sort order (TB-034):
    // 1. priorityRank ascending (lower rank = higher priority)
    // 2. underholdsbidragOrdning ascending (1 = privatretlig before 2 = offentlig; 0 for non-kat3)
    // 3. fifoSortKey ascending (oldest first — GIL § 4, stk. 2, FR-2.1/FR-2.2)
    Comparator<CollectionPriorityResult> gil4Comparator =
        Comparator.comparingInt(CollectionPriorityResult::getPriorityRank)
            .thenComparingInt(CollectionPriorityResult::getUnderholdsbidragOrdning)
            .thenComparing(
                CollectionPriorityResult::getFifoSortKey,
                Comparator.nullsLast(Comparator.naturalOrder()));

    return debts.stream().map(this::determineCollectionPriority).sorted(gil4Comparator).toList();
  }
}
