package dk.ufst.opendebt.debtservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dk.ufst.opendebt.debtservice.entity.BusinessConfigEntity;
import dk.ufst.opendebt.debtservice.repository.BusinessConfigRepository;

@ExtendWith(MockitoExtension.class)
class BusinessConfigServiceTest {

  @Mock private BusinessConfigRepository repository;

  @InjectMocks private BusinessConfigService service;

  private final LocalDate testDate = LocalDate.of(2026, 3, 19);

  @BeforeEach
  void setUp() {
    service.clearCache();
  }

  @Test
  void getDecimalValue_returnsValueFromDb() {
    BusinessConfigEntity entity =
        BusinessConfigEntity.builder()
            .configKey("RATE_INDR_STD")
            .configValue("0.0575")
            .valueType("DECIMAL")
            .validFrom(LocalDate.of(2025, 1, 1))
            .build();
    when(repository.findEffective("RATE_INDR_STD", testDate)).thenReturn(List.of(entity));

    BigDecimal result = service.getDecimalValue("RATE_INDR_STD", testDate);

    assertThat(result).isEqualByComparingTo(new BigDecimal("0.0575"));
    verify(repository).findEffective("RATE_INDR_STD", testDate);
  }

  @Test
  void getDecimalValue_returnsCachedValueOnSecondCall() {
    BusinessConfigEntity entity =
        BusinessConfigEntity.builder()
            .configKey("RATE_INDR_STD")
            .configValue("0.0575")
            .valueType("DECIMAL")
            .validFrom(LocalDate.of(2025, 1, 1))
            .build();
    when(repository.findEffective("RATE_INDR_STD", testDate)).thenReturn(List.of(entity));

    service.getDecimalValue("RATE_INDR_STD", testDate);
    BigDecimal result = service.getDecimalValue("RATE_INDR_STD", testDate);

    assertThat(result).isEqualByComparingTo(new BigDecimal("0.0575"));
    // Only one DB call — second hit comes from cache
    verify(repository, times(1)).findEffective("RATE_INDR_STD", testDate);
  }

  @Test
  void getDecimalValue_throwsWhenNotFound() {
    when(repository.findEffective("MISSING_KEY", testDate)).thenReturn(Collections.emptyList());

    assertThatThrownBy(() -> service.getDecimalValue("MISSING_KEY", testDate))
        .isInstanceOf(BusinessConfigService.ConfigurationNotFoundException.class)
        .hasMessageContaining("MISSING_KEY")
        .hasMessageContaining(testDate.toString());
  }

  @Test
  void preloadRatesForDate_loadsMultipleKeys() {
    BusinessConfigEntity stdEntity =
        BusinessConfigEntity.builder()
            .configKey("RATE_INDR_STD")
            .configValue("0.0575")
            .valueType("DECIMAL")
            .validFrom(LocalDate.of(2025, 1, 1))
            .build();
    BusinessConfigEntity toldEntity =
        BusinessConfigEntity.builder()
            .configKey("RATE_INDR_TOLD")
            .configValue("0.0375")
            .valueType("DECIMAL")
            .validFrom(LocalDate.of(2025, 1, 1))
            .build();
    when(repository.findEffective("RATE_INDR_STD", testDate)).thenReturn(List.of(stdEntity));
    when(repository.findEffective("RATE_INDR_TOLD", testDate)).thenReturn(List.of(toldEntity));

    Map<String, BigDecimal> rates =
        service.preloadRatesForDate(testDate, List.of("RATE_INDR_STD", "RATE_INDR_TOLD"));

    assertThat(rates).hasSize(2);
    assertThat(rates.get("RATE_INDR_STD")).isEqualByComparingTo(new BigDecimal("0.0575"));
    assertThat(rates.get("RATE_INDR_TOLD")).isEqualByComparingTo(new BigDecimal("0.0375"));
  }

  @Test
  void preloadRatesForDate_usesZeroForMissingKey() {
    when(repository.findEffective("MISSING_KEY", testDate)).thenReturn(Collections.emptyList());

    Map<String, BigDecimal> rates = service.preloadRatesForDate(testDate, List.of("MISSING_KEY"));

    assertThat(rates.get("MISSING_KEY")).isEqualByComparingTo(BigDecimal.ZERO);
  }

  @Test
  void preloadRatesForDate_populatesCache() {
    BusinessConfigEntity entity =
        BusinessConfigEntity.builder()
            .configKey("RATE_INDR_STD")
            .configValue("0.0575")
            .valueType("DECIMAL")
            .validFrom(LocalDate.of(2025, 1, 1))
            .build();
    when(repository.findEffective("RATE_INDR_STD", testDate)).thenReturn(List.of(entity));

    service.preloadRatesForDate(testDate, List.of("RATE_INDR_STD"));
    // Second call should come from cache
    BigDecimal cached = service.getDecimalValue("RATE_INDR_STD", testDate);

    assertThat(cached).isEqualByComparingTo(new BigDecimal("0.0575"));
    // Only one DB call total (from preload, not from getDecimalValue)
    verify(repository, times(1)).findEffective("RATE_INDR_STD", testDate);
  }

  @Test
  void clearCache_removesCachedValues() {
    BusinessConfigEntity entity =
        BusinessConfigEntity.builder()
            .configKey("RATE_INDR_STD")
            .configValue("0.0575")
            .valueType("DECIMAL")
            .validFrom(LocalDate.of(2025, 1, 1))
            .build();
    when(repository.findEffective("RATE_INDR_STD", testDate)).thenReturn(List.of(entity));

    service.getDecimalValue("RATE_INDR_STD", testDate);
    service.clearCache();
    service.getDecimalValue("RATE_INDR_STD", testDate);

    // Two DB calls — cache was cleared between
    verify(repository, times(2)).findEffective("RATE_INDR_STD", testDate);
  }

  @Test
  void getHistory_delegatesToRepository() {
    BusinessConfigEntity v1 =
        BusinessConfigEntity.builder()
            .configKey("RATE_INDR_STD")
            .configValue("0.05")
            .validFrom(LocalDate.of(2024, 1, 1))
            .build();
    BusinessConfigEntity v2 =
        BusinessConfigEntity.builder()
            .configKey("RATE_INDR_STD")
            .configValue("0.0575")
            .validFrom(LocalDate.of(2025, 7, 1))
            .build();
    when(repository.findByConfigKeyOrderByValidFromDesc("RATE_INDR_STD"))
        .thenReturn(List.of(v2, v1));

    List<BusinessConfigEntity> history = service.getHistory("RATE_INDR_STD");

    assertThat(history).hasSize(2);
    assertThat(history.get(0).getConfigValue()).isEqualTo("0.0575");
  }
}
