package dk.ufst.opendebt.debtservice.exception;

import java.util.UUID;

public class WaiverAlreadyAppliedException extends RuntimeException {
  public WaiverAlreadyAppliedException(UUID eventId) {
    super("Waiver already applied for event: " + eventId);
  }
}
