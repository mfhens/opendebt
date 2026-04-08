package dk.ufst.opendebt.debtservice.service;

import java.math.BigDecimal;
import java.util.List;

public record TierAllocationResult(
    List<FordringAllocation> tier1Allocations,
    List<FordringAllocation> tier2Allocations,
    List<FordringAllocation> tier3Allocations,
    BigDecimal residualPayoutAmount) {}
