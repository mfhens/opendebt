package dk.ufst.opendebt.personregistry.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import dk.ufst.opendebt.personregistry.entity.PersonEntity;
import dk.ufst.opendebt.personregistry.entity.PersonEntity.IdentifierType;
import dk.ufst.opendebt.personregistry.entity.PersonEntity.PersonRole;

@Repository
public interface PersonRepository extends JpaRepository<PersonEntity, UUID> {

  /**
   * Find person by identifier hash and role. This is the primary lookup method - uses hash to find
   * without exposing PII.
   */
  Optional<PersonEntity> findByIdentifierHashAndRole(String identifierHash, PersonRole role);

  /**
   * Find all persons by identifier hash (may return multiple if same person has PERSONAL and
   * BUSINESS roles).
   */
  List<PersonEntity> findByIdentifierHash(String identifierHash);

  /** Find persons with pending deletion requests. */
  List<PersonEntity> findByDeletionRequestedAtIsNotNullAndDeletedAtIsNull();

  /** Find persons past their retention date. */
  @Query("SELECT p FROM PersonEntity p WHERE p.dataRetentionUntil < :date AND p.deletedAt IS NULL")
  List<PersonEntity> findByDataRetentionExpired(@Param("date") LocalDate date);

  /** Check if person exists by identifier hash and role. */
  boolean existsByIdentifierHashAndRole(String identifierHash, PersonRole role);

  /** Update last accessed timestamp and increment access count. */
  @Modifying
  @Query(
      "UPDATE PersonEntity p SET p.lastAccessedAt = CURRENT_TIMESTAMP, p.accessCount = p.accessCount + 1 WHERE p.id = :id")
  void recordAccess(@Param("id") UUID id);

  /** Count active (non-deleted) persons by identifier type. */
  @Query(
      "SELECT COUNT(p) FROM PersonEntity p WHERE p.identifierType = :type AND p.deletedAt IS NULL")
  long countActiveByIdentifierType(@Param("type") IdentifierType type);
}
