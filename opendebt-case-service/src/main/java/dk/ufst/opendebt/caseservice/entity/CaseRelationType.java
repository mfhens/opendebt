package dk.ufst.opendebt.caseservice.entity;

/** Type of relationship between two cases. */
public enum CaseRelationType {
  PARENT,
  RELATED,
  PRECEDENT,
  SPLIT_FROM,
  MERGED_INTO
}
