package dk.ufst.opendebt.debtservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dk.ufst.opendebt.debtservice.dto.config.ConfigCreationResult;
import dk.ufst.opendebt.debtservice.dto.config.ConfigEntryDto;
import dk.ufst.opendebt.debtservice.dto.config.CreateConfigRequest;
import dk.ufst.opendebt.debtservice.dto.config.UpdateConfigRequest;
import dk.ufst.opendebt.debtservice.entity.BusinessConfigEntity;
import dk.ufst.opendebt.debtservice.repository.BusinessConfigAuditRepository;
import dk.ufst.opendebt.debtservice.repository.BusinessConfigRepository;

@ExtendWith(MockitoExtension.class)
class BusinessConfigServiceCrudTest {

  @Mock private BusinessConfigRepository repository;
  @Mock private BusinessConfigAuditRepository auditRepository;

  @InjectMocks private BusinessConfigService service;

  @BeforeEach
  void setUp() {
    service.clearCache();
  }

  @Test
  void createEntry_validRequest_createsAndAutoCloses() {
    LocalDate tomorrow = LocalDate.now().plusDays(1);
    BusinessConfigEntity openEntity =
        BusinessConfigEntity.builder()
            .id(UUID.randomUUID())
            .configKey("RATE_INDR_STD")
            .configValue("0.05")
            .valueType("DECIMAL")
            .validFrom(LocalDate.of(2024, 1, 1))
            .description("Old rate")
            .legalBasis("law")
            .build();

    when(repository.findOverlapping(eq("RATE_INDR_STD"), eq(tomorrow), any()))
        .thenReturn(List.of());
    when(repository.findOpenEnded("RATE_INDR_STD")).thenReturn(List.of(openEntity));
    when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    CreateConfigRequest req =
        CreateConfigRequest.builder()
            .configKey("RATE_INDR_STD")
            .configValue("0.0575")
            .valueType("DECIMAL")
            .validFrom(tomorrow)
            .description("New rate")
            .legalBasis("law")
            .build();

    ConfigCreationResult result = service.createEntry(req, "admin", false);

    assertThat(openEntity.getValidTo()).isEqualTo(tomorrow);
    verify(repository, atLeast(2)).save(any());
    verify(auditRepository).save(any());
    assertThat(result.getCreated()).isNotNull();
    assertThat(result.getDerivedEntries()).isEmpty();
  }

  @Test
  void createEntry_pastDate_throws() {
    CreateConfigRequest req =
        CreateConfigRequest.builder()
            .configKey("RATE_INDR_STD")
            .configValue("0.05")
            .valueType("DECIMAL")
            .validFrom(LocalDate.now().minusDays(1))
            .description("desc")
            .legalBasis("law")
            .build();

    assertThatThrownBy(() -> service.createEntry(req, "admin", false))
        .isInstanceOf(BusinessConfigService.ConfigValidationException.class);
  }

  @Test
  void createEntry_overlapping_throws() {
    LocalDate tomorrow = LocalDate.now().plusDays(1);
    BusinessConfigEntity existing =
        BusinessConfigEntity.builder()
            .configKey("RATE_INDR_STD")
            .configValue("0.05")
            .valueType("DECIMAL")
            .validFrom(LocalDate.of(2024, 1, 1))
            .build();
    when(repository.findOverlapping(any(), any(), any())).thenReturn(List.of(existing));

    CreateConfigRequest req =
        CreateConfigRequest.builder()
            .configKey("RATE_INDR_STD")
            .configValue("0.0575")
            .valueType("DECIMAL")
            .validFrom(tomorrow)
            .description("desc")
            .legalBasis("law")
            .build();

    assertThatThrownBy(() -> service.createEntry(req, "admin", false))
        .isInstanceOf(BusinessConfigService.ConfigValidationException.class)
        .hasMessageContaining("overlapper");
  }

  @Test
  void createEntry_invalidDecimal_throws() {
    CreateConfigRequest req =
        CreateConfigRequest.builder()
            .configKey("RATE_INDR_STD")
            .configValue("not-a-number")
            .valueType("DECIMAL")
            .validFrom(LocalDate.now().plusDays(1))
            .description("desc")
            .legalBasis("law")
            .build();

    assertThatThrownBy(() -> service.createEntry(req, "admin", false))
        .isInstanceOf(BusinessConfigService.ConfigValidationException.class);
  }

  @Test
  void createEntry_nbRate_createsThreeDerivedEntries() {
    LocalDate tomorrow = LocalDate.now().plusDays(1);
    when(repository.findOverlapping(any(), any(), any())).thenReturn(List.of());
    when(repository.findOpenEnded(any())).thenReturn(List.of());
    when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    CreateConfigRequest req =
        CreateConfigRequest.builder()
            .configKey("RATE_NB_UDLAAN")
            .configValue("0.0175")
            .valueType("DECIMAL")
            .validFrom(tomorrow)
            .description("NB udlånsrente")
            .legalBasis("Renteloven § 5")
            .build();

    ConfigCreationResult result = service.createEntry(req, "admin", false);

    assertThat(result.getDerivedEntries()).hasSize(3);
    List<String> keys =
        result.getDerivedEntries().stream().map(ConfigEntryDto::getConfigKey).toList();
    assertThat(keys).contains("RATE_INDR_STD", "RATE_INDR_TOLD", "RATE_INDR_TOLD_AFD");

    ConfigEntryDto std =
        result.getDerivedEntries().stream()
            .filter(e -> "RATE_INDR_STD".equals(e.getConfigKey()))
            .findFirst()
            .orElseThrow();
    assertThat(new BigDecimal(std.getConfigValue())).isEqualByComparingTo(new BigDecimal("0.0575"));

    ConfigEntryDto told =
        result.getDerivedEntries().stream()
            .filter(e -> "RATE_INDR_TOLD".equals(e.getConfigKey()))
            .findFirst()
            .orElseThrow();
    assertThat(new BigDecimal(told.getConfigValue()))
        .isEqualByComparingTo(new BigDecimal("0.0375"));

    ConfigEntryDto toldAfd =
        result.getDerivedEntries().stream()
            .filter(e -> "RATE_INDR_TOLD_AFD".equals(e.getConfigKey()))
            .findFirst()
            .orElseThrow();
    assertThat(new BigDecimal(toldAfd.getConfigValue()))
        .isEqualByComparingTo(new BigDecimal("0.0275"));

    // 1 save for NB + 3 saves for derived entries
    verify(repository, times(4)).save(any());
  }

  @Test
  void updateEntry_pendingReview_succeeds() {
    UUID id = UUID.randomUUID();
    BusinessConfigEntity entity =
        BusinessConfigEntity.builder()
            .id(id)
            .configKey("RATE_INDR_STD")
            .configValue("0.05")
            .valueType("DECIMAL")
            .validFrom(LocalDate.now().plusDays(1))
            .reviewStatus(BusinessConfigEntity.ReviewStatus.PENDING_REVIEW)
            .build();
    when(repository.findById(id)).thenReturn(Optional.of(entity));
    when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    UpdateConfigRequest req = UpdateConfigRequest.builder().configValue("0.0575").build();

    ConfigEntryDto result = service.updateEntry(id, req, "admin");

    assertThat(result.getConfigValue()).isEqualTo("0.0575");
    verify(auditRepository).save(any());
  }

  @Test
  void updateEntry_activeEntry_throws() {
    UUID id = UUID.randomUUID();
    BusinessConfigEntity entity =
        BusinessConfigEntity.builder()
            .id(id)
            .configKey("RATE_INDR_STD")
            .configValue("0.05")
            .valueType("DECIMAL")
            .validFrom(LocalDate.now().minusDays(1))
            .build();
    when(repository.findById(id)).thenReturn(Optional.of(entity));

    UpdateConfigRequest req = UpdateConfigRequest.builder().configValue("0.06").build();

    assertThatThrownBy(() -> service.updateEntry(id, req, "admin"))
        .isInstanceOf(BusinessConfigService.ConfigValidationException.class);
  }

  @Test
  void approveEntry_pendingReview_setsApproved() {
    UUID id = UUID.randomUUID();
    BusinessConfigEntity entity =
        BusinessConfigEntity.builder()
            .id(id)
            .configKey("RATE_INDR_STD")
            .configValue("0.05")
            .valueType("DECIMAL")
            .validFrom(LocalDate.now().plusDays(1))
            .reviewStatus(BusinessConfigEntity.ReviewStatus.PENDING_REVIEW)
            .build();
    when(repository.findById(id)).thenReturn(Optional.of(entity));
    when(repository.findOpenEnded("RATE_INDR_STD")).thenReturn(List.of());
    when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    ConfigEntryDto result = service.approveEntry(id, "admin");

    assertThat(result.getReviewStatus()).isEqualTo("APPROVED");
    verify(auditRepository).save(any());
  }

  @Test
  void approveEntry_notPendingReview_throws() {
    UUID id = UUID.randomUUID();
    BusinessConfigEntity entity =
        BusinessConfigEntity.builder()
            .id(id)
            .configKey("RATE_INDR_STD")
            .configValue("0.05")
            .valueType("DECIMAL")
            .validFrom(LocalDate.now().plusDays(1))
            .build();
    when(repository.findById(id)).thenReturn(Optional.of(entity));

    assertThatThrownBy(() -> service.approveEntry(id, "admin"))
        .isInstanceOf(BusinessConfigService.ConfigValidationException.class);
  }

  @Test
  void rejectEntry_pendingReview_deletesEntry() {
    UUID id = UUID.randomUUID();
    BusinessConfigEntity entity =
        BusinessConfigEntity.builder()
            .id(id)
            .configKey("RATE_INDR_STD")
            .configValue("0.05")
            .valueType("DECIMAL")
            .validFrom(LocalDate.now().plusDays(1))
            .reviewStatus(BusinessConfigEntity.ReviewStatus.PENDING_REVIEW)
            .build();
    when(repository.findById(id)).thenReturn(Optional.of(entity));

    service.rejectEntry(id, "admin");

    verify(repository).delete(entity);
    verify(auditRepository).save(any());
  }

  @Test
  void deleteEntry_futureEntry_deletes() {
    UUID id = UUID.randomUUID();
    BusinessConfigEntity entity =
        BusinessConfigEntity.builder()
            .id(id)
            .configKey("RATE_INDR_STD")
            .configValue("0.05")
            .valueType("DECIMAL")
            .validFrom(LocalDate.now().plusDays(1))
            .build();
    when(repository.findById(id)).thenReturn(Optional.of(entity));

    service.deleteEntry(id, "admin");

    verify(repository).delete(entity);
    verify(auditRepository).save(any());
  }

  @Test
  void deleteEntry_activeEntry_throws() {
    UUID id = UUID.randomUUID();
    BusinessConfigEntity entity =
        BusinessConfigEntity.builder()
            .id(id)
            .configKey("RATE_INDR_STD")
            .configValue("0.05")
            .valueType("DECIMAL")
            .validFrom(LocalDate.now().minusDays(1))
            .build();
    when(repository.findById(id)).thenReturn(Optional.of(entity));

    assertThatThrownBy(() -> service.deleteEntry(id, "admin"))
        .isInstanceOf(BusinessConfigService.ConfigValidationException.class);
  }

  @Test
  void previewDerivedRates_returnsCorrectValues() {
    LocalDate tomorrow = LocalDate.now().plusDays(1);

    List<ConfigEntryDto> entries = service.previewDerivedRates(new BigDecimal("0.0175"), tomorrow);

    assertThat(entries).hasSize(3);
    ConfigEntryDto std =
        entries.stream()
            .filter(e -> "RATE_INDR_STD".equals(e.getConfigKey()))
            .findFirst()
            .orElseThrow();
    assertThat(new BigDecimal(std.getConfigValue())).isEqualByComparingTo(new BigDecimal("0.0575"));

    ConfigEntryDto told =
        entries.stream()
            .filter(e -> "RATE_INDR_TOLD".equals(e.getConfigKey()))
            .findFirst()
            .orElseThrow();
    assertThat(new BigDecimal(told.getConfigValue()))
        .isEqualByComparingTo(new BigDecimal("0.0375"));

    ConfigEntryDto toldAfd =
        entries.stream()
            .filter(e -> "RATE_INDR_TOLD_AFD".equals(e.getConfigKey()))
            .findFirst()
            .orElseThrow();
    assertThat(new BigDecimal(toldAfd.getConfigValue()))
        .isEqualByComparingTo(new BigDecimal("0.0275"));

    entries.forEach(e -> assertThat(e.getValidFrom()).isEqualTo(tomorrow));
  }
}
