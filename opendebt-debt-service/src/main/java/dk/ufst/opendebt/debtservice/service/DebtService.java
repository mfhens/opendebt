package dk.ufst.opendebt.debtservice.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import dk.ufst.opendebt.common.dto.DebtDto;

public interface DebtService {

  Page<DebtDto> listDebts(
      String creditorId,
      String debtorId,
      DebtDto.DebtStatus status,
      DebtDto.ReadinessStatus readinessStatus,
      Pageable pageable);

  DebtDto getDebtById(UUID id);

  List<DebtDto> getDebtsByDebtor(String debtorId);

  List<DebtDto> getDebtsByCreditor(String creditorId);

  DebtDto createDebt(DebtDto debtDto);

  DebtDto updateDebt(UUID id, DebtDto debtDto);

  void cancelDebt(UUID id);

  List<String> getDebtTypes();

  List<DebtDto> findByOcrLine(String ocrLine);

  DebtDto writeDown(UUID id, java.math.BigDecimal amount);
}
