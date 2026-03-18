package dk.ufst.opendebt.caseservice.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.*;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import lombok.*;

/** Free-text note on a case, replacing the old flat notes field. */
@Entity
@Table(
    name = "case_journal_notes",
    indexes = {@Index(name = "idx_journal_notes_case_id", columnList = "case_id")})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CaseJournalNoteEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "case_id", nullable = false)
  private UUID caseId;

  @Column(name = "note_title", nullable = false, length = 200)
  private String noteTitle;

  @Column(name = "note_text", nullable = false, columnDefinition = "TEXT")
  private String noteText;

  @Column(name = "author_id", nullable = false, length = 100)
  private String authorId;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;
}
