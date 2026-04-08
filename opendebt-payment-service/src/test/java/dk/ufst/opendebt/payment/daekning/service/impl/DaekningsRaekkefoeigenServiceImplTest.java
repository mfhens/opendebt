package dk.ufst.opendebt.payment.daekning.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dk.ufst.opendebt.payment.daekning.InddrivelsesindsatsType;
import dk.ufst.opendebt.payment.daekning.PrioritetKategori;
import dk.ufst.opendebt.payment.daekning.dto.DaekningsraekkefoelgePositionDto;
import dk.ufst.opendebt.payment.daekning.dto.SimulatePositionDto;
import dk.ufst.opendebt.payment.daekning.entity.DaekningFordringEntity;
import dk.ufst.opendebt.payment.daekning.entity.DaekningRecord;
import dk.ufst.opendebt.payment.daekning.repository.DaekningFordringRepository;
import dk.ufst.opendebt.payment.daekning.repository.DaekningRecordRepository;

/**
 * Unit tests for {@link DaekningsRaekkefoeigenServiceImpl}.
 *
 * <p>Tests exercise the public API (apply / simulate / getOrdering) to cover all internal helper
 * paths without exposing private methods.
 */
@ExtendWith(MockitoExtension.class)
class DaekningsRaekkefoeigenServiceImplTest {

  private static final String DEBTOR = "debtor-001";
  private static final Instant NOW = Instant.parse("2026-04-01T10:00:00Z");
  private static final Clock FIXED_CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

  // Use named constants so tests read clearly without full enum names inline
  private static final PrioritetKategori HIGH_PRIORITY = PrioritetKategori.RIMELIGE_OMKOSTNINGER;
  private static final PrioritetKategori LOW_PRIORITY = PrioritetKategori.ANDRE_FORDRINGER;

  @Mock private DaekningFordringRepository fordringRepo;
  @Mock private DaekningRecordRepository recordRepo;

  private DaekningsRaekkefoeigenServiceImpl svc;

  @BeforeEach
  void setUp() {
    svc = new DaekningsRaekkefoeigenServiceImpl(fordringRepo, recordRepo, FIXED_CLOCK);
    org.mockito.Mockito.lenient()
        .when(recordRepo.saveAll(org.mockito.ArgumentMatchers.anyList()))
        .thenAnswer(inv -> inv.getArgument(0));
  }

  private DaekningFordringEntity fordring(
      String id, PrioritetKategori kat, String beloeb, boolean inLoen, boolean inUdlaeg) {
    return DaekningFordringEntity.builder()
        .fordringId(id)
        .debtorId(DEBTOR)
        .prioritetKategori(kat)
        .tilbaestaaendeBeloeb(new BigDecimal(beloeb))
        .modtagelsesdato(LocalDate.of(2025, 1, 1))
        .receivedAt(NOW.minusSeconds(60))
        .inLoenindeholdelsesIndsats(inLoen)
        .inUdlaegForretning(inUdlaeg)
        .build();
  }

  @Nested
  @DisplayName("apply() — FRIVILLIG")
  class ApplyFrivillig {

    @Test
    @DisplayName("single fordring fully covered")
    void singleFordringFullCoverage() {
      DaekningFordringEntity f = fordring("F1", LOW_PRIORITY, "300.00", false, false);
      when(fordringRepo.findByDebtorId(DEBTOR)).thenReturn(List.of(f));

      List<DaekningRecord> records =
          svc.apply(DEBTOR, new BigDecimal("300.00"), InddrivelsesindsatsType.FRIVILLIG, NOW, NOW);

      assertThat(records).hasSize(1);
      assertThat(records.get(0).getFordringId()).isEqualTo("F1");
      assertThat(records.get(0).getDaekningBeloeb()).isEqualByComparingTo("300.00");
    }

    @Test
    @DisplayName("high-priority fordring covered before low-priority")
    void twoFordringerPriorityOrder() {
      DaekningFordringEntity fLow = fordring("F_LOW", LOW_PRIORITY, "500.00", false, false);
      DaekningFordringEntity fHigh = fordring("F_HIGH", HIGH_PRIORITY, "500.00", false, false);
      when(fordringRepo.findByDebtorId(DEBTOR)).thenReturn(List.of(fLow, fHigh));

      List<DaekningRecord> records =
          svc.apply(DEBTOR, new BigDecimal("600.00"), InddrivelsesindsatsType.FRIVILLIG, NOW, NOW);

      assertThat(records).hasSize(2);
      assertThat(records.get(0).getFordringId()).isEqualTo("F_HIGH");
      assertThat(records.get(0).getDaekningBeloeb()).isEqualByComparingTo("500.00");
      assertThat(records.get(1).getDaekningBeloeb()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("partial coverage — stops when amount exhausted")
    void partialCoverage() {
      DaekningFordringEntity f = fordring("F1", LOW_PRIORITY, "1000.00", false, false);
      when(fordringRepo.findByDebtorId(DEBTOR)).thenReturn(List.of(f));

      List<DaekningRecord> records =
          svc.apply(DEBTOR, new BigDecimal("250.00"), InddrivelsesindsatsType.FRIVILLIG, NOW, NOW);

      assertThat(records).hasSize(1);
      assertThat(records.get(0).getDaekningBeloeb()).isEqualByComparingTo("250.00");
    }
  }

  @Nested
  @DisplayName("apply() — LOENINDEHOLDELSE")
  class ApplyLoenindeholdelse {

    @Test
    @DisplayName("indsats fordringer covered before non-indsats surplus fordringer")
    void indsatsCoveredFirst() {
      DaekningFordringEntity surplus = fordring("SURPLUS", LOW_PRIORITY, "200.00", false, false);
      DaekningFordringEntity indsats = fordring("INDSATS", LOW_PRIORITY, "200.00", true, false);
      when(fordringRepo.findByDebtorId(DEBTOR)).thenReturn(List.of(surplus, indsats));

      List<DaekningRecord> records =
          svc.apply(
              DEBTOR, new BigDecimal("250.00"), InddrivelsesindsatsType.LOENINDEHOLDELSE, NOW, NOW);

      assertThat(records.get(0).getFordringId()).isEqualTo("INDSATS");
      assertThat(records.get(0).getDaekningBeloeb()).isEqualByComparingTo("200.00");
      assertThat(records.get(1).getFordringId()).isEqualTo("SURPLUS");
      assertThat(records.get(1).getDaekningBeloeb()).isEqualByComparingTo("50.00");
    }
  }

  @Nested
  @DisplayName("apply() — UDLAEG")
  class ApplyUdlaeg {

    @Test
    @DisplayName("non-udlaeg fordringer are skipped")
    void onlyUdlaegFordringer() {
      DaekningFordringEntity nonUdlaeg = fordring("NON", LOW_PRIORITY, "500.00", false, false);
      DaekningFordringEntity udlaeg = fordring("UDLAEG", LOW_PRIORITY, "300.00", false, true);
      when(fordringRepo.findByDebtorId(DEBTOR)).thenReturn(List.of(nonUdlaeg, udlaeg));

      List<DaekningRecord> records =
          svc.apply(DEBTOR, new BigDecimal("200.00"), InddrivelsesindsatsType.UDLAEG, NOW, NOW);

      assertThat(records).hasSize(1);
      assertThat(records.get(0).getFordringId()).isEqualTo("UDLAEG");
      assertThat(Boolean.TRUE.equals(records.get(0).getUdlaegSurplus())).isFalse();
    }

    @Test
    @DisplayName("surplus after all udlaeg fordringer exhausted — marked as udlaegSurplus")
    void surplusRecordedWhenUdlaegExhausted() {
      DaekningFordringEntity udlaeg = fordring("UDLAEG", LOW_PRIORITY, "100.00", false, true);
      when(fordringRepo.findByDebtorId(DEBTOR)).thenReturn(List.of(udlaeg));

      List<DaekningRecord> records =
          svc.apply(DEBTOR, new BigDecimal("250.00"), InddrivelsesindsatsType.UDLAEG, NOW, NOW);

      assertThat(records).hasSize(2);
      DaekningRecord surplus =
          records.stream()
              .filter(r -> Boolean.TRUE.equals(r.getUdlaegSurplus()))
              .findFirst()
              .orElseThrow();
      assertThat(surplus.getDaekningBeloeb()).isEqualByComparingTo("150.00");
    }
  }

  @Nested
  @DisplayName("simulate()")
  class Simulate {

    @Test
    @DisplayName("returns positions without persisting records")
    void returnsPositions() {
      DaekningFordringEntity f = fordring("F1", LOW_PRIORITY, "500.00", false, false);
      when(fordringRepo.findByDebtorId(DEBTOR)).thenReturn(List.of(f));

      List<SimulatePositionDto> positions =
          svc.simulate(DEBTOR, new BigDecimal("300.00"), InddrivelsesindsatsType.FRIVILLIG, NOW);

      assertThat(positions).hasSize(1);
      assertThat(positions.get(0).fordringId()).isEqualTo("F1");
      assertThat(positions.get(0).daekningBeloeb()).isEqualByComparingTo("300.00");
      assertThat(positions.get(0).fullyCovers()).isFalse();
    }
  }

  @Nested
  @DisplayName("getOrdering() — opskrivning repositioning")
  class OpskrivningRepositioning {

    @Test
    @DisplayName("opskrivningsfordring positioned immediately after its parent")
    void opskrivningAfterParent() {
      DaekningFordringEntity parent = fordring("PARENT", LOW_PRIORITY, "100.00", false, false);
      DaekningFordringEntity child =
          DaekningFordringEntity.builder()
              .fordringId("CHILD")
              .debtorId(DEBTOR)
              .prioritetKategori(LOW_PRIORITY)
              .tilbaestaaendeBeloeb(new BigDecimal("50.00"))
              .modtagelsesdato(LocalDate.of(2025, 6, 1))
              .receivedAt(NOW.minusSeconds(60))
              .inLoenindeholdelsesIndsats(false)
              .inUdlaegForretning(false)
              .opskrivningAfFordringId("PARENT")
              .build();
      DaekningFordringEntity other = fordring("OTHER", LOW_PRIORITY, "200.00", false, false);
      when(fordringRepo.findByDebtorId(DEBTOR)).thenReturn(List.of(other, child, parent));

      var positions = svc.getOrdering(DEBTOR, null);

      List<String> ids =
          positions.stream().map(DaekningsraekkefoelgePositionDto::fordringId).toList();
      int parentIdx = ids.indexOf("PARENT");
      int childIdx = ids.indexOf("CHILD");
      assertThat(childIdx).isEqualTo(parentIdx + 1);
    }
  }
}
