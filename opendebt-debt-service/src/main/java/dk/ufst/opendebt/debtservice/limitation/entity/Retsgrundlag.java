package dk.ufst.opendebt.debtservice.limitation.entity;

public enum Retsgrundlag {
  ORDINARY,
  SPECIAL;

  public static Retsgrundlag fromContractValue(String value) {
    if (value == null) {
      return ORDINARY;
    }
    return switch (value.trim().toUpperCase()) {
      case "SPECIAL", "SAERLIGT_RETSGRUNDLAG" -> SPECIAL;
      default -> ORDINARY;
    };
  }
}
