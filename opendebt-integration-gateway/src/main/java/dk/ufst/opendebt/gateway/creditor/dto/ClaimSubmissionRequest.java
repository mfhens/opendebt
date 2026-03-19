package dk.ufst.opendebt.gateway.creditor.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimSubmissionRequest {

  @NotBlank private String debtorId;
  @NotBlank private String creditorId;
  @NotBlank private String debtTypeCode;
  @NotNull @Positive private BigDecimal principalAmount;
  private BigDecimal interestAmount;
  private BigDecimal feesAmount;
  @NotNull private LocalDate dueDate;
  private LocalDate originalDueDate;
  private String externalReference;
  private String ocrLine;
  private String claimArt;
  private String claimCategory;
  private String creditorReference;
  private String description;
  private LocalDate limitationDate;
  private LocalDate periodFrom;
  private LocalDate periodTo;
  private LocalDate inceptionDate;
  private LocalDate paymentDeadline;
  private LocalDate lastPaymentDate;
  private Boolean estateProcessing;
  private LocalDate judgmentDate;
  private LocalDate settlementDate;
  private String interestRule;
  private String interestRateCode;
  private BigDecimal additionalInterestRate;
  private String claimNote;
  private String customerNote;
}
