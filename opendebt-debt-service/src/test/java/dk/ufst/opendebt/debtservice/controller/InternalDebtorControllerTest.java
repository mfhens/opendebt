package dk.ufst.opendebt.debtservice.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dk.ufst.opendebt.debtservice.dto.ActiveFordringResponseDto;
import dk.ufst.opendebt.debtservice.service.ActiveFordringService;

@ExtendWith(MockitoExtension.class)
class InternalDebtorControllerTest {

  @Mock private ActiveFordringService activeFordringService;

  private InternalDebtorController controller;

  @BeforeEach
  void setUp() {
    controller = new InternalDebtorController(activeFordringService);
  }

  @Test
  void getActiveFordringer_delegatesToServiceAndReturns200() {
    UUID debtorId = UUID.randomUUID();
    List<ActiveFordringResponseDto> expected = List.of(activeFordring(debtorId));
    when(activeFordringService.getActiveFordringer(debtorId)).thenReturn(expected);

    var response = controller.getActiveFordringer(debtorId);

    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getBody()).isSameAs(expected);
    verify(activeFordringService).getActiveFordringer(debtorId);
  }

  @Test
  void getActiveFordringer_returnsEmptyListWhenNoActiveFordringer() {
    UUID debtorId = UUID.randomUUID();
    when(activeFordringService.getActiveFordringer(debtorId)).thenReturn(List.of());

    var response = controller.getActiveFordringer(debtorId);

    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getBody()).isNotNull().isEmpty();
  }

  @Test
  void getActiveFordringer_returnsMulipleFordringer() {
    UUID debtorId = UUID.randomUUID();
    List<ActiveFordringResponseDto> expected =
        List.of(activeFordring(debtorId), activeFordring(debtorId));
    when(activeFordringService.getActiveFordringer(debtorId)).thenReturn(expected);

    var response = controller.getActiveFordringer(debtorId);

    assertThat(response.getBody()).hasSize(2);
  }

  // ── helpers ────────────────────────────────────────────────────────────────

  private ActiveFordringResponseDto activeFordring(UUID debtorId) {
    return ActiveFordringResponseDto.builder()
        .fordringId(UUID.randomUUID())
        .fordringType("600")
        .beloebResterende(new BigDecimal("5000.00"))
        .opkraevningsrenter(BigDecimal.ZERO)
        .inddrivelsesrenterFordringshaver(BigDecimal.ZERO)
        .inddrivelsesrenterFoerTilbagefoersel(BigDecimal.ZERO)
        .inddrivelsesrenterStk1(BigDecimal.ZERO)
        .oevrigeRenterPsrm(BigDecimal.ZERO)
        .inddrivelsesomkostninger(BigDecimal.ZERO)
        .sekvensNummer(1)
        .inLoenindeholdelsesIndsats(false)
        .opskrivningAfFordringId(null)
        .fordringshaverId(UUID.randomUUID())
        .gilParagraf("GIL § 4, stk. 1")
        .applicationTimestamp(OffsetDateTime.now())
        .build();
  }
}
