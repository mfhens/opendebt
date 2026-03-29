package dk.ufst.opendebt.payment.daekning.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import dk.ufst.opendebt.payment.daekning.PrioritetKategori;
import dk.ufst.opendebt.payment.daekning.RenteKomponent;

public record DaekningsraekkefoelgePositionDto(
    String fordringId,
    PrioritetKategori prioritetKategori,
    String gilParagraf,
    RenteKomponent komponent,
    LocalDate fifoSortKey,
    BigDecimal tilbaestaaendeBeloeb,
    String opskrivningAfFordringId) {}
