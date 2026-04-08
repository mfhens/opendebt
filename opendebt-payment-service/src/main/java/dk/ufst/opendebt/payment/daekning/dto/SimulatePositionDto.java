package dk.ufst.opendebt.payment.daekning.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import dk.ufst.opendebt.payment.daekning.PrioritetKategori;
import dk.ufst.opendebt.payment.daekning.RenteKomponent;

public record SimulatePositionDto(
    String fordringId,
    PrioritetKategori prioritetKategori,
    String gilParagraf,
    RenteKomponent komponent,
    LocalDate fifoSortKey,
    BigDecimal tilbaestaaendeBeloeb,
    String opskrivningAfFordringId,
    BigDecimal daekningBeloeb,
    boolean fullyCovers) {}
