package dk.ufst.opendebt.debtservice.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import dk.ufst.opendebt.debtservice.entity.NotificationOutboxEntity;

@Repository
public interface NotificationOutboxRepository
    extends JpaRepository<NotificationOutboxEntity, UUID> {}
