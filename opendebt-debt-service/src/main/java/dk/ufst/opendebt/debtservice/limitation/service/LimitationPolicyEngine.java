package dk.ufst.opendebt.debtservice.limitation.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Service;

import dk.ufst.opendebt.debtservice.limitation.entity.AfbrydelsesType;
import dk.ufst.opendebt.debtservice.limitation.entity.ForaeldelseStatus;
import dk.ufst.opendebt.debtservice.limitation.entity.Retsgrundlag;

@Service
public class LimitationPolicyEngine {

  private static final LocalDate PSRM_THRESHOLD = LocalDate.of(2015, 11, 19);
  private static final LocalDate PSRM_POSTPONEMENT = LocalDate.of(2021, 11, 20);
  private static final LocalDate DMI_THRESHOLD = LocalDate.of(2024, 1, 1);
  private static final LocalDate DMI_POSTPONEMENT = LocalDate.of(2027, 11, 20);

  private final Clock clock;
  private final AtomicReference<LocalDate> lastEvaluatedDate = new AtomicReference<>();

  public LimitationPolicyEngine(Clock limitationClock) {
    this.clock = limitationClock;
  }

  public LocalDate determinePostponementDate(LocalDate registrationDate, String sourceSystem) {
    if (registrationDate == null || sourceSystem == null) {
      return null;
    }
    String normalized = sourceSystem.trim().toUpperCase();
    if ("PSRM".equals(normalized) && !registrationDate.isBefore(PSRM_THRESHOLD)) {
      return PSRM_POSTPONEMENT;
    }
    if ("DMI_SAP38".equals(normalized) && !registrationDate.isBefore(DMI_THRESHOLD)) {
      return DMI_POSTPONEMENT;
    }
    return null;
  }

  public boolean isInUdskydelse(LocalDate udskydelseDato) {
    LocalDate today = LocalDate.now(clock);
    lastEvaluatedDate.set(today);
    return udskydelseDato != null && today.isBefore(udskydelseDato);
  }

  public LocalDate calculateInitialExpiry(
      LocalDate registrationDate, Retsgrundlag retsgrundlag, LocalDate udskydelseDato) {
    LocalDate baseExpiry = registrationDate.plusYears(3);
    if (udskydelseDato == null) {
      return baseExpiry;
    }
    LocalDate postponedFloor = udskydelseDato.plusDays(1).plusYears(3);
    return baseExpiry.isBefore(postponedFloor) ? postponedFloor : baseExpiry;
  }

  public LocalDate calculateInterruptedExpiry(
      AfbrydelsesType type, Retsgrundlag retsgrundlag, LocalDate eventDate) {
    return switch (type) {
      case BEROSTILLELSE, UDLAEG ->
          eventDate.plusYears(retsgrundlag == Retsgrundlag.SPECIAL ? 10 : 3);
      case LOENINDEHOLDELSE, MODREGNING -> eventDate.plusYears(3);
    };
  }

  public LocalDate calculateSupplementaryExpiry(
      LocalDate currentFristExpires, LocalDate appliedDate) {
    LocalDate anchor = currentFristExpires;
    if (appliedDate != null && appliedDate.isAfter(anchor)) {
      anchor = appliedDate;
    }
    return anchor.plusYears(2);
  }

  public ForaeldelseStatus deriveCurrentStatus(ForaeldelseStatus current, LocalDate expiryDate) {
    if (current == ForaeldelseStatus.INDSIGELSE_PENDING) {
      return current;
    }
    LocalDate today = LocalDate.now(clock);
    lastEvaluatedDate.set(today);
    if (current == ForaeldelseStatus.FORAELDET || expiryDate.isBefore(today)) {
      return ForaeldelseStatus.FORAELDET;
    }
    return ForaeldelseStatus.ACTIVE;
  }

  public LocalDate getLastEvaluatedDate() {
    return lastEvaluatedDate.get();
  }
}
