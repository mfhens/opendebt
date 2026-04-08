package dk.ufst.opendebt.caseworker.dto;

/** Portal DTO for a single position in the GIL § 4 payment application order. */
public record DaekningsraekkefoelgePositionDto(
    String fordringId,
    String fordringshaverId,
    String prioritetKategori,
    String gilParagraf,
    String komponent,
    String fifoSortKey,
    String modtagelsesdato,
    String tilbaestaaendeBeloeb,
    String opskrivningAfFordringId) {}
