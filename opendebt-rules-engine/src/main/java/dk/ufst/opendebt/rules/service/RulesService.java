package dk.ufst.opendebt.rules.service;

import java.util.List;

import dk.ufst.opendebt.rules.model.*;

public interface RulesService {

  /** Evaluates if a debt is ready for collection (indrivelsesparat). */
  DebtReadinessResult evaluateReadiness(DebtReadinessRequest request);

  /** Calculates interest for a debt based on debt type and duration. */
  InterestCalculationResult calculateInterest(InterestCalculationRequest request);

  /** Determines collection priority for offsetting/garnishment. */
  CollectionPriorityResult determineCollectionPriority(CollectionPriorityRequest request);

  /** Sorts debts by collection priority. */
  List<CollectionPriorityResult> sortByCollectionPriority(List<CollectionPriorityRequest> debts);
}
