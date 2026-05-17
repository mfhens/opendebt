package dk.ufst.opendebt.wagegarnishment.limitation;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/internal/v1/limitation-facts")
@Tag(
    name = "Wage garnishment limitation facts",
    description = "Internal fact contract for petition059")
public class WageGarnishmentLimitationFactsController {

  @GetMapping("/debtors/{debtorPersonId}")
  @PreAuthorize("hasRole('SERVICE') or hasRole('ADMIN') or hasRole('CASEWORKER')")
  @Operation(summary = "Get limitation facts for wage garnishment decisions")
  public ResponseEntity<WageGarnishmentLimitationFacts> getFacts(
      @PathVariable UUID debtorPersonId, @RequestParam(required = false) LocalDate asOfDate) {
    return ResponseEntity.ok(
        WageGarnishmentLimitationFacts.builder()
            .debtorPersonId(debtorPersonId)
            .decisionRegistered(false)
            .coveredFordringIds(List.of())
            .build());
  }
}
