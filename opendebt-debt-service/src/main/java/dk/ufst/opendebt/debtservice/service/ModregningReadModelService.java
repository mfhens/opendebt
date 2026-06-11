package dk.ufst.opendebt.debtservice.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import dk.ufst.opendebt.debtservice.repository.ModregningEventRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ModregningReadModelService {

  private final ModregningEventRepository modregningEventRepository;
  private final ModregningResultMapper modregningResultMapper;

  public List<ModregningResult> listOperativeEvents(UUID debtorPersonId) {
    return modregningEventRepository
        .findByDebtorPersonIdAndOperativeTrue(debtorPersonId, PageRequest.of(0, 100))
        .stream()
        .map(event -> modregningResultMapper.toResult(event, List.of()))
        .toList();
  }
}
