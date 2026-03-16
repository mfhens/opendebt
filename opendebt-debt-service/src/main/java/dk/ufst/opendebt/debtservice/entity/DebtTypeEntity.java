package dk.ufst.opendebt.debtservice.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import lombok.*;

@Entity
@Table(name = "debt_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DebtTypeEntity {

  @Id
  @Column(name = "code", length = 20)
  private String code;

  @Column(name = "name", nullable = false, length = 200)
  private String name;

  @Column(name = "category", length = 50)
  private String category;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Column(name = "legal_basis", length = 500)
  private String legalBasis;

  @Column(name = "active", nullable = false)
  @Builder.Default
  private boolean active = true;

  @Column(name = "requires_manual_review")
  @Builder.Default
  private boolean requiresManualReview = false;

  @Column(name = "interest_applicable")
  @Builder.Default
  private boolean interestApplicable = true;

  @Column(name = "civilretlig")
  @Builder.Default
  private boolean civilretlig = false;

  @Column(name = "fordringstype_kode", length = 20)
  private String fordringstypeKode;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;
}
