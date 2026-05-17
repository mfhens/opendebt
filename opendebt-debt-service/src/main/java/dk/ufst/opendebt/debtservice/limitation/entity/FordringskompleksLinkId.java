package dk.ufst.opendebt.debtservice.limitation.entity;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FordringskompleksLinkId implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  private UUID kompleksId;
  private UUID fordringId;
}
