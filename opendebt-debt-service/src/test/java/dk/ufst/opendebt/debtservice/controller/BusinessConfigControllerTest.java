package dk.ufst.opendebt.debtservice.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dk.ufst.opendebt.debtservice.dto.config.ConfigCreationResult;
import dk.ufst.opendebt.debtservice.dto.config.ConfigEntryDto;
import dk.ufst.opendebt.debtservice.dto.config.CreateConfigRequest;
import dk.ufst.opendebt.debtservice.dto.config.UpdateConfigRequest;
import dk.ufst.opendebt.debtservice.service.BusinessConfigService;

@ExtendWith(MockitoExtension.class)
class BusinessConfigControllerTest {

  @Mock private BusinessConfigService configService;

  private BusinessConfigController controller;

  @BeforeEach
  void setUp() {
    controller = new BusinessConfigController(configService);
  }

  @Test
  void listAll_returnsGroupedEntries() {
    ConfigEntryDto dto = ConfigEntryDto.builder().configKey("RATE_INDR_STD").build();
    Map<String, List<ConfigEntryDto>> grouped = Map.of("RATE_INDR_STD", List.of(dto));
    when(configService.listAllGrouped()).thenReturn(grouped);

    var response = controller.listAll();

    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getBody()).containsKey("RATE_INDR_STD");
    assertThat(response.getBody().get("RATE_INDR_STD")).hasSize(1);
  }

  @Test
  void getEffective_defaultsToToday() {
    ConfigEntryDto dto = ConfigEntryDto.builder().configKey("RATE_INDR_STD").build();
    when(configService.getEffectiveEntry(eq("RATE_INDR_STD"), any(LocalDate.class)))
        .thenReturn(dto);

    var response = controller.getEffective("RATE_INDR_STD", null);

    assertThat(response.getStatusCode().value()).isEqualTo(200);
    verify(configService).getEffectiveEntry("RATE_INDR_STD", LocalDate.now());
  }

  @Test
  void getEffective_withDate_usesProvidedDate() {
    LocalDate specificDate = LocalDate.of(2025, 6, 1);
    ConfigEntryDto dto = ConfigEntryDto.builder().configKey("RATE_INDR_STD").build();
    when(configService.getEffectiveEntry("RATE_INDR_STD", specificDate)).thenReturn(dto);

    var response = controller.getEffective("RATE_INDR_STD", specificDate);

    assertThat(response.getStatusCode().value()).isEqualTo(200);
    verify(configService).getEffectiveEntry("RATE_INDR_STD", specificDate);
  }

  @Test
  void create_returnsCreated() {
    CreateConfigRequest req =
        CreateConfigRequest.builder()
            .configKey("RATE_INDR_STD")
            .configValue("0.05")
            .valueType("DECIMAL")
            .validFrom(LocalDate.now().plusDays(1))
            .description("desc")
            .legalBasis("law")
            .build();
    ConfigEntryDto dto = ConfigEntryDto.builder().configKey("RATE_INDR_STD").build();
    ConfigCreationResult result =
        ConfigCreationResult.builder().created(dto).derivedEntries(List.of()).build();
    when(configService.createEntry(any(CreateConfigRequest.class), any(), anyBoolean()))
        .thenReturn(result);

    var response = controller.create(req, null);

    assertThat(response.getStatusCode().value()).isEqualTo(201);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCreated()).isSameAs(dto);
  }

  @Test
  void create_nbRate_includesDerivedEntries() {
    CreateConfigRequest req =
        CreateConfigRequest.builder()
            .configKey("RATE_NB_UDLAAN")
            .configValue("0.0175")
            .valueType("DECIMAL")
            .validFrom(LocalDate.now().plusDays(1))
            .description("NB rate")
            .legalBasis("law")
            .build();
    List<ConfigEntryDto> derived =
        List.of(
            ConfigEntryDto.builder().configKey("RATE_INDR_STD").build(),
            ConfigEntryDto.builder().configKey("RATE_INDR_TOLD").build(),
            ConfigEntryDto.builder().configKey("RATE_INDR_TOLD_AFD").build());
    ConfigCreationResult result =
        ConfigCreationResult.builder()
            .created(ConfigEntryDto.builder().configKey("RATE_NB_UDLAAN").build())
            .derivedEntries(derived)
            .build();
    when(configService.createEntry(any(CreateConfigRequest.class), any(), anyBoolean()))
        .thenReturn(result);

    var response = controller.create(req, null);

    assertThat(response.getStatusCode().value()).isEqualTo(201);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getDerivedEntries()).hasSize(3);
  }

  @Test
  void update_returnUpdatedEntry() {
    UUID id = UUID.randomUUID();
    UpdateConfigRequest req = UpdateConfigRequest.builder().configValue("0.06").build();
    ConfigEntryDto dto =
        ConfigEntryDto.builder().configKey("RATE_INDR_STD").configValue("0.06").build();
    when(configService.updateEntry(eq(id), any(UpdateConfigRequest.class), any())).thenReturn(dto);

    var response = controller.update(id, req, null);

    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getBody()).isSameAs(dto);
  }

  @Test
  void approve_returnsApprovedEntry() {
    UUID id = UUID.randomUUID();
    ConfigEntryDto dto = ConfigEntryDto.builder().reviewStatus("APPROVED").build();
    when(configService.approveEntry(eq(id), any())).thenReturn(dto);

    var response = controller.approve(id, null);

    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getReviewStatus()).isEqualTo("APPROVED");
  }

  @Test
  void reject_returnsNoContent() {
    UUID id = UUID.randomUUID();

    var response = controller.reject(id, null);

    assertThat(response.getStatusCode().value()).isEqualTo(204);
    verify(configService).rejectEntry(eq(id), any());
  }

  @Test
  void delete_returnsNoContent() {
    UUID id = UUID.randomUUID();

    var response = controller.delete(id, null);

    assertThat(response.getStatusCode().value()).isEqualTo(204);
    verify(configService).deleteEntry(eq(id), any());
  }

  @Test
  void handleValidationException_returns400() {
    var ex = new BusinessConfigService.ConfigValidationException("invalid input");

    var response = controller.handleValidation(ex);

    assertThat(response.getStatusCode().value()).isEqualTo(400);
    assertThat(response.getBody()).containsEntry("error", "invalid input");
  }

  @Test
  void handleNotFoundException_returns404() {
    var ex =
        new BusinessConfigService.ConfigurationNotFoundException(
            "RATE_INDR_STD", LocalDate.of(2026, 1, 1));

    var response = controller.handleNotFound(ex);

    assertThat(response.getStatusCode().value()).isEqualTo(404);
    assertThat(response.getBody()).containsEntry("error", ex.getMessage());
  }
}
