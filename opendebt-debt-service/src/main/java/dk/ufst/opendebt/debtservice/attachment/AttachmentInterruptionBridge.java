package dk.ufst.opendebt.debtservice.attachment;

import java.time.LocalDate;
import java.util.UUID;

public interface AttachmentInterruptionBridge {
  String registerUdlaegInterruption(UUID fordringId, LocalDate outcomeDate);
}
