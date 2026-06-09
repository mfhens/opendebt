package dk.ufst.opendebt.citizen.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CitizenDebtItem(
    UUID debtId,
    String debtTypeCode,
    String debtTypeName,
    String creditorDisplayName,
    BigDecimal principalAmount,
    BigDecimal outstandingAmount,
    BigDecimal interestAmount,
    BigDecimal feesAmount,
    LocalDate dueDate,
    String status,
    String citizenStatus,
    String statusReasonCode,
    String interestAccrualState,
    String interestPauseReasonCode,
    String interestRuleCode,
    BigDecimal currentInterestRate,
    String writtenOffReasonCode) {}
