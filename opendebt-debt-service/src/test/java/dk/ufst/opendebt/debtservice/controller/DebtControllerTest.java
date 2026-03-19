package dk.ufst.opendebt.debtservice.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import dk.ufst.opendebt.common.dto.DebtDto;
import dk.ufst.opendebt.debtservice.dto.TransferForCollectionRequest;
import dk.ufst.opendebt.debtservice.entity.ClaimLifecycleState;
import dk.ufst.opendebt.debtservice.entity.DebtEntity;
import dk.ufst.opendebt.debtservice.service.ClaimLifecycleService;
import dk.ufst.opendebt.debtservice.service.ClaimSubmissionService;
import dk.ufst.opendebt.debtservice.service.DebtService;
import dk.ufst.opendebt.debtservice.service.ReadinessValidationService;

@ExtendWith(MockitoExtension.class)
class DebtControllerTest {

  @Mock private DebtService debtService;
  @Mock private ReadinessValidationService readinessValidationService;
  @Mock private ClaimSubmissionService claimSubmissionService;
  @Mock private ClaimLifecycleService claimLifecycleService;

  private DebtController controller;

  @BeforeEach
  void setUp() {
    controller =
        new DebtController(
            debtService, readinessValidationService, claimSubmissionService, claimLifecycleService);
  }

  @Test
  void listDebts_returnsOk() {
    Page<DebtDto> page = new PageImpl<>(List.of(debtDto()), PageRequest.of(0, 10), 1);
    when(debtService.listDebts(null, null, null, null, PageRequest.of(0, 10))).thenReturn(page);

    var response = controller.listDebts(null, null, null, null, PageRequest.of(0, 10));

    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getBody()).isSameAs(page);
  }

  @Test
  void getDebt_returnsOk() {
    UUID debtId = UUID.randomUUID();
    DebtDto dto = debtDto();
    when(debtService.getDebtById(debtId)).thenReturn(dto);

    var response = controller.getDebt(debtId);

    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getBody()).isSameAs(dto);
  }

  @Test
  void getDebtsByDebtor_returnsOk() {
    List<DebtDto> debts = List.of(debtDto());
    when(debtService.getDebtsByDebtor("debtor-1")).thenReturn(debts);

    var response = controller.getDebtsByDebtor("debtor-1");

    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getBody()).isSameAs(debts);
  }

  @Test
  void createDebt_returnsCreated() {
    DebtDto dto = debtDto();
    when(debtService.createDebt(dto)).thenReturn(dto);

    var response = controller.createDebt(dto);

    assertThat(response.getStatusCode().value()).isEqualTo(201);
    assertThat(response.getBody()).isSameAs(dto);
  }

  @Test
  void updateDebt_returnsOk() {
    UUID debtId = UUID.randomUUID();
    DebtDto dto = debtDto();
    when(debtService.updateDebt(debtId, dto)).thenReturn(dto);

    var response = controller.updateDebt(debtId, dto);

    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getBody()).isSameAs(dto);
  }

  @Test
  void validateReadiness_returnsOk() {
    UUID debtId = UUID.randomUUID();
    DebtDto dto = debtDto();
    when(readinessValidationService.validateReadiness(debtId)).thenReturn(dto);

    var response = controller.validateReadiness(debtId);

    assertThat(response.getBody()).isSameAs(dto);
  }

  @Test
  void approveReadiness_returnsOk() {
    UUID debtId = UUID.randomUUID();
    DebtDto dto = debtDto();
    when(readinessValidationService.approveReadiness(debtId)).thenReturn(dto);

    var response = controller.approveReadiness(debtId);

    assertThat(response.getBody()).isSameAs(dto);
  }

  @Test
  void rejectReadiness_returnsOk() {
    UUID debtId = UUID.randomUUID();
    DebtDto dto = debtDto();
    when(readinessValidationService.rejectReadiness(debtId, "missing docs")).thenReturn(dto);

    var response = controller.rejectReadiness(debtId, "missing docs");

    assertThat(response.getBody()).isSameAs(dto);
  }

  @Test
  void cancelDebt_returnsNoContent() {
    UUID debtId = UUID.randomUUID();

    var response = controller.cancelDebt(debtId);

    assertThat(response.getStatusCode().value()).isEqualTo(204);
    verify(debtService).cancelDebt(debtId);
  }

  @Test
  void findByOcrLine_returnsOk() {
    List<DebtDto> debts = List.of(debtDto());
    when(debtService.findByOcrLine("OCR-1")).thenReturn(debts);

    var response = controller.findByOcrLine("OCR-1");

    assertThat(response.getBody()).isSameAs(debts);
  }

  @Test
  void writeDown_returnsOk() {
    UUID debtId = UUID.randomUUID();
    DebtDto dto = debtDto();
    when(debtService.writeDown(debtId, new BigDecimal("100"))).thenReturn(dto);

    var response = controller.writeDown(debtId, new BigDecimal("100"));

    assertThat(response.getBody()).isSameAs(dto);
  }

  @Test
  void evaluateClaimState_returnsOk() {
    UUID debtId = UUID.randomUUID();
    DebtEntity entity = debtEntity(debtId, ClaimLifecycleState.RESTANCE);
    DebtDto dto = debtDto();
    LocalDate evalDate = LocalDate.of(2026, 3, 2);
    when(claimLifecycleService.evaluateClaimState(debtId, evalDate)).thenReturn(entity);
    when(debtService.toDto(entity)).thenReturn(dto);

    var response = controller.evaluateClaimState(debtId, evalDate);

    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getBody()).isSameAs(dto);
  }

  @Test
  void evaluateClaimState_defaultsToToday() {
    UUID debtId = UUID.randomUUID();
    DebtEntity entity = debtEntity(debtId, ClaimLifecycleState.REGISTERED);
    DebtDto dto = debtDto();
    when(claimLifecycleService.evaluateClaimState(eq(debtId), any(LocalDate.class)))
        .thenReturn(entity);
    when(debtService.toDto(entity)).thenReturn(dto);

    var response = controller.evaluateClaimState(debtId, null);

    assertThat(response.getStatusCode().value()).isEqualTo(200);
    verify(claimLifecycleService).evaluateClaimState(eq(debtId), eq(LocalDate.now()));
  }

  @Test
  void transferForCollection_returnsOk() {
    UUID debtId = UUID.randomUUID();
    UUID recipientId = UUID.randomUUID();
    DebtEntity entity = debtEntity(debtId, ClaimLifecycleState.OVERDRAGET);
    DebtDto dto = debtDto();
    TransferForCollectionRequest request =
        TransferForCollectionRequest.builder().recipientId(recipientId).build();
    when(claimLifecycleService.transferForCollection(debtId, recipientId)).thenReturn(entity);
    when(debtService.toDto(entity)).thenReturn(dto);

    var response = controller.transferForCollection(debtId, request);

    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getBody()).isSameAs(dto);
  }

  private DebtDto debtDto() {
    return DebtDto.builder().id(UUID.randomUUID()).debtTypeCode("600").build();
  }

  private DebtEntity debtEntity(UUID id, ClaimLifecycleState state) {
    return DebtEntity.builder()
        .id(id)
        .debtorPersonId(UUID.randomUUID())
        .creditorOrgId(UUID.randomUUID())
        .debtTypeCode("600")
        .principalAmount(new BigDecimal("1000"))
        .dueDate(LocalDate.of(2025, 12, 1))
        .outstandingBalance(new BigDecimal("1000"))
        .status(DebtEntity.DebtStatus.ACTIVE)
        .readinessStatus(DebtEntity.ReadinessStatus.READY_FOR_COLLECTION)
        .lifecycleState(state)
        .build();
  }
}
