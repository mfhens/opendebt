package dk.ufst.opendebt.debtservice.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Represents a public disbursement event from Nemkonto that triggers FR-1 modregning. */
public record PublicDisbursementEvent(
    String nemkontoReferenceId,
    UUID debtorPersonId,
    BigDecimal disbursementAmount,
    String paymentType,
    Integer indkomstAar,
    UUID payingAuthorityOrgId,
    LocalDate receiptDate,
    LocalDate decisionDate) {}
