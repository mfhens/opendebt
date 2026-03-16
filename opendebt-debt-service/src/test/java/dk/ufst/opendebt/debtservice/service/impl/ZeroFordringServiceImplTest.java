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
import dk.ufst.opendebt.debtservice.entity.DebtEntity;
import dk.ufst.opendebt.debtservice.entity.FordringKategori;
import dk.ufst.opendebt.debtservice.entity.FordringsartEnum;
import dk.ufst.opendebt.debtservice.repository.DebtRepository;

@ExtendWith(MockitoExtension.class)
class ZeroFordringServiceImplTest {

  @Mock private DebtRepository debtRepository;

  private ZeroFordringServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new ZeroFordringServiceImpl(debtRepository);
  }

  // =========================================================================
  // validateZeroFordring
  // =========================================================================

  @Test
  void validateZeroFordring_validHF_success() {
    DebtEntity entity = zeroHfEntity();

    assertThatCode(() -> service.validateZeroFordring(entity)).doesNotThrowAnyException();
  }

  @Test
  void validateZeroFordring_ufWithZeroPrincipal_throws() {
    DebtEntity entity = zeroHfEntity();
    entity.setFordringKategori(FordringKategori.UF);

    assertThatThrownBy(() -> service.validateZeroFordring(entity))
        .isInstanceOf(OpenDebtException.class)
        .satisfies(
            ex -> {
              OpenDebtException ode = (OpenDebtException) ex;
              assertThatCode(() -> ode.getErrorCode()).doesNotThrowAnyException();
              assert ode.getErrorCode().equals("ZERO_PRINCIPAL_NOT_ALLOWED_FOR_UF");
            })
        .hasMessageContaining("Underfordring cannot have zero principal");
  }

  @Test
  void validateZeroFordring_hfWithPositivePrincipal_noOp() {
    DebtEntity entity = zeroHfEntity();
    entity.setPrincipalAmount(new BigDecimal("1000"));

    // Should be a no-op for positive principal — no exception
    assertThatCode(() -> service.validateZeroFordring(entity)).doesNotThrowAnyException();
  }

  @Test
  void validateZeroFordring_missingStamdata_throws() {
    DebtEntity entity = zeroHfEntity();
    entity.setForaeldelsesdato(null); // Remove required stamdata

    assertThatThrownBy(() -> service.validateZeroFordring(entity))
        .isInstanceOf(OpenDebtException.class)
        .satisfies(
            ex -> {
              OpenDebtException ode = (OpenDebtException) ex;
              assert ode.getErrorCode().equals("ZERO_FORDRING_MISSING_STAMDATA");
            })
        .hasMessageContaining("foraeldelsesdato");
  }

  // =========================================================================
  // validateUnderfordringReference
  // =========================================================================

  @Test
  void validateUnderfordring_validReference_success() {
    UUID hovedfordringsId = UUID.randomUUID();
    DebtEntity underfordring = ufEntity(hovedfordringsId);
    DebtEntity hovedfordring = hfEntityWithPrincipal(hovedfordringsId, new BigDecimal("5000"));

    when(debtRepository.findById(hovedfordringsId)).thenReturn(Optional.of(hovedfordring));

    assertThatCode(() -> service.validateUnderfordringReference(underfordring))
        .doesNotThrowAnyException();
  }

  @Test
  void validateUnderfordring_referencesZeroFordring_success() {
    UUID hovedfordringsId = UUID.randomUUID();
    DebtEntity underfordring = ufEntity(hovedfordringsId);
    DebtEntity hovedfordring = hfEntityWithPrincipal(hovedfordringsId, BigDecimal.ZERO);

    when(debtRepository.findById(hovedfordringsId)).thenReturn(Optional.of(hovedfordring));

    assertThatCode(() -> service.validateUnderfordringReference(underfordring))
        .doesNotThrowAnyException();
  }

  @Test
  void validateUnderfordring_nullHovedfordringsId_throws() {
    DebtEntity underfordring = ufEntity(null);

    assertThatThrownBy(() -> service.validateUnderfordringReference(underfordring))
        .isInstanceOf(OpenDebtException.class)
        .satisfies(
            ex -> {
              OpenDebtException ode = (OpenDebtException) ex;
              assert ode.getErrorCode().equals("HOVEDFORDRINGS_ID_REQUIRED");
            })
        .hasMessageContaining("hovedfordringsId");
  }

  @Test
  void validateUnderfordring_hovedfordringNotFound_throws() {
    UUID hovedfordringsId = UUID.randomUUID();
    DebtEntity underfordring = ufEntity(hovedfordringsId);

    when(debtRepository.findById(hovedfordringsId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.validateUnderfordringReference(underfordring))
        .isInstanceOf(OpenDebtException.class)
        .satisfies(
            ex -> {
              OpenDebtException ode = (OpenDebtException) ex;
              assert ode.getErrorCode().equals("HOVEDFORDRING_NOT_FOUND");
            })
        .hasMessageContaining("hovedfordring not found");
  }

  @Test
  void validateUnderfordring_hfDoesNotNeedReference() {
    DebtEntity hf =
        DebtEntity.builder()
            .id(UUID.randomUUID())
            .fordringKategori(FordringKategori.HF)
            .principalAmount(new BigDecimal("1000"))
            .hovedfordringsId(null)
            .build();

    // HF should not trigger reference validation — no exception
    assertThatCode(() -> service.validateUnderfordringReference(hf)).doesNotThrowAnyException();
  }

  // =========================================================================
  // Helpers
  // =========================================================================

  /** Creates a valid 0-fordring HF entity with all required stamdata. */
  private DebtEntity zeroHfEntity() {
    return DebtEntity.builder()
        .id(UUID.randomUUID())
        .debtorPersonId(UUID.randomUUID())
        .creditorOrgId(UUID.randomUUID())
        .debtTypeCode("600")
        .principalAmount(BigDecimal.ZERO)
        .fordringKategori(FordringKategori.HF)
        .fordringsart(FordringsartEnum.INDR)
        .foraeldelsesdato(LocalDate.of(2030, 12, 31))
        .dueDate(LocalDate.of(2026, 6, 1))
        .status(DebtEntity.DebtStatus.ACTIVE)
        .readinessStatus(DebtEntity.ReadinessStatus.PENDING_REVIEW)
        .build();
  }

  /** Creates an underfordring (UF) referencing a hovedfordring. */
  private DebtEntity ufEntity(UUID hovedfordringsId) {
    return DebtEntity.builder()
        .id(UUID.randomUUID())
        .debtorPersonId(UUID.randomUUID())
        .creditorOrgId(UUID.randomUUID())
        .debtTypeCode("600")
        .principalAmount(new BigDecimal("150"))
        .fordringKategori(FordringKategori.UF)
        .hovedfordringsId(hovedfordringsId)
        .dueDate(LocalDate.of(2026, 6, 1))
        .status(DebtEntity.DebtStatus.ACTIVE)
        .readinessStatus(DebtEntity.ReadinessStatus.PENDING_REVIEW)
        .build();
  }

  /** Creates a hovedfordring (HF) with a specific principal amount. */
  private DebtEntity hfEntityWithPrincipal(UUID id, BigDecimal principalAmount) {
    return DebtEntity.builder()
        .id(id)
        .debtorPersonId(UUID.randomUUID())
        .creditorOrgId(UUID.randomUUID())
        .debtTypeCode("600")
        .principalAmount(principalAmount)
        .fordringKategori(FordringKategori.HF)
        .fordringsart(FordringsartEnum.INDR)
        .foraeldelsesdato(LocalDate.of(2030, 12, 31))
        .dueDate(LocalDate.of(2026, 6, 1))
        .status(DebtEntity.DebtStatus.ACTIVE)
        .readinessStatus(DebtEntity.ReadinessStatus.PENDING_REVIEW)
        .build();
  }
}
