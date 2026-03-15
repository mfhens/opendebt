package dk.ufst.opendebt.creditor.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import dk.ufst.opendebt.creditor.dto.FordringFormDto;
import dk.ufst.opendebt.creditor.dto.PortalDebtDto;

class FordringMapperTest {

  private final FordringMapper mapper = new FordringMapper();

  @Test
  void toDebtRequest_mapsAllFields() {
    UUID debtorId = UUID.randomUUID();
    UUID creditorOrgId = UUID.randomUUID();
    LocalDate dueDate = LocalDate.now().plusDays(30);

    FordringFormDto form =
        FordringFormDto.builder()
            .debtorPersonId(debtorId)
            .principalAmount(new BigDecimal("5000.50"))
            .debtTypeCode("SKAT")
            .dueDate(dueDate)
            .description("Restgæld")
            .build();

    PortalDebtDto result = mapper.toDebtRequest(form, creditorOrgId);

    assertThat(result.getDebtorPersonId()).isEqualTo(debtorId);
    assertThat(result.getCreditorOrgId()).isEqualTo(creditorOrgId);
    assertThat(result.getPrincipalAmount()).isEqualByComparingTo("5000.50");
    assertThat(result.getDebtTypeCode()).isEqualTo("SKAT");
    assertThat(result.getDueDate()).isEqualTo(dueDate);
    assertThat(result.getDescription()).isEqualTo("Restgæld");
  }

  @Test
  void toDebtRequest_handlesNullDescription() {
    FordringFormDto form =
        FordringFormDto.builder()
            .debtorPersonId(UUID.randomUUID())
            .principalAmount(new BigDecimal("100.00"))
            .debtTypeCode("AFGIFT")
            .dueDate(LocalDate.now().plusDays(10))
            .build();

    PortalDebtDto result = mapper.toDebtRequest(form, UUID.randomUUID());

    assertThat(result.getDescription()).isNull();
  }
}
