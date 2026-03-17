package dk.ufst.opendebt.debtservice.service.impl;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dk.ufst.opendebt.common.exception.OpenDebtException;
import dk.ufst.opendebt.debtservice.entity.ClaimArtEnum;
import dk.ufst.opendebt.debtservice.entity.ClaimCategory;
import dk.ufst.opendebt.debtservice.entity.DebtEntity;
import dk.ufst.opendebt.debtservice.repository.DebtRepository;

@ExtendWith(MockitoExtension.class)
class ZeroClaimServiceImplTest {

  @Mock private DebtRepository debtRepository;

  private ZeroClaimServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new ZeroClaimServiceImpl(debtRepository);
  }

  // =========================================================================
  // validateZeroClaim
  // =========================================================================

  @Test
  void validateZeroClaim_validHF_success() {
    DebtEntity entity = zeroHfEntity();

    assertThatCode(() -> service.validateZeroClaim(entity)).doesNotThrowAnyException();
  }

  @Test
  void validateZeroClaim_ufWithZeroPrincipal_throws() {
    DebtEntity entity = zeroHfEntity();
    entity.setClaimCategory(ClaimCategory.UF);

    assertThatThrownBy(() -> service.validateZeroClaim(entity))
        .isInstanceOf(OpenDebtException.class)
        .satisfies(
            ex -> {
              OpenDebtException ode = (OpenDebtException) ex;
              assertThatCode(() -> ode.getErrorCode()).doesNotThrowAnyException();
              assert ode.getErrorCode().equals("ZERO_PRINCIPAL_NOT_ALLOWED_FOR_UF");
            })
        .hasMessageContaining("Sub-claim cannot have zero principal");
  }

  @Test
  void validateZeroClaim_hfWithPositivePrincipal_noOp() {
    DebtEntity entity = zeroHfEntity();
    entity.setPrincipalAmount(new BigDecimal("1000"));

    // Should be a no-op for positive principal — no exception
    assertThatCode(() -> service.validateZeroClaim(entity)).doesNotThrowAnyException();
  }

  @Test
  void validateZeroClaim_missingStamdata_throws() {
    DebtEntity entity = zeroHfEntity();
    entity.setLimitationDate(null); // Remove required stamdata

    assertThatThrownBy(() -> service.validateZeroClaim(entity))
        .isInstanceOf(OpenDebtException.class)
        .satisfies(
            ex -> {
              OpenDebtException ode = (OpenDebtException) ex;
              assert ode.getErrorCode().equals("ZERO_CLAIM_MISSING_STAMDATA");
            })
        .hasMessageContaining("limitationDate");
  }

  // =========================================================================
  // validateSubClaimReference
  // =========================================================================

  @Test
  void validateSubClaim_validReference_success() {
    UUID parentClaimId = UUID.randomUUID();
    DebtEntity subClaim = ufEntity(parentClaimId);
    DebtEntity parentClaim = hfEntityWithPrincipal(parentClaimId, new BigDecimal("5000"));

    when(debtRepository.findById(parentClaimId)).thenReturn(Optional.of(parentClaim));

    assertThatCode(() -> service.validateSubClaimReference(subClaim)).doesNotThrowAnyException();
  }

  @Test
  void validateSubClaim_referencesZeroClaim_success() {
    UUID parentClaimId = UUID.randomUUID();
    DebtEntity subClaim = ufEntity(parentClaimId);
    DebtEntity parentClaim = hfEntityWithPrincipal(parentClaimId, BigDecimal.ZERO);

    when(debtRepository.findById(parentClaimId)).thenReturn(Optional.of(parentClaim));

    assertThatCode(() -> service.validateSubClaimReference(subClaim)).doesNotThrowAnyException();
  }

  @Test
  void validateSubClaim_nullParentClaimId_throws() {
    DebtEntity subClaim = ufEntity(null);

    assertThatThrownBy(() -> service.validateSubClaimReference(subClaim))
        .isInstanceOf(OpenDebtException.class)
        .satisfies(
            ex -> {
              OpenDebtException ode = (OpenDebtException) ex;
              assert ode.getErrorCode().equals("PARENT_CLAIM_ID_REQUIRED");
            })
        .hasMessageContaining("parentClaimId");
  }

  @Test
  void validateSubClaim_parentClaimNotFound_throws() {
    UUID parentClaimId = UUID.randomUUID();
    DebtEntity subClaim = ufEntity(parentClaimId);

    when(debtRepository.findById(parentClaimId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.validateSubClaimReference(subClaim))
        .isInstanceOf(OpenDebtException.class)
        .satisfies(
            ex -> {
              OpenDebtException ode = (OpenDebtException) ex;
              assert ode.getErrorCode().equals("PARENT_CLAIM_NOT_FOUND");
            })
        .hasMessageContaining("main claim not found");
  }

  @Test
  void validateSubClaim_hfDoesNotNeedReference() {
    DebtEntity hf =
        DebtEntity.builder()
            .id(UUID.randomUUID())
            .claimCategory(ClaimCategory.HF)
            .principalAmount(new BigDecimal("1000"))
            .parentClaimId(null)
            .build();

    // HF should not trigger reference validation — no exception
    assertThatCode(() -> service.validateSubClaimReference(hf)).doesNotThrowAnyException();
  }

  // =========================================================================
  // Helpers
  // =========================================================================

  /** Creates a valid zero-principal claim HF entity with all required stamdata. */
  private DebtEntity zeroHfEntity() {
    return DebtEntity.builder()
        .id(UUID.randomUUID())
        .debtorPersonId(UUID.randomUUID())
        .creditorOrgId(UUID.randomUUID())
        .debtTypeCode("600")
        .principalAmount(BigDecimal.ZERO)
        .claimCategory(ClaimCategory.HF)
        .claimArt(ClaimArtEnum.INDR)
        .limitationDate(LocalDate.of(2030, 12, 31))
        .dueDate(LocalDate.of(2026, 6, 1))
        .status(DebtEntity.DebtStatus.ACTIVE)
        .readinessStatus(DebtEntity.ReadinessStatus.PENDING_REVIEW)
        .build();
  }

  /** Creates a sub-claim (UF) referencing a parent claim. */
  private DebtEntity ufEntity(UUID parentClaimId) {
    return DebtEntity.builder()
        .id(UUID.randomUUID())
        .debtorPersonId(UUID.randomUUID())
        .creditorOrgId(UUID.randomUUID())
        .debtTypeCode("600")
        .principalAmount(new BigDecimal("150"))
        .claimCategory(ClaimCategory.UF)
        .parentClaimId(parentClaimId)
        .dueDate(LocalDate.of(2026, 6, 1))
        .status(DebtEntity.DebtStatus.ACTIVE)
        .readinessStatus(DebtEntity.ReadinessStatus.PENDING_REVIEW)
        .build();
  }

  /** Creates a main claim (HF) with a specific principal amount. */
  private DebtEntity hfEntityWithPrincipal(UUID id, BigDecimal principalAmount) {
    return DebtEntity.builder()
        .id(id)
        .debtorPersonId(UUID.randomUUID())
        .creditorOrgId(UUID.randomUUID())
        .debtTypeCode("600")
        .principalAmount(principalAmount)
        .claimCategory(ClaimCategory.HF)
        .claimArt(ClaimArtEnum.INDR)
        .limitationDate(LocalDate.of(2030, 12, 31))
        .dueDate(LocalDate.of(2026, 6, 1))
        .status(DebtEntity.DebtStatus.ACTIVE)
        .readinessStatus(DebtEntity.ReadinessStatus.PENDING_REVIEW)
        .build();
  }
}
