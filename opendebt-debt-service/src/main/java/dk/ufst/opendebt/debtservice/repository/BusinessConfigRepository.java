package dk.ufst.opendebt.debtservice.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import dk.ufst.opendebt.debtservice.entity.BusinessConfigEntity;

@Repository
public interface BusinessConfigRepository extends JpaRepository<BusinessConfigEntity, UUID> {

  /**
   * Resolves the config value effective on the given date. Returns the row where valid_from <=
   * effectiveDate and (valid_to IS NULL OR valid_to > effectiveDate).
   */
  @Query(
      "SELECT c FROM BusinessConfigEntity c "
          + "WHERE c.configKey = :key "
          + "AND c.validFrom <= :effectiveDate "
          + "AND (c.validTo IS NULL OR c.validTo > :effectiveDate) "
          + "ORDER BY c.validFrom DESC")
  List<BusinessConfigEntity> findEffective(
      @Param("key") String configKey, @Param("effectiveDate") LocalDate effectiveDate);

  /** Returns the full version history for a config key, ordered by valid_from descending. */
  List<BusinessConfigEntity> findByConfigKeyOrderByValidFromDesc(String configKey);

  /** Checks for any existing entry for the same key that overlaps the given period. */
  @Query(
      "SELECT c FROM BusinessConfigEntity c WHERE c.configKey = :key "
          + "AND c.validFrom < :newValidTo "
          + "AND (c.validTo IS NULL OR c.validTo > :newValidFrom)")
  List<BusinessConfigEntity> findOverlapping(
      @Param("key") String configKey,
      @Param("newValidFrom") LocalDate newValidFrom,
      @Param("newValidTo") LocalDate newValidTo);

  /** Finds the current open-ended entry for a key (validTo IS NULL). */
  @Query(
      "SELECT c FROM BusinessConfigEntity c WHERE c.configKey = :key AND c.validTo IS NULL ORDER BY c.validFrom DESC")
  List<BusinessConfigEntity> findOpenEnded(@Param("key") String configKey);

  /** Returns all distinct config keys. */
  @Query("SELECT DISTINCT c.configKey FROM BusinessConfigEntity c ORDER BY c.configKey")
  List<String> findAllDistinctKeys();
}
