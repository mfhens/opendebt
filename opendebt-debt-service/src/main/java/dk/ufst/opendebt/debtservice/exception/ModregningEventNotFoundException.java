package dk.ufst.opendebt.debtservice.exception;

import java.util.UUID;

public class ModregningEventNotFoundException extends RuntimeException {
  public ModregningEventNotFoundException(UUID id) {
    super("ModregningEvent not found: " + id);
  }
}
