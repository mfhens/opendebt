package dk.ufst.opendebt.debtservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dk.ufst.opendebt.debtservice.dto.config.ConfigCreationResult;
import dk.ufst.opendebt.debtservice.dto.config.ConfigEntryDto;
import dk.ufst.opendebt.debtservice.dto.config.CreateConfigRequest;
import dk.ufst.opendebt.debtservice.repository.BusinessConfigAuditRepository;
import dk.ufst.opendebt.debtservice.repository.BusinessConfigRepository;

@ExtendWith(MockitoExtension.class)
class DerivedRateComputationTest {

  @Mock private BusinessConfigRepository repository;
  @Mock private BusinessConfigAuditRepository auditRepository;

  @InjectMocks private BusinessConfigService service;

  private final LocalDate tomorrow = LocalDate.now().plusDays(1);

  @BeforeEach
  void setUp() {
    service.clearCache();
    when(repository.findOverlapping(any(), any(), any())).thenReturn(List.of());
    when(repository.findOpenEnded(any())).thenReturn(List.of());
    when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
  }

  private ConfigCreationResult createNbRate(String nbRateValue) {
    CreateConfigRequest req =
        CreateConfigRequest.builder()
            .configKey("RATE_NB_UDLAAN")
            .configValue(nbRateValue)
            .valueType("DECIMAL")
            .validFrom(tomorrow)
            .description("NB udlånsrente")
            .legalBasis("Renteloven § 5")
            .build();
    return service.createEntry(req, "admin", false);
  }

  @Test
  void createNbRate_generatesStdRate_nbPlus4Percent() {
    ConfigCreationResult result = createNbRate("0.0175");

    ConfigEntryDto std =
        result.getDerivedEntries().stream()
            .filter(e -> "RATE_INDR_STD".equals(e.getConfigKey()))
            .findFirst()
            .orElseThrow();
    assertThat(new BigDecimal(std.getConfigValue())).isEqualByComparingTo(new BigDecimal("0.0575"));
  }

  @Test
  void createNbRate_generatesToldRate_nbPlus2Percent() {
    ConfigCreationResult result = createNbRate("0.0175");

    ConfigEntryDto told =
        result.getDerivedEntries().stream()
            .filter(e -> "RATE_INDR_TOLD".equals(e.getConfigKey()))
            .findFirst()
            .orElseThrow();
    assertThat(new BigDecimal(told.getConfigValue()))
        .isEqualByComparingTo(new BigDecimal("0.0375"));
  }

  @Test
  void createNbRate_generatesToldAfdRate_nbPlus1Percent() {
    ConfigCreationResult result = createNbRate("0.0175");

    ConfigEntryDto toldAfd =
        result.getDerivedEntries().stream()
            .filter(e -> "RATE_INDR_TOLD_AFD".equals(e.getConfigKey()))
            .findFirst()
            .orElseThrow();
    assertThat(new BigDecimal(toldAfd.getConfigValue()))
        .isEqualByComparingTo(new BigDecimal("0.0275"));
  }

  @Test
  void createNbRate_derivedEntriesHavePendingReviewStatus() {
    ConfigCreationResult result = createNbRate("0.0175");

    result
        .getDerivedEntries()
        .forEach(e -> assertThat(e.getReviewStatus()).isEqualTo("PENDING_REVIEW"));
  }

  @Test
  void createNbRate_derivedEntriesSameValidFrom() {
    ConfigCreationResult result = createNbRate("0.0175");

    result.getDerivedEntries().forEach(e -> assertThat(e.getValidFrom()).isEqualTo(tomorrow));
  }

  @Test
  void createOtherKey_noDerivedEntries() {
    CreateConfigRequest req =
        CreateConfigRequest.builder()
            .configKey("RATE_INDR_STD")
            .configValue("0.0575")
            .valueType("DECIMAL")
            .validFrom(tomorrow)
            .description("Standard indrivelsesrente")
            .legalBasis("Gældsinddrivelsesloven § 5")
            .build();

    ConfigCreationResult result = service.createEntry(req, "admin", false);

    assertThat(result.getDerivedEntries()).isEmpty();
  }
}
