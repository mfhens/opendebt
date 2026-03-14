package dk.ufst.opendebt.creditorservice.dto;

import java.util.*;

import dk.ufst.opendebt.creditorservice.entity.*;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditorDto {

  private UUID id;

  private UUID creditorOrgId;
  private String externalCreditorId;
  private UUID parentCreditorId;
  private CreditorType creditorType;
  private PaymentType paymentType;
  private AdjustmentTypeProfile adjustmentTypeProfile;
  private DcsStatus dcsStatus;
  private ActivityStatus activityStatus;
  private ConnectionType connectionType;
  private Boolean usesDmi;
  private Boolean systemAgreementActive;
  private Boolean ipWhitelisted;
}
