package dk.ufst.opendebt.debtservice.service;

import java.math.BigDecimal;
import java.util.UUID;

public record FordringAllocation(UUID fordringId, BigDecimal amountCovered, int tier) {}
