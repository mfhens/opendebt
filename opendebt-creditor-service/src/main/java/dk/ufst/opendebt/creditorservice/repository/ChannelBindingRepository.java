package dk.ufst.opendebt.creditorservice.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import dk.ufst.opendebt.creditorservice.dto.ChannelType;
import dk.ufst.opendebt.creditorservice.entity.ChannelBindingEntity;

@Repository
public interface ChannelBindingRepository extends JpaRepository<ChannelBindingEntity, UUID> {

  Optional<ChannelBindingEntity> findByChannelIdentityAndActiveTrue(String channelIdentity);

  Optional<ChannelBindingEntity> findByChannelIdentity(String channelIdentity);

  List<ChannelBindingEntity> findByCreditorIdAndActiveTrue(UUID creditorId);

  List<ChannelBindingEntity> findByCreditorId(UUID creditorId);

  List<ChannelBindingEntity> findByChannelTypeAndActiveTrue(ChannelType channelType);

  boolean existsByChannelIdentity(String channelIdentity);
}
