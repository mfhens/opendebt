package dk.ufst.opendebt.debtservice.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import dk.ufst.opendebt.debtservice.entity.NotificationEntity;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {

  List<NotificationEntity> findByDebtIdOrderBySentAtDesc(UUID debtId);

  List<NotificationEntity> findByRecipientPersonIdOrderBySentAtDesc(UUID recipientPersonId);

  List<NotificationEntity> findByDeliveryState(NotificationEntity.DeliveryState deliveryState);
}
