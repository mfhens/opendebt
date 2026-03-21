package dk.ufst.opendebt.caseservice.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.*;

import org.hibernate.annotations.CurrentTimestamp;
import org.hibernate.annotations.SourceType;
import org.hibernate.generator.EventType;

import lombok.*;

/** A journal entry (document registration) on a case, aligned with OIO Journalpost. */
@Entity
@Table(
    name = "case_journal_entries",
    indexes = {@Index(name = "idx_journal_entries_case_id", columnList = "case_id")})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CaseJournalEntryEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "case_id", nullable = false)
  private UUID caseId;

  @Column(name = "journal_entry_title", nullable = false, length = 200)
  private String journalEntryTitle;

  @Column(name = "journal_entry_time", nullable = false)
  private LocalDateTime journalEntryTime;

  @Column(name = "document_id")
  private UUID documentId;

  @Enumerated(EnumType.STRING)
  @Column(name = "document_direction", length = 20)
  private DocumentDirection documentDirection;

  @Column(name = "document_type", length = 100)
  private String documentType;

  @Column(name = "confidential_title", length = 200)
  private String confidentialTitle;

  @Column(name = "registered_by", length = 100)
  private String registeredBy;

  @CurrentTimestamp(event = EventType.INSERT, source = SourceType.VM)
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;
}
