package dk.ufst.opendebt.creditor.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import dk.ufst.opendebt.creditor.client.DebtServiceClient;
import dk.ufst.opendebt.creditor.client.RestPage;
import dk.ufst.opendebt.creditor.dto.ClaimCountsDto;
import dk.ufst.opendebt.creditor.dto.ClaimListItemDto;
import dk.ufst.opendebt.creditor.service.PortalSessionService;

@ExtendWith(MockitoExtension.class)
class ClaimsListControllerTest {

  private static final UUID TEST_CREDITOR_ORG_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000001");

  @Mock private DebtServiceClient debtServiceClient;
  @Mock private PortalSessionService portalSessionService;

  @InjectMocks private ClaimsListController controller;

  private MockHttpSession session;

  @BeforeEach
  void setUp() {
    session = new MockHttpSession();
  }

  @Test
  void recoveryList_redirectsToDemoLogin_whenNoSession() {
    when(portalSessionService.resolveActingCreditor(eq(null), any())).thenReturn(null);

    Model model = new ConcurrentModel();
    String viewName = controller.recoveryList(model, session);

    assertThat(viewName).isEqualTo("redirect:/demo-login");
  }

  @Test
  void recoveryList_returnsRecoveryListView() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);

    Model model = new ConcurrentModel();
    String viewName = controller.recoveryList(model, session);

    assertThat(viewName).isEqualTo("claims/recovery-list");
    assertThat(model.getAttribute("currentPage")).isEqualTo("claims-recovery");
    assertThat(model.getAttribute("listType")).isEqualTo("recovery");
  }

  @Test
  void recoveryTableFragment_returnsClaimsWithData() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);

    List<ClaimListItemDto> claimList = List.of(buildClaimListItem("CVR", "12345678"));
    RestPage<ClaimListItemDto> page = new RestPage<>(claimList, 0, 20, 1, 1);
    when(debtServiceClient.listClaimsInRecovery(eq(TEST_CREDITOR_ORG_ID), any())).thenReturn(page);

    Model model = new ConcurrentModel();
    String viewName =
        controller.recoveryTableFragment(
            0, 20, null, "asc", null, null, null, null, model, session);

    assertThat(viewName).isEqualTo("claims/fragments/claims-table :: claimsTable");
    @SuppressWarnings("unchecked")
    List<ClaimListItemDto> claims = (List<ClaimListItemDto>) model.getAttribute("claims");
    assertThat(claims).hasSize(1);
    assertThat(model.getAttribute("listType")).isEqualTo("recovery");
  }

  @Test
  void recoveryTableFragment_returnsEmptyList_whenServiceUnavailable() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);
    when(debtServiceClient.listClaimsInRecovery(any(), any()))
        .thenThrow(new RuntimeException("Connection refused"));

    Model model = new ConcurrentModel();
    String viewName =
        controller.recoveryTableFragment(
            0, 20, null, "asc", null, null, null, null, model, session);

    assertThat(viewName).isEqualTo("claims/fragments/claims-table :: claimsTable");
    @SuppressWarnings("unchecked")
    List<ClaimListItemDto> claims = (List<ClaimListItemDto>) model.getAttribute("claims");
    assertThat(claims).isEmpty();
  }

  @Test
  void recoveryTableFragment_censorsCprNumbers() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);

    List<ClaimListItemDto> claimList = List.of(buildClaimListItem("CPR", "0101901234"));
    RestPage<ClaimListItemDto> page = new RestPage<>(claimList, 0, 20, 1, 1);
    when(debtServiceClient.listClaimsInRecovery(eq(TEST_CREDITOR_ORG_ID), any())).thenReturn(page);

    Model model = new ConcurrentModel();
    controller.recoveryTableFragment(0, 20, null, "asc", null, null, null, null, model, session);

    @SuppressWarnings("unchecked")
    List<ClaimListItemDto> claims = (List<ClaimListItemDto>) model.getAttribute("claims");
    assertThat(claims).hasSize(1);
    assertThat(claims.get(0).getDebtorIdentifier()).isEqualTo("010190****");
  }

  @Test
  void recoveryTableFragment_doesNotCensorCvrNumbers() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);

    List<ClaimListItemDto> claimList = List.of(buildClaimListItem("CVR", "12345678"));
    RestPage<ClaimListItemDto> page = new RestPage<>(claimList, 0, 20, 1, 1);
    when(debtServiceClient.listClaimsInRecovery(eq(TEST_CREDITOR_ORG_ID), any())).thenReturn(page);

    Model model = new ConcurrentModel();
    controller.recoveryTableFragment(0, 20, null, "asc", null, null, null, null, model, session);

    @SuppressWarnings("unchecked")
    List<ClaimListItemDto> claims = (List<ClaimListItemDto>) model.getAttribute("claims");
    assertThat(claims.get(0).getDebtorIdentifier()).isEqualTo("12345678");
  }

  @Test
  void zeroBalanceList_redirectsToDemoLogin_whenNoSession() {
    when(portalSessionService.resolveActingCreditor(eq(null), any())).thenReturn(null);

    Model model = new ConcurrentModel();
    String viewName = controller.zeroBalanceList(model, session);

    assertThat(viewName).isEqualTo("redirect:/demo-login");
  }

  @Test
  void zeroBalanceList_returnsZeroBalanceListView() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);

    Model model = new ConcurrentModel();
    String viewName = controller.zeroBalanceList(model, session);

    assertThat(viewName).isEqualTo("claims/zero-balance-list");
    assertThat(model.getAttribute("currentPage")).isEqualTo("claims-zerobalance");
  }

  @Test
  void zeroBalanceTableFragment_returnsClaimsWithData() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);

    List<ClaimListItemDto> claimList = List.of(buildClaimListItem("CVR", "87654321"));
    RestPage<ClaimListItemDto> page = new RestPage<>(claimList, 0, 20, 1, 1);
    when(debtServiceClient.listZeroBalanceClaims(eq(TEST_CREDITOR_ORG_ID), any())).thenReturn(page);

    Model model = new ConcurrentModel();
    String viewName =
        controller.zeroBalanceTableFragment(
            0, 20, null, "asc", null, null, null, null, model, session);

    assertThat(viewName).isEqualTo("claims/fragments/claims-table :: claimsTable");
    @SuppressWarnings("unchecked")
    List<ClaimListItemDto> claims = (List<ClaimListItemDto>) model.getAttribute("claims");
    assertThat(claims).hasSize(1);
    assertThat(model.getAttribute("listType")).isEqualTo("zerobalance");
  }

  @Test
  void claimsCounts_redirectsToDemoLogin_whenNoSession() {
    when(portalSessionService.resolveActingCreditor(eq(null), any())).thenReturn(null);

    Model model = new ConcurrentModel();
    String viewName = controller.claimsCounts(null, null, model, session);

    assertThat(viewName).isEqualTo("redirect:/demo-login");
  }

  @Test
  void claimsCounts_returnsCountsView() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);
    ClaimCountsDto counts = ClaimCountsDto.builder().inRecovery(10).zeroBalance(5).build();
    when(debtServiceClient.getClaimCounts(TEST_CREDITOR_ORG_ID)).thenReturn(counts);

    Model model = new ConcurrentModel();
    String viewName = controller.claimsCounts(null, null, model, session);

    assertThat(viewName).isEqualTo("claims/counts");
    ClaimCountsDto result = (ClaimCountsDto) model.getAttribute("counts");
    assertThat(result.getInRecovery()).isEqualTo(10);
    assertThat(result.getZeroBalance()).isEqualTo(5);
  }

  @Test
  void claimsCounts_usesDateRangeWhenProvided() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);
    LocalDate dateFrom = LocalDate.of(2025, 1, 1);
    LocalDate dateTo = LocalDate.of(2025, 3, 31);
    ClaimCountsDto counts = ClaimCountsDto.builder().inRecovery(3).zeroBalance(1).build();
    when(debtServiceClient.getClaimCountsForDateRange(TEST_CREDITOR_ORG_ID, dateFrom, dateTo))
        .thenReturn(counts);

    Model model = new ConcurrentModel();
    String viewName = controller.claimsCounts(dateFrom, dateTo, model, session);

    assertThat(viewName).isEqualTo("claims/counts");
    assertThat(model.getAttribute("dateFrom")).isEqualTo(dateFrom);
    assertThat(model.getAttribute("dateTo")).isEqualTo(dateTo);
  }

  @Test
  void claimsCounts_returnsEmptyCounts_whenBackendFails() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);
    when(debtServiceClient.getClaimCounts(any())).thenThrow(new RuntimeException("timeout"));

    Model model = new ConcurrentModel();
    String viewName = controller.claimsCounts(null, null, model, session);

    assertThat(viewName).isEqualTo("claims/counts");
    ClaimCountsDto result = (ClaimCountsDto) model.getAttribute("counts");
    assertThat(result).isNotNull();
    assertThat(result.getInRecovery()).isZero();
  }

  private ClaimListItemDto buildClaimListItem(String debtorType, String debtorId) {
    return ClaimListItemDto.builder()
        .claimId(UUID.randomUUID())
        .receivedDate(LocalDate.of(2025, 1, 15))
        .debtorType(debtorType)
        .debtorIdentifier(debtorId)
        .debtorCount(1)
        .creditorReference("REF-001")
        .claimTypeName("SKAT")
        .claimStatus("IN_RECOVERY")
        .incorporationDate(LocalDate.of(2024, 6, 1))
        .periodFrom(LocalDate.of(2024, 1, 1))
        .periodTo(LocalDate.of(2024, 12, 31))
        .amountSentForRecovery(new BigDecimal("45000.00"))
        .balance(new BigDecimal("32750.00"))
        .balanceWithInterestAndFees(new BigDecimal("33500.00"))
        .build();
  }
}
