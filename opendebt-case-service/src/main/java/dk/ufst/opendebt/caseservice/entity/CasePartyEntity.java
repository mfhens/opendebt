package dk.ufst.opendebt.caseservice.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.*;

import org.hibernate.annotations.CreationTimestamp;

import lombok.*;

/** A party (person or organisation) associated with a case. */
@Entity
@Table(
    name = "case_parties",
    indexes = {
      @Index(name = "idx_case_parties_case_id", columnList = "case_id"),
      @Index(name = "idx_case_parties_person_id", columnList = "person_id")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CasePartyEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "case_id", nullable = false)
  private UUID caseId;

  @Column(name = "person_id", nullable = false)
  private UUID personId;

  @Enumerated(EnumType.STRING)
  @Column(name = "party_role", nullable = false, length = 30)
  private PartyRole partyRole;

  @Enumerated(EnumType.STRING)
  @Column(name = "party_type", nullable = false, length = 20)
  private PartyType partyType;

  @Column(name = "active_from")
  private LocalDate activeFrom;

  @Column(name = "active_to")
  private LocalDate activeTo;

  @Column(name = "added_by", length = 100)
  private String addedBy;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;
}
