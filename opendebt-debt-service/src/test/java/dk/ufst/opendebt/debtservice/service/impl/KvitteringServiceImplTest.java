package dk.ufst.opendebt.debtservice.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dk.ufst.opendebt.debtservice.dto.KvitteringResponse;
import dk.ufst.opendebt.debtservice.dto.SlutstatusEnum;
import dk.ufst.opendebt.debtservice.entity.ClaimLifecycleState;
import dk.ufst.opendebt.debtservice.entity.DebtEntity;
import dk.ufst.opendebt.debtservice.entity.HoeringEntity;
import dk.ufst.opendebt.debtservice.entity.HoeringStatus;

class KvitteringServiceImplTest {

  private KvitteringServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new KvitteringServiceImpl();
  }

  @Test
  void buildKvittering_udfoert() {
    UUID debtId = UUID.randomUUID();
    DebtEntity entity = testDebt(debtId);

    KvitteringResponse result =
        service.buildKvittering(debtId, entity, Collections.emptyList(), null);

    assertThat(result.getFordringsId()).isEqualTo(debtId);
    assertThat(result.getSlutstatus()).isEqualTo(SlutstatusEnum.UDFOERT);
    assertThat(result.getAfvistBegrundelse()).isNull();
    assertThat(result.getAfvistErrorCode()).isNull();
    assertThat(result.getHoeringInfo()).isNull();
  }

  @Test
  void buildKvittering_afvist() {
    UUID debtId = UUID.randomUUID();
    DebtEntity entity = testDebt(debtId);
    List<String> errors = List.of("Hovedstol mangler", "Forfaldsdato ugyldig");

    KvitteringResponse result = service.buildKvittering(debtId, entity, errors, null);

    assertThat(result.getFordringsId()).isEqualTo(debtId);
    assertThat(result.getSlutstatus()).isEqualTo(SlutstatusEnum.AFVIST);
    assertThat(result.getAfvistBegrundelse()).isEqualTo("Hovedstol mangler");
    assertThat(result.getHoeringInfo()).isNull();
  }

  @Test
  void buildKvittering_hoering() {
    UUID debtId = UUID.randomUUID();
    UUID hoeringId = UUID.randomUUID();
    DebtEntity entity = testDebt(debtId);
    LocalDateTime slaDeadline = LocalDateTime.now().plusDays(14);

    HoeringEntity hoering =
        HoeringEntity.builder()
            .id(hoeringId)
            .debtId(debtId)
            .hoeringStatus(HoeringStatus.AFVENTER_FORDRINGSHAVER)
            .deviationDescription("Hovedstol afviger fra stamdata")
            .slaDeadline(slaDeadline)
            .build();

    KvitteringResponse result =
        service.buildKvittering(debtId, entity, Collections.emptyList(), hoering);

    assertThat(result.getFordringsId()).isEqualTo(debtId);
    assertThat(result.getSlutstatus()).isEqualTo(SlutstatusEnum.HOERING);
    assertThat(result.getHoeringInfo()).isNotNull();
    assertThat(result.getHoeringInfo().getHoeringId()).isEqualTo(hoeringId);
    assertThat(result.getHoeringInfo().getDeviationDescription())
        .isEqualTo("Hovedstol afviger fra stamdata");
    assertThat(result.getHoeringInfo().getSlaDeadline()).isEqualTo(slaDeadline);
    assertThat(result.getAfvistBegrundelse()).isNull();
  }

  // =========================================================================
  // Helpers
  // =========================================================================

  private DebtEntity testDebt(UUID debtId) {
    return DebtEntity.builder()
        .id(debtId)
        .debtorPersonId(UUID.randomUUID())
        .creditorOrgId(UUID.randomUUID())
        .debtTypeCode("600")
        .principalAmount(new BigDecimal("10000"))
        .dueDate(LocalDate.of(2026, 6, 1))
        .lifecycleState(ClaimLifecycleState.REGISTERED)
        .status(DebtEntity.DebtStatus.PENDING)
        .readinessStatus(DebtEntity.ReadinessStatus.PENDING_REVIEW)
        .build();
  }
}
