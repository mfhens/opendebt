package dk.ufst.opendebt.caseservice.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.*;

import org.hibernate.annotations.CreationTimestamp;

import lombok.*;

/** Legal basis (law/regulation reference) for a case. */
@Entity
@Table(
    name = "case_legal_bases",
    indexes = {@Index(name = "idx_legal_bases_case_id", columnList = "case_id")})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CaseLegalBasisEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "case_id", nullable = false)
  private UUID caseId;

  @Column(name = "legal_source_uri", length = 500)
  private String legalSourceUri;

  @Column(name = "legal_source_title", nullable = false, length = 300)
  private String legalSourceTitle;

  @Column(name = "paragraph_reference", length = 100)
  private String paragraphReference;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;
}
