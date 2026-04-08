package dk.ufst.opendebt.debtservice.entity;

public enum ClaimCategory {
  HF, // Main claim (hovedfordring)
  UF, // Sub-claim (underfordring)
  RENTE // Interest claim (opkrævningsrente) — FR-2 / G.A.1.4.3
}
