package dk.ufst.opendebt.debtservice.attachment;

public enum AttachmentWorkflowOutcomeQualifier {
  NONE,
  COMPLETED,
  NO_ATTACHABLE_ASSETS,
  INSOLVENCY_DECLARED,
  LEGAL_OR_PROCEDURAL_DEFECT,
  THIRD_PARTY_RIGHT_BLOCK,
  COURT_REJECTION,
  WITHDRAWN
}
