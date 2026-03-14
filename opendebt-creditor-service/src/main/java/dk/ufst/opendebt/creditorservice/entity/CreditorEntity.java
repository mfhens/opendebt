package dk.ufst.opendebt.creditorservice.entity;

import java.util.UUID;

import jakarta.persistence.*;

import dk.ufst.opendebt.common.audit.AuditableEntity;

import lombok.*;

@Entity
@Table(
    name = "creditors",
    indexes = {
      @Index(name = "idx_creditor_org_id", columnList = "creditor_org_id"),
      @Index(name = "idx_external_creditor_id", columnList = "external_creditor_id", unique = true),
      @Index(name = "idx_parent_creditor_id", columnList = "parent_creditor_id"),
      @Index(name = "idx_creditor_type", columnList = "creditor_type"),
      @Index(name = "idx_activity_status", columnList = "activity_status"),
      @Index(name = "idx_dcs_status", columnList = "dcs_status")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreditorEntity extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  // Organization reference (FK to Person Registry)
  @Column(name = "creditor_org_id", nullable = false)
  private UUID creditorOrgId;

  // Core identity
  @Column(name = "external_creditor_id", nullable = false, length = 50, unique = true)
  private String externalCreditorId;

  @Column(name = "auto_created")
  @Builder.Default
  private Boolean autoCreated = false;

  @Column(name = "unique_link_id", length = 30)
  private String uniqueLinkId;

  @Column(name = "sorting_id", length = 8)
  private String sortingId;

  @Column(name = "captia_id", length = 15)
  private String captiaId;

  // Parent-child hierarchy
  @Column(name = "parent_creditor_id")
  private UUID parentCreditorId;

  @Column(name = "system_reporter_id", length = 50)
  private String systemReporterId;

  // Notification preferences
  @Column(name = "interest_notifications")
  @Builder.Default
  private Boolean interestNotifications = false;

  @Column(name = "detailed_interest_notifications")
  @Builder.Default
  private Boolean detailedInterestNotifications = false;

  @Column(name = "equalisation_notifications")
  @Builder.Default
  private Boolean equalisationNotifications = false;

  @Column(name = "allocation_notifications")
  @Builder.Default
  private Boolean allocationNotifications = false;

  @Column(name = "settlement_notifications")
  @Builder.Default
  private Boolean settlementNotifications = false;

  @Column(name = "return_notifications")
  @Builder.Default
  private Boolean returnNotifications = false;

  @Column(name = "write_off_notifications")
  @Builder.Default
  private Boolean writeOffNotifications = false;

  // Action permissions
  @Column(name = "allow_portal_actions")
  @Builder.Default
  private Boolean allowPortalActions = false;

  @Column(name = "allow_write_down")
  @Builder.Default
  private Boolean allowWriteDown = false;

  @Column(name = "allow_create_recovery_claims")
  @Builder.Default
  private Boolean allowCreateRecoveryClaims = false;

  @Column(name = "allow_create_offset_claims")
  @Builder.Default
  private Boolean allowCreateOffsetClaims = false;

  @Column(name = "allow_create_transports")
  @Builder.Default
  private Boolean allowCreateTransports = false;

  @Column(name = "allow_withdraw")
  @Builder.Default
  private Boolean allowWithdraw = false;

  @Column(name = "allow_write_up_reversed_write_down_adjustment")
  @Builder.Default
  private Boolean allowWriteUpReversedWriteDownAdjustment = false;

  @Column(name = "allow_write_up_cancelled_write_down_payment")
  @Builder.Default
  private Boolean allowWriteUpCancelledWriteDownPayment = false;

  @Column(name = "allow_write_down_cancelled_write_up_adjustment")
  @Builder.Default
  private Boolean allowWriteDownCancelledWriteUpAdjustment = false;

  @Column(name = "allow_write_down_cancelled_write_up_payment")
  @Builder.Default
  private Boolean allowWriteDownCancelledWriteUpPayment = false;

  @Column(name = "allow_incorrect_principal_report")
  @Builder.Default
  private Boolean allowIncorrectPrincipalReport = false;

  @Column(name = "allow_write_up_adjustment")
  @Builder.Default
  private Boolean allowWriteUpAdjustment = false;

  @Column(name = "allow_resubmit_claims")
  @Builder.Default
  private Boolean allowResubmitClaims = false;

  @Column(name = "allow_reject_offset_claims")
  @Builder.Default
  private Boolean allowRejectOffsetClaims = false;

  @Column(name = "allow_auto_cancel_hearing")
  @Builder.Default
  private Boolean allowAutoCancelHearing = false;

  @Column(name = "auto_cancel_hearing_days")
  private Integer autoCancelHearingDays;

  // Settlement configuration
  @Column(name = "currency_code", length = 3)
  @Builder.Default
  private String currencyCode = "DKK";

  @Column(name = "settlement_frequency", length = 20)
  @Builder.Default
  private String settlementFrequency = "MONTHLY";

  @Enumerated(EnumType.STRING)
  @Column(name = "settlement_method", length = 20)
  private SettlementMethod settlementMethod;

  @Column(name = "iban", length = 34)
  private String iban;

  @Column(name = "swift_code", length = 11)
  private String swiftCode;

  @Column(name = "danish_account_number", length = 14)
  private String danishAccountNumber;

  // Classification
  @Column(name = "business_unit", length = 10)
  private String businessUnit;

  @Enumerated(EnumType.STRING)
  @Column(name = "creditor_type", length = 30)
  private CreditorType creditorType;

  @Enumerated(EnumType.STRING)
  @Column(name = "payment_type", length = 30)
  private PaymentType paymentType;

  @Enumerated(EnumType.STRING)
  @Column(name = "adjustment_type_profile", length = 10)
  private AdjustmentTypeProfile adjustmentTypeProfile;

  @Column(name = "uses_dmi")
  @Builder.Default
  private Boolean usesDmi = false;

  // Status and lifecycle
  @Enumerated(EnumType.STRING)
  @Column(name = "dcs_status", length = 30)
  private DcsStatus dcsStatus;

  @Column(name = "system_agreement_active")
  @Builder.Default
  private Boolean systemAgreementActive = false;

  @Enumerated(EnumType.STRING)
  @Column(name = "activity_status", length = 40)
  @Builder.Default
  private ActivityStatus activityStatus = ActivityStatus.ACTIVE;

  @Enumerated(EnumType.STRING)
  @Column(name = "connection_type", length = 20)
  private ConnectionType connectionType;

  @Column(name = "ip_whitelisted")
  @Builder.Default
  private Boolean ipWhitelisted = false;

  // Audit fields inherited from AuditableEntity
}
