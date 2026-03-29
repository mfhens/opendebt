package dk.ufst.opendebt.payment.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dk.ufst.opendebt.payment.daekning.dto.DaekningsraekkefoelgePositionDto;
import dk.ufst.opendebt.payment.daekning.dto.SimulatePositionDto;
import dk.ufst.opendebt.payment.daekning.dto.SimulateRequestDto;
import dk.ufst.opendebt.payment.daekning.service.DaekningsRaekkefoeigenService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/debtors/{debtorId}/daekningsraekkefoelge")
@Tag(name = "Dækningsrækkefølge", description = "GIL § 4 payment application order")
@RequiredArgsConstructor
public class DaekningsRaekkefoeigenController {

  private final DaekningsRaekkefoeigenService service;

  @GetMapping
  @PreAuthorize("hasRole('SAGSBEHANDLER') or hasRole('ADMIN') or hasRole('SERVICE')")
  public ResponseEntity<List<DaekningsraekkefoelgePositionDto>> getOrdering(
      @PathVariable String debtorId,
      @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate asOf) {
    List<DaekningsraekkefoelgePositionDto> result = service.getOrdering(debtorId, asOf);
    if (result.isEmpty()) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(result);
  }

  @PostMapping("/simulate")
  @PreAuthorize("hasRole('SAGSBEHANDLER') or hasRole('ADMIN') or hasRole('SERVICE')")
  public ResponseEntity<List<SimulatePositionDto>> simulate(
      @PathVariable String debtorId,
      @Valid @RequestBody SimulateRequestDto request) {
    return ResponseEntity.ok(
        service.simulate(
            debtorId, request.beloeb(), request.inddrivelsesindsatsType(), Instant.now()));
  }
}
