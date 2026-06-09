package dk.ufst.opendebt.debtservice.section50.service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import dk.ufst.opendebt.debtservice.section50.Section50ClaimCategory;
import dk.ufst.opendebt.debtservice.section50.Section50ContextType;
import dk.ufst.opendebt.debtservice.section50.Section50ItemType;
import dk.ufst.opendebt.debtservice.section50.Section50OrderingMode;
import dk.ufst.opendebt.debtservice.section50.client.PaymentCoverageOrderClient;
import dk.ufst.opendebt.debtservice.section50.dto.GenerateSection50WorklistRequest;
import dk.ufst.opendebt.debtservice.section50.entity.Section50CandidateItemEntity;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class Section50OrderingPolicyEngine {

  private static final Map<String, Integer> COMPLEXITY_PRIORITY =
      Map.of("low", 0, "medium", 1, "high", 2);
  private static final Map<String, Integer> PAYMENT_OPPORTUNITY_PRIORITY =
      Map.of("high", 0, "medium", 1, "low", 2);

  private final PaymentCoverageOrderClient paymentCoverageOrderClient;

  public ComputedWorklist compute(
      UUID debtorPersonId,
      GenerateSection50WorklistRequest request,
      List<Section50CandidateItemEntity> candidateItems,
      String overrideReason,
      String overrideLegalBasis,
      boolean expedited,
      List<String> selectedClaimOrder) {
    Section50ContextType contextType = request.contextType();
    BigDecimal amountWindow = resolveAmountWindow(request);
    List<Section50CandidateItemEntity> eligible = filterEligibleItems(candidateItems, contextType);

    return switch (contextType) {
      case DEFAULT ->
          buildDefault(
              eligible, amountWindow, overrideReason, overrideLegalBasis, selectedClaimOrder);
      case DATA_ERROR -> buildDataError(eligible, amountWindow);
      case VOLUNTARY_PAYMENT_SURPLUS ->
          buildVoluntarySurplus(
              debtorPersonId,
              eligible,
              amountWindow,
              expedited,
              overrideReason,
              overrideLegalBasis);
      case MODREGNING -> buildModregning(debtorPersonId, eligible, amountWindow);
    };
  }

  private ComputedWorklist buildDefault(
      List<Section50CandidateItemEntity> eligible,
      BigDecimal amountWindow,
      String overrideReason,
      String overrideLegalBasis,
      List<String> selectedClaimOrder) {
    List<Section50CandidateItemEntity> ordered =
        orderedByDefault(eligible).stream().collect(Collectors.toCollection(ArrayList::new));
    Section50OrderingMode orderingMode = Section50OrderingMode.DEFAULT_SECTION_50;
    String rulePath = "DEFAULT_SECTION_50_PATH";
    String legalReference = "Section 50 default";
    String deviationReason = null;

    if (overrideReason != null && !overrideReason.isBlank()) {
      orderingMode = Section50OrderingMode.OVERRIDE;
      rulePath = "SECTION_50_OVERRIDE_PATH";
      legalReference = overrideLegalBasis;
      deviationReason = overrideReason;
      if (selectedClaimOrder != null && !selectedClaimOrder.isEmpty()) {
        Map<String, Integer> orderIndex = indexByValue(selectedClaimOrder);
        ordered.sort(
            Comparator.comparingInt(
                    (Section50CandidateItemEntity item) ->
                        orderIndex.getOrDefault(item.getClaimId(), Integer.MAX_VALUE))
                .thenComparing(this::defaultComparator));
      }
    }

    List<ComputedEntry> entries = applyWindow(ordered, amountWindow, false);
    return new ComputedWorklist(
        orderingMode,
        legalReference,
        amountWindow,
        firstClaimId(entries),
        entries,
        rulePath,
        inputHash(ordered, orderingMode, amountWindow),
        String.join(" | ", nonBlankValues(overrideReason, overrideLegalBasis)),
        nonBlank(List.of()),
        deviationReason);
  }

  private ComputedWorklist buildDataError(
      List<Section50CandidateItemEntity> eligible, BigDecimal amountWindow) {
    List<Section50CandidateItemEntity> ordered =
        eligible.stream()
            .sorted(
                Comparator.comparingInt(
                        (Section50CandidateItemEntity item) ->
                            PAYMENT_OPPORTUNITY_PRIORITY.getOrDefault(
                                lower(item.getPaymentOpportunity()), 9))
                    .thenComparingInt(
                        item -> COMPLEXITY_PRIORITY.getOrDefault(lower(item.getComplexity()), 9))
                    .thenComparing(
                        Section50CandidateItemEntity::getAmount, Comparator.reverseOrder())
                    .thenComparing(Section50CandidateItemEntity::getClaimId))
            .toList();
    List<ComputedEntry> entries = applyWindow(ordered, amountWindow, true);
    return new ComputedWorklist(
        Section50OrderingMode.DATA_ERROR_DISCRETIONARY,
        "Section 50 data error discretion",
        amountWindow,
        firstClaimId(entries),
        entries,
        "SECTION_50_DATA_ERROR_DISCRETIONARY_PATH",
        inputHash(ordered, Section50OrderingMode.DATA_ERROR_DISCRETIONARY, amountWindow),
        "Discretionary ordering based on payment opportunity, complexity, amount, and suspected data error",
        List.of("paymentOpportunity", "complexity", "amount", "suspectedDataError"),
        null);
  }

  private ComputedWorklist buildVoluntarySurplus(
      UUID debtorPersonId,
      List<Section50CandidateItemEntity> eligible,
      BigDecimal amountWindow,
      boolean expedited,
      String overrideReason,
      String overrideLegalBasis) {
    List<Section50CandidateItemEntity> principals =
        eligible.stream()
            .filter(item -> item.getItemType() == Section50ItemType.PRINCIPAL)
            .toList();
    List<Section50CandidateItemEntity> accessories =
        eligible.stream()
            .filter(item -> item.getItemType() == Section50ItemType.ACCESSORY)
            .toList();
    List<String> orderedPrincipalClaimIds =
        paymentCoverageOrderClient.orderPrincipalClaimIds(
            debtorPersonId,
            amountWindow,
            principals.stream().map(Section50CandidateItemEntity::getClaimId).toList());
    Map<String, Integer> orderIndex = indexByValue(orderedPrincipalClaimIds);
    List<Section50CandidateItemEntity> orderedPrincipals =
        principals.stream()
            .sorted(
                Comparator.comparingInt(
                        (Section50CandidateItemEntity item) ->
                            orderIndex.getOrDefault(item.getClaimId(), Integer.MAX_VALUE))
                    .thenComparing(Section50CandidateItemEntity::getClaimId))
            .toList();
    List<Section50CandidateItemEntity> orderedAccessories =
        accessories.stream().sorted(this::defaultComparator).toList();

    List<Section50CandidateItemEntity> ordered = new ArrayList<>(orderedPrincipals);
    ordered.addAll(orderedAccessories);
    Section50OrderingMode orderingMode =
        expedited
            ? Section50OrderingMode.EXPEDITED_SURPLUS
            : Section50OrderingMode.DEFAULT_SECTION_50;
    String legalReference =
        expedited ? "Section 50 subsection 4 expedited" : "Section 50 subsection 4";
    String rulePath =
        expedited ? "SECTION_50_EXPEDITED_SURPLUS_PATH" : "SECTION_50_SURPLUS_WINDOW_PATH";
    String deviationReason =
        expedited
            ? String.join(
                " | ", nonBlankValues(overrideReason, "Quicker-to-apply claims were prioritised"))
            : null;

    if (expedited) {
      ordered =
          ordered.stream()
              .sorted(
                  Comparator.comparing(Section50CandidateItemEntity::getAmount)
                      .thenComparing(this::defaultComparator))
              .toList();
    }

    List<ComputedEntry> entries = applyWindow(ordered, amountWindow, false);
    return new ComputedWorklist(
        orderingMode,
        legalReference,
        amountWindow,
        firstClaimId(entries),
        entries,
        rulePath,
        inputHash(ordered, orderingMode, amountWindow),
        String.join(" | ", nonBlankValues(overrideReason, overrideLegalBasis)),
        List.of("principalOrderingReuse"),
        deviationReason);
  }

  private ComputedWorklist buildModregning(
      UUID debtorPersonId, List<Section50CandidateItemEntity> eligible, BigDecimal amountWindow) {
    List<Section50CandidateItemEntity> confirmed =
        eligible.stream()
            .filter(Section50CandidateItemEntity::isConfirmedRetskraft)
            .sorted(this::defaultComparator)
            .toList();
    List<Section50CandidateItemEntity> doubtful =
        eligible.stream()
            .filter(item -> !item.isConfirmedRetskraft())
            .sorted(
                Comparator.comparingInt(
                        (Section50CandidateItemEntity item) ->
                            item.getItemType() == Section50ItemType.PRINCIPAL ? 0 : 1)
                    .thenComparing(this::defaultComparator))
            .toList();
    List<Section50CandidateItemEntity> ordered = new ArrayList<>(confirmed);
    ordered.addAll(doubtful);
    List<ComputedEntry> entries =
        applyWindowWithConfirmedPrefix(ordered, confirmed.size(), amountWindow);
    return new ComputedWorklist(
        Section50OrderingMode.MODREGNING_WINDOWED,
        "Section 50 subsection 5",
        amountWindow,
        firstClaimId(entries),
        entries,
        "SECTION_50_MODREGNING_WINDOW_PATH",
        inputHash(ordered, Section50OrderingMode.MODREGNING_WINDOWED, amountWindow),
        "Confirmed retskraft claims consumed before doubtful window",
        List.of("confirmedRetskraftFirst"),
        null);
  }

  private List<Section50CandidateItemEntity> filterEligibleItems(
      List<Section50CandidateItemEntity> candidateItems, Section50ContextType contextType) {
    Map<String, Section50CandidateItemEntity> byClaimId =
        candidateItems.stream()
            .collect(
                Collectors.toMap(Section50CandidateItemEntity::getClaimId, Function.identity()));
    return candidateItems.stream()
        .filter(item -> !item.isDisproportionateWriteOff())
        .filter(
            item ->
                item.getItemType() != Section50ItemType.ACCESSORY
                    || accessoryEligible(item, byClaimId, contextType))
        .toList();
  }

  private boolean accessoryEligible(
      Section50CandidateItemEntity accessory,
      Map<String, Section50CandidateItemEntity> byClaimId,
      Section50ContextType contextType) {
    if (accessory.getAccessoryOfClaimId() == null) {
      return true;
    }
    Section50CandidateItemEntity principal = byClaimId.get(accessory.getAccessoryOfClaimId());
    if (principal == null) {
      return false;
    }
    return switch (contextType) {
      case DEFAULT, DATA_ERROR -> principal.isConfirmedRetskraft();
      case VOLUNTARY_PAYMENT_SURPLUS, MODREGNING -> true;
    };
  }

  private Comparator<Section50CandidateItemEntity> defaultComparator() {
    return this::defaultComparator;
  }

  private int defaultComparator(
      Section50CandidateItemEntity left, Section50CandidateItemEntity right) {
    int categoryCompare =
        Integer.compare(
            categoryPriority(left.getClaimCategory()), categoryPriority(right.getClaimCategory()));
    if (categoryCompare != 0) {
      return categoryCompare;
    }
    int typeCompare =
        Integer.compare(
            left.getItemType() == Section50ItemType.PRINCIPAL ? 0 : 1,
            right.getItemType() == Section50ItemType.PRINCIPAL ? 0 : 1);
    if (typeCompare != 0) {
      return typeCompare;
    }
    return left.getClaimId().compareTo(right.getClaimId());
  }

  private List<Section50CandidateItemEntity> orderedByDefault(
      List<Section50CandidateItemEntity> items) {
    return items.stream().sorted(this::defaultComparator).toList();
  }

  private List<ComputedEntry> applyWindow(
      List<Section50CandidateItemEntity> ordered, BigDecimal amountWindow, boolean includeFactors) {
    List<ComputedEntry> entries = new ArrayList<>();
    BigDecimal remaining = amountWindow;
    for (int index = 0; index < ordered.size(); index++) {
      Section50CandidateItemEntity item = ordered.get(index);
      boolean withinAmountWindow = remaining == null || remaining.compareTo(item.getAmount()) >= 0;
      if (withinAmountWindow && remaining != null) {
        remaining = remaining.subtract(item.getAmount());
      }
      entries.add(
          new ComputedEntry(
              index + 1,
              item.getClaimId(),
              item.getItemType(),
              item.getClaimCategory(),
              item.isSuspectedDataError(),
              item.isConfirmedRetskraft(),
              withinAmountWindow,
              selectionReason(item, includeFactors),
              includeFactors ? prioritisationFactors(item) : List.of(),
              null,
              item.getAmount()));
    }
    return entries;
  }

  private List<ComputedEntry> applyWindowWithConfirmedPrefix(
      List<Section50CandidateItemEntity> ordered, int confirmedCount, BigDecimal amountWindow) {
    List<ComputedEntry> entries = new ArrayList<>();
    BigDecimal remaining = amountWindow;
    for (int index = 0; index < ordered.size(); index++) {
      Section50CandidateItemEntity item = ordered.get(index);
      boolean withinAmountWindow = false;
      if (index >= confirmedCount) {
        withinAmountWindow = remaining == null || remaining.compareTo(item.getAmount()) >= 0;
        if (withinAmountWindow && remaining != null) {
          remaining = remaining.subtract(item.getAmount());
        }
      }
      entries.add(
          new ComputedEntry(
              index + 1,
              item.getClaimId(),
              item.getItemType(),
              item.getClaimCategory(),
              item.isSuspectedDataError(),
              item.isConfirmedRetskraft(),
              withinAmountWindow,
              item.isConfirmedRetskraft()
                  ? "Confirmed retskraft claim applied before doubtful items"
                  : selectionReason(item, false),
              List.of(),
              null,
              item.getAmount()));
    }
    return entries;
  }

  private List<String> prioritisationFactors(Section50CandidateItemEntity item) {
    return nonBlankValues(
        "paymentOpportunity=" + item.getPaymentOpportunity(),
        "complexity=" + item.getComplexity(),
        "amount=" + item.getAmount(),
        item.getErrorType() != null ? "errorType=" + item.getErrorType() : null);
  }

  private String selectionReason(Section50CandidateItemEntity item, boolean includeFactors) {
    if (item.getItemType() == Section50ItemType.ACCESSORY) {
      return "Accessory amount ranked after principal items";
    }
    if (includeFactors) {
      return "Discretionary ordering used prioritisation factors";
    }
    return "Section 50 ordering selected this item next";
  }

  private BigDecimal resolveAmountWindow(GenerateSection50WorklistRequest request) {
    if (request.availableAmount() == null) {
      return null;
    }
    BigDecimal confirmed =
        request.confirmedAmountCovered() == null
            ? BigDecimal.ZERO
            : request.confirmedAmountCovered();
    return request.availableAmount().subtract(confirmed).max(BigDecimal.ZERO);
  }

  private int categoryPriority(Section50ClaimCategory claimCategory) {
    return switch (claimCategory) {
      case FINE -> 0;
      case PRIVATE_MAINTENANCE -> 1;
      case OTHER -> 2;
    };
  }

  private String firstClaimId(List<ComputedEntry> entries) {
    return entries.isEmpty() ? null : entries.get(0).claimId();
  }

  private String inputHash(
      List<Section50CandidateItemEntity> ordered,
      Section50OrderingMode orderingMode,
      BigDecimal amountWindow) {
    String fingerprint =
        ordered.stream()
                .map(
                    item ->
                        item.getClaimId()
                            + ":"
                            + item.getAmount()
                            + ":"
                            + item.isConfirmedRetskraft())
                .collect(Collectors.joining("|"))
            + "|"
            + orderingMode
            + "|"
            + amountWindow;
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(fingerprint.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 not available", ex);
    }
  }

  private Map<String, Integer> indexByValue(List<String> values) {
    return values.stream()
        .collect(Collectors.toMap(Function.identity(), values::indexOf, (left, right) -> left));
  }

  private List<String> nonBlank(List<String> values) {
    return values.stream().filter(value -> value != null && !value.isBlank()).toList();
  }

  private List<String> nonBlankValues(String... values) {
    return java.util.Arrays.stream(values)
        .filter(value -> value != null && !value.isBlank())
        .toList();
  }

  private String lower(String value) {
    return value == null ? "" : value.toLowerCase();
  }

  public record ComputedEntry(
      int rank,
      String claimId,
      dk.ufst.opendebt.debtservice.section50.Section50ItemType itemType,
      dk.ufst.opendebt.debtservice.section50.Section50ClaimCategory claimCategory,
      boolean suspectedDataError,
      boolean confirmedRetskraft,
      boolean withinAmountWindow,
      String selectionReason,
      List<String> prioritisationFactors,
      String suppressedReason,
      BigDecimal amount) {}

  public record ComputedWorklist(
      dk.ufst.opendebt.debtservice.section50.Section50OrderingMode orderingMode,
      String legalReference,
      BigDecimal amountWindow,
      String selectedNextItemId,
      List<ComputedEntry> entries,
      String rulePath,
      String inputHash,
      String notes,
      List<String> prioritisationFactors,
      String deviationReason) {}
}
