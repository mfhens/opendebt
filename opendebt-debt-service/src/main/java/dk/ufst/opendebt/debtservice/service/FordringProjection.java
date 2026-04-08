package dk.ufst.opendebt.debtservice.service;

import java.math.BigDecimal;
import java.util.UUID;

public record FordringProjection(
    UUID fordringId, BigDecimal tilbaestaaendeBeloeb, java.time.LocalDate registreringsdato) {}
