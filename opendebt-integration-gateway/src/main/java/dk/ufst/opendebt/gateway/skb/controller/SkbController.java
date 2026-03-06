package dk.ufst.opendebt.gateway.skb.controller;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import dk.ufst.opendebt.gateway.skb.model.DebitAdvice;
import dk.ufst.opendebt.gateway.skb.model.SkbMessage;
import dk.ufst.opendebt.gateway.skb.service.SkbEdifactService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/skb")
@Tag(name = "SKB", description = "Statens Koncernbetalinger CREMUL/DEBMUL integration")
@RequiredArgsConstructor
public class SkbController {

  private final SkbEdifactService skbEdifactService;

  @PostMapping(value = "/cremul/parse", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Operation(summary = "Parse a CREMUL file from SKB")
  @PreAuthorize("hasRole('SYSTEM') or hasRole('ADMIN')")
  public ResponseEntity<SkbMessage> parseCremul(@RequestParam("file") MultipartFile file)
      throws IOException {
    try (InputStream stream = file.getInputStream()) {
      SkbMessage message = skbEdifactService.parseCremul(stream);
      return ResponseEntity.ok(message);
    }
  }

  @PostMapping(value = "/debmul/generate", produces = "application/edifact")
  @Operation(summary = "Generate a DEBMUL file for SKB")
  @PreAuthorize("hasRole('SYSTEM') or hasRole('ADMIN')")
  public ResponseEntity<byte[]> generateDebmul(@RequestBody List<DebitAdvice> debitAdvices) {
    byte[] debmul = skbEdifactService.generateDebmul(debitAdvices);
    return ResponseEntity.ok()
        .header("Content-Disposition", "attachment; filename=\"debmul.edi\"")
        .body(debmul);
  }
}
