package dk.ufst.opendebt.payment.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import dk.ufst.opendebt.payment.entity.PaymentEntity;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentEntity, UUID> {

  List<PaymentEntity> findByDebtId(UUID debtId);

  List<PaymentEntity> findByOcrLine(String ocrLine);

  List<PaymentEntity> findByCaseId(UUID caseId);
}
