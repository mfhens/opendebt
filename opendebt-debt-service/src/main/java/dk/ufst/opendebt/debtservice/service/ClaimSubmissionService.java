package dk.ufst.opendebt.debtservice.service;

import dk.ufst.opendebt.common.dto.DebtDto;
import dk.ufst.opendebt.debtservice.dto.ClaimSubmissionResponse;

/**
 * Orchestrates the full claim submission flow: validation, debt creation, and case assignment.
 * Returns UDFOERT, AFVIST, or HOERING outcome per PSRM specification.
 */
public interface ClaimSubmissionService {

  ClaimSubmissionResponse submitClaim(DebtDto claim);
}
