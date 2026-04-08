package dk.ufst.opendebt.debtservice.service;

import java.math.BigDecimal;
import java.util.UUID;

public record KorrektionspuljeResult(
    BigDecimal step1Consumed,
    BigDecimal gendaekketAmount,
    UUID poolEntryId,
    BigDecimal poolAmount) {}
