package dk.ufst.opendebt.payment.daekning.service.impl;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dk.ufst.opendebt.payment.daekning.InddrivelsesindsatsType;
import dk.ufst.opendebt.payment.daekning.PrioritetKategori;
import dk.ufst.opendebt.payment.daekning.RenteKomponent;
import dk.ufst.opendebt.payment.daekning.dto.DaekningsraekkefoelgePositionDto;
import dk.ufst.opendebt.payment.daekning.dto.SimulatePositionDto;
import dk.ufst.opendebt.payment.daekning.entity.DaekningFordringEntity;
import dk.ufst.opendebt.payment.daekning.entity.DaekningRecord;
import dk.ufst.opendebt.payment.daekning.repository.DaekningFordringRepository;
import dk.ufst.opendebt.payment.daekning.repository.DaekningRecordRepository;
import dk.ufst.opendebt.payment.daekning.service.DaekningsRaekkefoeigenService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DaekningsRaekkefoeigenServiceImpl implements DaekningsRaekkefoeigenService {

  private static final LocalDate LEGACY_CUTOFF = LocalDate.of(2013, 9, 1);

  private final DaekningFordringRepository fordringRepository;
  private final DaekningRecordRepository daekningRecordRepository;
  private final Clock clock;

  // ─────────────────────────────────────────────────────────────────────────
  // Public API
  // ─────────────────────────────────────────────────────────────────────────

  @Override
  @Transactional(readOnly = true)
  public List<DaekningsraekkefoelgePositionDto> getOrdering(String debtorId, LocalDate asOf) {
    Instant cutoff =
        asOf != null
            ? asOf.plusDays(1).atStartOfDay().toInstant(java.time.ZoneOffset.UTC)
            : Instant.now(clock);
    List<DaekningFordringEntity> all = fordringRepository.findByDebtorId(debtorId);
    List<DaekningFordringEntity> filtered =
        all.stream().filter(f -> !f.getReceivedAt().isAfter(cutoff)).toList();

    List<DaekningFordringEntity> ordered = buildOrderedFordringList(filtered, null);

    List<DaekningsraekkefoelgePositionDto> positions = new ArrayList<>();
    for (DaekningFordringEntity e : ordered) {
      List<SubPosition> subs = expandSubPositions(e);
      for (SubPosition sub : subs) {
        positions.add(
            new DaekningsraekkefoelgePositionDto(
                e.getFordringId(),
                e.getFordringshaverId(),
                e.getPrioritetKategori(),
                e.getPrioritetKategori().gilParagraf,
                sub.komponent(),
                computeFifoSortKey(e),
                e.getModtagelsesdato(),
                sub.beloeb(),
                e.getOpskrivningAfFordringId()));
      }
    }
    return positions;
  }

  @Override
  @Transactional(readOnly = true)
  public List<SimulatePositionDto> simulate(
      String debtorId,
      BigDecimal beloeb,
      InddrivelsesindsatsType inddrivelsesindsatsType,
      Instant applicationTimestamp) {

    List<DaekningFordringEntity> all = fordringRepository.findByDebtorId(debtorId);
    List<DaekningFordringEntity> filtered = filterByTimestamp(all, applicationTimestamp);

    return buildSimulation(filtered, beloeb, inddrivelsesindsatsType);
  }

  @Override
  @Transactional
  public List<DaekningRecord> apply(
      String debtorId,
      BigDecimal beloeb,
      InddrivelsesindsatsType inddrivelsesindsatsType,
      Instant betalingstidspunkt,
      Instant applicationTimestamp) {

    List<DaekningFordringEntity> all = fordringRepository.findByDebtorId(debtorId);
    List<DaekningFordringEntity> filtered = filterByTimestamp(all, applicationTimestamp);

    List<AllocationEntry> allocations = buildAllocations(filtered, beloeb, inddrivelsesindsatsType);

    List<DaekningRecord> records = new ArrayList<>();
    for (AllocationEntry a : allocations) {
      if (a.daekningBeloeb().compareTo(BigDecimal.ZERO) <= 0) {
        continue;
      }
      DaekningRecord r =
          DaekningRecord.builder()
              .fordringId(a.entity().getFordringId())
              .debtorId(debtorId)
              .komponent(a.komponent())
              .daekningBeloeb(a.daekningBeloeb())
              .betalingstidspunkt(betalingstidspunkt)
              .applicationTimestamp(applicationTimestamp)
              .gilParagraf(
                  resolveGilParagraf(inddrivelsesindsatsType, a.entity().getPrioritetKategori()))
              .prioritetKategori(a.entity().getPrioritetKategori())
              .fifoSortKey(computeFifoSortKey(a.entity()))
              .udlaegSurplus(a.isUdlaegSurplus())
              .inddrivelsesindsatsType(inddrivelsesindsatsType)
              .opskrivningAfFordringId(a.entity().getOpskrivningAfFordringId())
              .createdBy("system")
              .createdAt(Instant.now(clock))
              .build();
      records.add(r);
    }

    return daekningRecordRepository.saveAll(records);
  }

  // ─────────────────────────────────────────────────────────────────────────
  // Core algorithm steps
  // ─────────────────────────────────────────────────────────────────────────

  /** Step 1: filter by applicationTimestamp */
  private List<DaekningFordringEntity> filterByTimestamp(
      List<DaekningFordringEntity> all, Instant applicationTimestamp) {
    if (applicationTimestamp == null) {
      return all;
    }
    return all.stream().filter(f -> !f.getReceivedAt().isAfter(applicationTimestamp)).toList();
  }

  /**
   * Steps 2-5: partition by inddrivelsesindsatsType, sort by priority+FIFO, reposition
   * opskrivningsfordringer.
   */
  private List<DaekningFordringEntity> buildOrderedFordringList(
      List<DaekningFordringEntity> fordringer, InddrivelsesindsatsType inddrivelsesindsatsType) {

    // Step 2: filter by indsats
    List<DaekningFordringEntity> primary;
    if (inddrivelsesindsatsType == InddrivelsesindsatsType.UDLAEG) {
      primary =
          fordringer.stream()
              .filter(f -> Boolean.TRUE.equals(f.getInUdlaegForretning()))
              .collect(Collectors.toCollection(ArrayList::new));
    } else if (inddrivelsesindsatsType == InddrivelsesindsatsType.LOENINDEHOLDELSE
        || inddrivelsesindsatsType == InddrivelsesindsatsType.MODREGNING) {
      // indsats-fordringer first, then eligible surplus fordringer
      List<DaekningFordringEntity> indsats =
          fordringer.stream()
              .filter(f -> Boolean.TRUE.equals(f.getInLoenindeholdelsesIndsats()))
              .collect(Collectors.toCollection(ArrayList::new));
      List<DaekningFordringEntity> surplus =
          fordringer.stream()
              .filter(f -> !Boolean.TRUE.equals(f.getInLoenindeholdelsesIndsats()))
              .collect(Collectors.toCollection(ArrayList::new));
      primary = new ArrayList<>();
      primary.addAll(indsats);
      primary.addAll(surplus);
    } else {
      primary = new ArrayList<>(fordringer);
    }

    // Step 3+4: sort by PrioritetKategori ordinal, then FIFO sort key, then sekvensNummer, then
    // fordringId for deterministic stable ordering (test reproducibility)
    primary.sort(
        Comparator.comparingInt((DaekningFordringEntity e) -> e.getPrioritetKategori().ordinal())
            .thenComparing(this::computeFifoSortKey, Comparator.naturalOrder())
            .thenComparingInt(
                e -> e.getSekvensNummer() != null ? e.getSekvensNummer() : Integer.MAX_VALUE)
            .thenComparing(DaekningFordringEntity::getFordringId));

    // Step 5: reposition opskrivningsfordringer
    return repositionOpskrivningsfordringer(primary);
  }

  /** Step 5: insert each opskrivningsfordring immediately after its parent. */
  private List<DaekningFordringEntity> repositionOpskrivningsfordringer(
      List<DaekningFordringEntity> sorted) {

    List<DaekningFordringEntity> stamfordringer =
        sorted.stream()
            .filter(
                e ->
                    e.getOpskrivningAfFordringId() == null
                        || e.getOpskrivningAfFordringId().isBlank())
            .collect(Collectors.toCollection(LinkedList::new));

    Map<String, List<DaekningFordringEntity>> opskriv =
        sorted.stream()
            .filter(
                e ->
                    e.getOpskrivningAfFordringId() != null
                        && !e.getOpskrivningAfFordringId().isBlank())
            .sorted(Comparator.comparing(DaekningFordringEntity::getModtagelsesdato))
            .collect(Collectors.groupingBy(DaekningFordringEntity::getOpskrivningAfFordringId));

    if (opskriv.isEmpty()) {
      return stamfordringer;
    }

    List<DaekningFordringEntity> result = new ArrayList<>();
    for (DaekningFordringEntity e : stamfordringer) {
      result.add(e);
      List<DaekningFordringEntity> children = opskriv.get(e.getFordringId());
      if (children != null) {
        result.addAll(children);
      }
    }

    // Handle opskrivningsfordringer whose parent has tilbaestaaendeBeloeb == 0 (not in stamlist)
    Map<String, DaekningFordringEntity> allById =
        sorted.stream()
            .collect(Collectors.toMap(DaekningFordringEntity::getFordringId, e -> e, (a, b) -> a));

    for (Map.Entry<String, List<DaekningFordringEntity>> entry : opskriv.entrySet()) {
      String parentId = entry.getKey();
      List<DaekningFordringEntity> children = entry.getValue();
      boolean parentInResult = result.stream().anyMatch(e -> e.getFordringId().equals(parentId));
      if (!parentInResult) {
        insertOrphanedChildren(result, allById, parentId, children);
      }
    }

    return result;
  }

  /**
   * Inserts opskrivningsfordringer whose parent is absent from the result list at the correct
   * natural position (determined by parent's or child's FIFO sort key and priority category).
   */
  private void insertOrphanedChildren(
      List<DaekningFordringEntity> result,
      Map<String, DaekningFordringEntity> allById,
      String parentId,
      List<DaekningFordringEntity> children) {
    int insertAt;
    if (allById.containsKey(parentId)) {
      // Parent present in full sorted list (zero balance) → anchor by parent's fifo/category
      DaekningFordringEntity parent = allById.get(parentId);
      insertAt =
          findInsertPosition(result, computeFifoSortKey(parent), parent.getPrioritetKategori());
    } else if (!children.isEmpty()) {
      // Parent excluded by timestamp filter → anchor by first child's fifo/category
      DaekningFordringEntity first = children.get(0);
      insertAt =
          findInsertPosition(result, computeFifoSortKey(first), first.getPrioritetKategori());
    } else {
      insertAt = 0;
    }
    result.addAll(insertAt, children);
  }

  /**
   * Returns the index after the last result entry that shares {@code kat} and is not after {@code
   * fifoKey}.
   */
  private int findInsertPosition(
      List<DaekningFordringEntity> result, LocalDate fifoKey, PrioritetKategori kat) {
    int insertAt = 0;
    for (int i = 0; i < result.size(); i++) {
      DaekningFordringEntity r = result.get(i);
      if (r.getPrioritetKategori() == kat && !computeFifoSortKey(r).isAfter(fifoKey)) {
        insertAt = i + 1;
      }
    }
    return insertAt;
  }

  /** Step 4: compute FIFO sort key for a fordring. */
  private LocalDate computeFifoSortKey(DaekningFordringEntity e) {
    LocalDate legacy = e.getLegacyModtagelsesdato();
    if (legacy != null && e.getModtagelsesdato().isBefore(LEGACY_CUTOFF)) {
      return legacy;
    }
    return e.getModtagelsesdato();
  }

  /** Step 6: expand a fordring into sub-positions (only those with beloeb > 0). */
  private List<SubPosition> expandSubPositions(DaekningFordringEntity e) {
    List<SubPosition> subs = new ArrayList<>();
    BigDecimal opkr = e.getBeloebOpkraevningsrenter();
    BigDecimal inddrFord = e.getBeloebInddrivelsesrenterFordringshaver();
    BigDecimal inddrFoer = e.getBeloebInddrivelsesrenterFoerTilbagefoersel();
    BigDecimal inddrStk1 = e.getBeloebInddrivelsesrenterStk1();
    BigDecimal oevrige = e.getBeloebOevrigeRenterPsrm();
    BigDecimal hoof = e.getBeloebHooffordring();

    // If no explicit breakdown → use tilbaestaaendeBeloeb as HOOFDFORDRING
    boolean hasBreakdown = anyPositive(opkr, inddrFord, inddrFoer, inddrStk1, oevrige, hoof);
    if (!hasBreakdown) {
      BigDecimal total = e.getTilbaestaaendeBeloeb();
      if (total != null && total.compareTo(BigDecimal.ZERO) > 0) {
        subs.add(new SubPosition(RenteKomponent.HOOFDFORDRING, total));
      }
      return subs;
    }

    addIfPositive(subs, RenteKomponent.OPKRAEVNINGSRENTER, opkr);
    addIfPositive(subs, RenteKomponent.INDDRIVELSESRENTER_FORDRINGSHAVER, inddrFord);
    addIfPositive(subs, RenteKomponent.INDDRIVELSESRENTER_FOER_TILBAGEFOERSEL, inddrFoer);
    addIfPositive(subs, RenteKomponent.INDDRIVELSESRENTER_STK1, inddrStk1);
    addIfPositive(subs, RenteKomponent.OEVRIGE_RENTER_PSRM, oevrige);
    addIfPositive(subs, RenteKomponent.HOOFDFORDRING, hoof);

    return subs;
  }

  private boolean anyPositive(BigDecimal... values) {
    for (BigDecimal v : values) {
      if (v != null && v.compareTo(BigDecimal.ZERO) > 0) return true;
    }
    return false;
  }

  private void addIfPositive(List<SubPosition> list, RenteKomponent k, BigDecimal v) {
    if (v != null && v.compareTo(BigDecimal.ZERO) > 0) {
      list.add(new SubPosition(k, v));
    }
  }

  // ─────────────────────────────────────────────────────────────────────────
  // Allocation helpers
  // ─────────────────────────────────────────────────────────────────────────

  private List<AllocationEntry> buildAllocations(
      List<DaekningFordringEntity> fordringer,
      BigDecimal totalBeloeb,
      InddrivelsesindsatsType inddrivelsesindsatsType) {
    if (inddrivelsesindsatsType == InddrivelsesindsatsType.UDLAEG) {
      return allocateUdlaeg(fordringer, totalBeloeb);
    }
    return allocateToEntities(
        buildOrderedFordringList(fordringer, inddrivelsesindsatsType), totalBeloeb);
  }

  /** Allocates against udlaeg fordringer only; any surplus is appended as a udlaegSurplus entry. */
  private List<AllocationEntry> allocateUdlaeg(
      List<DaekningFordringEntity> fordringer, BigDecimal beloeb) {
    List<DaekningFordringEntity> udlaeg =
        fordringer.stream()
            .filter(f -> Boolean.TRUE.equals(f.getInUdlaegForretning()))
            .collect(Collectors.toCollection(ArrayList::new));
    udlaeg.sort(
        Comparator.comparingInt((DaekningFordringEntity e) -> e.getPrioritetKategori().ordinal())
            .thenComparing(this::computeFifoSortKey)
            .thenComparingInt(
                e -> e.getSekvensNummer() != null ? e.getSekvensNummer() : Integer.MAX_VALUE));
    udlaeg = repositionOpskrivningsfordringer(udlaeg);

    List<AllocationEntry> result = allocateToEntities(udlaeg, beloeb);

    if (!udlaeg.isEmpty()) {
      BigDecimal allocated =
          result.stream()
              .map(AllocationEntry::daekningBeloeb)
              .reduce(BigDecimal.ZERO, BigDecimal::add);
      BigDecimal surplus = beloeb.subtract(allocated);
      if (surplus.compareTo(BigDecimal.ZERO) > 0) {
        result.add(new AllocationEntry(udlaeg.get(0), RenteKomponent.HOOFDFORDRING, surplus, true));
      }
    }
    return result;
  }

  /** Greedily allocates {@code beloeb} across the ordered list, stopping when exhausted. */
  private List<AllocationEntry> allocateToEntities(
      List<DaekningFordringEntity> ordered, BigDecimal beloeb) {
    List<AllocationEntry> result = new ArrayList<>();
    BigDecimal remaining = beloeb;
    for (DaekningFordringEntity e : ordered) {
      for (SubPosition sub : expandSubPositions(e)) {
        BigDecimal cover = remaining.min(sub.beloeb());
        result.add(new AllocationEntry(e, sub.komponent(), cover, false));
        remaining = remaining.subtract(cover);
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;
      }
      if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;
    }
    return result;
  }

  private List<SimulatePositionDto> buildSimulation(
      List<DaekningFordringEntity> fordringer,
      BigDecimal beloeb,
      InddrivelsesindsatsType inddrivelsesindsatsType) {

    List<AllocationEntry> allocations =
        buildAllocations(fordringer, beloeb, inddrivelsesindsatsType);
    List<SimulatePositionDto> result = new ArrayList<>();
    for (AllocationEntry a : allocations) {
      if (a.isUdlaegSurplus()) continue;
      result.add(
          new SimulatePositionDto(
              a.entity().getFordringId(),
              a.entity().getPrioritetKategori(),
              a.entity().getPrioritetKategori().gilParagraf,
              a.komponent(),
              computeFifoSortKey(a.entity()),
              getSubBeloeb(a.entity(), a.komponent()),
              a.entity().getOpskrivningAfFordringId(),
              a.daekningBeloeb(),
              a.daekningBeloeb().compareTo(getSubBeloeb(a.entity(), a.komponent())) >= 0));
    }
    return result;
  }

  private BigDecimal getSubBeloeb(DaekningFordringEntity e, RenteKomponent k) {
    return switch (k) {
      case OPKRAEVNINGSRENTER -> e.getBeloebOpkraevningsrenter();
      case INDDRIVELSESRENTER_FORDRINGSHAVER -> e.getBeloebInddrivelsesrenterFordringshaver();
      case INDDRIVELSESRENTER_FOER_TILBAGEFOERSEL ->
          e.getBeloebInddrivelsesrenterFoerTilbagefoersel();
      case INDDRIVELSESRENTER_STK1 -> e.getBeloebInddrivelsesrenterStk1();
      case OEVRIGE_RENTER_PSRM -> e.getBeloebOevrigeRenterPsrm();
      case HOOFDFORDRING -> {
        BigDecimal hoof = e.getBeloebHooffordring();
        yield hoof != null ? hoof : e.getTilbaestaaendeBeloeb();
      }
    };
  }

  private String resolveGilParagraf(
      InddrivelsesindsatsType inddrivelsesindsatsType, PrioritetKategori kategori) {
    if (inddrivelsesindsatsType == InddrivelsesindsatsType.LOENINDEHOLDELSE
        || inddrivelsesindsatsType == InddrivelsesindsatsType.MODREGNING
        || inddrivelsesindsatsType == InddrivelsesindsatsType.UDLAEG) {
      return "GIL § 4, stk. 3";
    }
    return kategori != null ? kategori.gilParagraf : "GIL § 4";
  }

  // ─────────────────────────────────────────────────────────────────────────
  // Internal value types
  // ─────────────────────────────────────────────────────────────────────────

  private record SubPosition(RenteKomponent komponent, BigDecimal beloeb) {}

  private record AllocationEntry(
      DaekningFordringEntity entity,
      RenteKomponent komponent,
      BigDecimal daekningBeloeb,
      boolean isUdlaegSurplus) {}
}
