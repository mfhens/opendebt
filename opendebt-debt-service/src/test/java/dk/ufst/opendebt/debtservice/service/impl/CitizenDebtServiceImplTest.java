package dk.ufst.opendebt.debtservice.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import dk.ufst.opendebt.debtservice.client.CreditorDisplayClient;
import dk.ufst.opendebt.debtservice.dto.CitizenDebtStatus;
import dk.ufst.opendebt.debtservice.dto.CitizenDebtSummaryResponse;
import dk.ufst.opendebt.debtservice.dto.InterestAccrualState;
import dk.ufst.opendebt.debtservice.dto.InterestPauseReasonCode;
import dk.ufst.opendebt.debtservice.dto.WrittenOffReasonCode;
import dk.ufst.opendebt.debtservice.dto.config.ConfigEntryDto;
import dk.ufst.opendebt.debtservice.entity.ClaimLifecycleState;
import dk.ufst.opendebt.debtservice.entity.DebtEntity;
import dk.ufst.opendebt.debtservice.entity.InterestSelectionEmbeddable;
import dk.ufst.opendebt.debtservice.repository.DebtRepository;
import dk.ufst.opendebt.debtservice.service.BusinessConfigService;

@ExtendWith(MockitoExtension.class)
class CitizenDebtServiceImplTest {

  private static final UUID PERSON_ID = UUID.randomUUID();

  @Mock private DebtRepository debtRepository;
  @Mock private CreditorDisplayClient creditorDisplayClient;
  @Mock private BusinessConfigService businessConfigService;

  @InjectMocks private CitizenDebtServiceImpl citizenDebtService;

  private DebtEntity debt1;
  private DebtEntity debt2;

  @BeforeEach
  void setUp() {
    debt1 =
        debt(
            PERSON_ID,
            DebtEntity.DebtStatus.ACTIVE,
            ClaimLifecycleState.OVERDRAGET,
            new BigDecimal("10000"),
            new BigDecimal("8000"),
            new BigDecimal("500"),
            new BigDecimal("100"));
    debt2 =
        debt(
            PERSON_ID,
            DebtEntity.DebtStatus.ACTIVE,
            ClaimLifecycleState.REGISTERED,
            new BigDecimal("5000"),
            new BigDecimal("5000"),
            new BigDecimal("200"),
            new BigDecimal("50"));

    lenient()
        .when(creditorDisplayClient.getDisplayName(any(UUID.class)))
        .thenAnswer(
            invocation ->
                "Creditor-" + invocation.getArgument(0, UUID.class).toString().substring(0, 8));
  }

  @Test
  void getDebtSummary_noStatusFilter_enrichesCitizenProjection() {
    Pageable pageable = PageRequest.of(0, 20);
    when(debtRepository.findByDebtorPersonId(PERSON_ID)).thenReturn(List.of(debt1, debt2));

    CitizenDebtSummaryResponse response =
        citizenDebtService.getDebtSummary(PERSON_ID, null, pageable);

    assertThat(response.getDebts()).hasSize(2);
    assertThat(response.getTotalDebtCount()).isEqualTo(2);
    assertThat(response.getTotalOutstandingAmount()).isEqualByComparingTo(new BigDecimal("13000"));
    assertThat(response.getEffectiveInterestRates()).isEmpty();

    var firstDebt = response.getDebts().get(0);
    assertThat(firstDebt.getCreditorDisplayName()).startsWith("Creditor-");
    assertThat(firstDebt.getCitizenStatus()).isEqualTo(CitizenDebtStatus.IN_COLLECTION);
    assertThat(firstDebt.getInterestAccrualState()).isEqualTo(InterestAccrualState.ACTIVE);
    assertThat(firstDebt.getInterestRuleCode()).isEqualTo("INDR_STD");
    assertThat(firstDebt.getCurrentInterestRate()).isNull();
  }

  @Test
  void getDebtSummary_withStatusFilter_returnsFilteredDebtsButPreservesTotals() {
    Pageable pageable = PageRequest.of(0, 20);
    Page<DebtEntity> page = new PageImpl<>(List.of(debt1), pageable, 1);
    when(debtRepository.findByFilters(
            eq(null), eq(PERSON_ID), eq(DebtEntity.DebtStatus.ACTIVE), eq(null), any()))
        .thenReturn(page);
    when(debtRepository.findByDebtorPersonId(PERSON_ID)).thenReturn(List.of(debt1, debt2));

    CitizenDebtSummaryResponse response =
        citizenDebtService.getDebtSummary(PERSON_ID, DebtEntity.DebtStatus.ACTIVE, pageable);

    assertThat(response.getDebts()).hasSize(1);
    assertThat(response.getTotalDebtCount()).isEqualTo(2);
    assertThat(response.getTotalOutstandingAmount()).isEqualByComparingTo(new BigDecimal("13000"));
    assertThat(response.getTotalElements()).isEqualTo(1);
  }

  @Test
  void getDebtSummary_withExplicitInterestRule_exposesRateMetadata() {
    DebtEntity interestBearingDebt =
        debt(
            PERSON_ID,
            DebtEntity.DebtStatus.ACTIVE,
            ClaimLifecycleState.OVERDRAGET,
            new BigDecimal("7300"),
            new BigDecimal("7150"),
            new BigDecimal("150"),
            BigDecimal.ZERO);
    interestBearingDebt.setInterestSelection(
        InterestSelectionEmbeddable.builder()
            .interestRule("INDR_STD")
            .interestRateCode("RATE_INDR_STD")
            .build());

    when(debtRepository.findByDebtorPersonId(PERSON_ID)).thenReturn(List.of(interestBearingDebt));
    when(businessConfigService.findEffectiveEntry(eq("RATE_INDR_STD"), any(LocalDate.class)))
        .thenReturn(
            Optional.of(
                ConfigEntryDto.builder()
                    .configKey("RATE_INDR_STD")
                    .configValue("0.0575")
                    .validFrom(LocalDate.of(2024, 1, 1))
                    .build()));

    CitizenDebtSummaryResponse response =
        citizenDebtService.getDebtSummary(PERSON_ID, null, PageRequest.of(0, 20));

    assertThat(response.getDebts()).hasSize(1);
    assertThat(response.getDebts().get(0).getInterestRuleCode()).isEqualTo("INDR_STD");
    assertThat(response.getDebts().get(0).getCurrentInterestRate())
        .isEqualByComparingTo(new BigDecimal("0.0575"));
    assertThat(response.getEffectiveInterestRates()).hasSize(1);
    assertThat(response.getEffectiveInterestRates().get(0).getAnnualRate())
        .isEqualByComparingTo(new BigDecimal("0.0575"));
    assertThat(response.getEffectiveInterestRates().get(0).getValidFrom())
        .isEqualTo(LocalDate.of(2024, 1, 1));
  }

  @Test
  void getDebtSummary_withPausedInterest_exposesPauseReasonCode() {
    DebtEntity pausedDebt =
        debt(
            PERSON_ID,
            DebtEntity.DebtStatus.ACTIVE,
            ClaimLifecycleState.OVERDRAGET,
            new BigDecimal("5100"),
            new BigDecimal("5100"),
            new BigDecimal("55"),
            BigDecimal.ZERO);
    pausedDebt.setIkkeinddrivelsesparat(true);
    pausedDebt.setClaimNote("CLAIM_UNCLEAR_DEBTOR_CANNOT_PAY");
    pausedDebt.setInterestSelection(
        InterestSelectionEmbeddable.builder()
            .interestRule("INDR_STD")
            .interestRateCode("RATE_INDR_STD")
            .build());

    when(debtRepository.findByDebtorPersonId(PERSON_ID)).thenReturn(List.of(pausedDebt));

    CitizenDebtSummaryResponse response =
        citizenDebtService.getDebtSummary(PERSON_ID, null, PageRequest.of(0, 20));

    assertThat(response.getDebts()).hasSize(1);
    assertThat(response.getDebts().get(0).getInterestAccrualState())
        .isEqualTo(InterestAccrualState.PAUSED);
    assertThat(response.getDebts().get(0).getInterestPauseReasonCode())
        .isEqualTo(InterestPauseReasonCode.CLAIM_UNCLEAR_DEBTOR_CANNOT_PAY);
    assertThat(response.getDebts().get(0).getCurrentInterestRate()).isNull();
    assertThat(response.getEffectiveInterestRates()).isEmpty();
  }

  @Test
  void getDebtSummary_withWrittenOffDebt_exposesWrittenOffReasonCode() {
    DebtEntity writtenOffDebt =
        debt(
            PERSON_ID,
            DebtEntity.DebtStatus.WRITTEN_OFF,
            ClaimLifecycleState.AFSKREVET,
            new BigDecimal("2400"),
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO);
    writtenOffDebt.setClaimNote("LIMITATION_EXPIRED");

    when(debtRepository.findByDebtorPersonId(PERSON_ID)).thenReturn(List.of(writtenOffDebt));

    CitizenDebtSummaryResponse response =
        citizenDebtService.getDebtSummary(PERSON_ID, null, PageRequest.of(0, 20));

    assertThat(response.getDebts()).hasSize(1);
    assertThat(response.getDebts().get(0).getCitizenStatus())
        .isEqualTo(CitizenDebtStatus.WRITTEN_OFF);
    assertThat(response.getDebts().get(0).getStatusReasonCode()).isEqualTo("LIMITATION_EXPIRED");
    assertThat(response.getDebts().get(0).getWrittenOffReasonCode())
        .isEqualTo(WrittenOffReasonCode.LIMITATION_EXPIRED);
  }

  @Test
  void getDebtSummary_pagination_returnsCorrectPageMetadata() {
    List<DebtEntity> debts =
        List.of(
            debt1,
            debt2,
            debt(
                PERSON_ID,
                DebtEntity.DebtStatus.ACTIVE,
                ClaimLifecycleState.OVERDRAGET,
                BigDecimal.ONE,
                BigDecimal.ONE,
                BigDecimal.ZERO,
                BigDecimal.ZERO),
            debt(
                PERSON_ID,
                DebtEntity.DebtStatus.ACTIVE,
                ClaimLifecycleState.OVERDRAGET,
                BigDecimal.TEN,
                BigDecimal.TEN,
                BigDecimal.ZERO,
                BigDecimal.ZERO),
            debt(
                PERSON_ID,
                DebtEntity.DebtStatus.ACTIVE,
                ClaimLifecycleState.OVERDRAGET,
                new BigDecimal("100"),
                new BigDecimal("100"),
                BigDecimal.ZERO,
                BigDecimal.ZERO));
    Pageable pageable = PageRequest.of(1, 2);
    when(debtRepository.findByDebtorPersonId(PERSON_ID)).thenReturn(debts);

    CitizenDebtSummaryResponse response =
        citizenDebtService.getDebtSummary(PERSON_ID, null, pageable);

    assertThat(response.getPageNumber()).isEqualTo(1);
    assertThat(response.getPageSize()).isEqualTo(2);
    assertThat(response.getTotalElements()).isEqualTo(5);
    assertThat(response.getTotalPages()).isEqualTo(3);
    assertThat(response.getDebts()).hasSize(2);
  }

  @Test
  void getDebtSummary_handlesNullAmountsWithZeroes() {
    DebtEntity debtWithNulls =
        debt(
            PERSON_ID,
            DebtEntity.DebtStatus.ACTIVE,
            ClaimLifecycleState.OVERDRAGET,
            new BigDecimal("2000"),
            null,
            null,
            null);

    when(debtRepository.findByDebtorPersonId(PERSON_ID)).thenReturn(List.of(debtWithNulls));

    CitizenDebtSummaryResponse response =
        citizenDebtService.getDebtSummary(PERSON_ID, null, PageRequest.of(0, 20));

    assertThat(response.getTotalOutstandingAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(response.getDebts()).hasSize(1);
    assertThat(response.getDebts().get(0).getOutstandingAmount())
        .isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(response.getDebts().get(0).getInterestAmount())
        .isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(response.getDebts().get(0).getFeesAmount()).isEqualByComparingTo(BigDecimal.ZERO);
  }

  @Test
  void citizenDebtSummaryResponse_matchesPetition026SummaryContract() throws IOException {
    String openApi = readOpenApi("openapi-debt-service.yaml");
    String summarySchema = schemaBlock(openApi, "CitizenDebtSummaryResponse", "CitizenDebtItem");

    assertThat(summarySchema).contains("effectiveInterestRates:");
    assertThat(summarySchema)
        .contains("debts:")
        .contains("totalOutstandingAmount:")
        .contains("totalDebtCount:")
        .contains("pageNumber:")
        .contains("pageSize:")
        .contains("totalPages:")
        .contains("totalElements:");
  }

  @Test
  void citizenDebtItemDto_matchesPetition026CitizenProjectionContract() throws IOException {
    String openApi = readOpenApi("openapi-debt-service.yaml");
    String itemSchema = schemaBlock(openApi, "CitizenDebtItem", "CitizenEffectiveInterestRate");

    assertThat(itemSchema)
        .contains("creditorDisplayName:")
        .contains("citizenStatus:")
        .contains("interestAccrualState:")
        .contains("interestRuleCode:")
        .contains("statusReasonCode:")
        .contains("interestPauseReasonCode:")
        .contains("currentInterestRate:")
        .contains("writtenOffReasonCode:");
  }

  @Test
  void citizenProjectionPresentationEnums_matchPetition026OpenApiValues() throws Exception {
    assertEnumValues(
        "dk.ufst.opendebt.debtservice.dto.CitizenDebtStatus",
        List.of(
            "IN_COLLECTION",
            "SET_OFF",
            "PAID",
            "WRITTEN_OFF",
            "DISPUTED",
            "INSTALMENT_ARRANGEMENT",
            "WAGE_GARNISHMENT"));
    assertEnumValues(
        "dk.ufst.opendebt.debtservice.dto.InterestAccrualState", List.of("ACTIVE", "PAUSED"));
    assertEnumValues(
        "dk.ufst.opendebt.debtservice.dto.InterestPauseReasonCode",
        List.of("CLAIM_UNCLEAR_DEBTOR_CANNOT_PAY"));
    assertEnumValues(
        "dk.ufst.opendebt.debtservice.dto.WrittenOffReasonCode",
        List.of(
            "LIMITATION_EXPIRED",
            "BANKRUPTCY",
            "ESTATE_OF_DECEASED",
            "DEBT_RESTRUCTURING",
            "RECOVERY_FUTILE",
            "RECOVERY_COST_DISPROPORTIONATE"));
  }

  @Test
  void creditorOpenApi_requiresDisplayNameForPetition026Enrichment() throws IOException {
    String creditorOpenApi = readOpenApi("openapi-creditor-service.yaml");
    String creditorSchema = schemaBlock(creditorOpenApi, "Creditor", "ValidateActionRequest");
    String requiredBlock = requiredBlock(creditorSchema);

    assertThat(creditorSchema).contains("displayName:");
    assertThat(requiredBlock).contains("- displayName");
  }

  private DebtEntity debt(
      UUID debtorPersonId,
      DebtEntity.DebtStatus status,
      ClaimLifecycleState lifecycleState,
      BigDecimal principalAmount,
      BigDecimal outstandingAmount,
      BigDecimal interestAmount,
      BigDecimal feesAmount) {
    return DebtEntity.builder()
        .id(UUID.randomUUID())
        .debtorPersonId(debtorPersonId)
        .creditorOrgId(UUID.randomUUID())
        .debtTypeCode("RESTSKAT")
        .principalAmount(principalAmount)
        .outstandingBalance(outstandingAmount)
        .interestAmount(interestAmount)
        .feesAmount(feesAmount)
        .dueDate(LocalDate.now().minusMonths(1))
        .status(status)
        .lifecycleState(lifecycleState)
        .readinessStatus(DebtEntity.ReadinessStatus.READY_FOR_COLLECTION)
        .build();
  }

  private void assertEnumValues(String fullyQualifiedClassName, List<String> expectedValues)
      throws Exception {
    Class<?> enumType = Class.forName(fullyQualifiedClassName);
    assertThat(enumType.isEnum()).isTrue();
    assertThat(
            Arrays.stream(enumType.getEnumConstants())
                .map(value -> ((Enum<?>) value).name())
                .toList())
        .containsExactlyElementsOf(expectedValues);
  }

  private String readOpenApi(String fileName) throws IOException {
    return Files.readString(Path.of("..", "api-specs", fileName), StandardCharsets.UTF_8);
  }

  private String schemaBlock(String openApi, String schemaName, String nextSchemaName) {
    String startMarker = "    " + schemaName + ":";
    int start = openApi.indexOf(startMarker);
    int end = openApi.indexOf("    " + nextSchemaName + ":", start + startMarker.length());
    if (end < 0) {
      end = openApi.length();
    }
    return openApi.substring(start, end);
  }

  private String requiredBlock(String schemaBlock) {
    int requiredStart = schemaBlock.indexOf("      required:");
    int propertiesStart = schemaBlock.indexOf("      properties:");
    if (requiredStart < 0 || propertiesStart < 0 || propertiesStart <= requiredStart) {
      return "";
    }
    return schemaBlock.substring(requiredStart, propertiesStart);
  }
}
