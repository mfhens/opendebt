package dk.ufst.opendebt.personregistry.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import dk.ufst.opendebt.personregistry.entity.OrganizationEntity;

@Repository
public interface OrganizationRepository extends JpaRepository<OrganizationEntity, UUID> {

  Optional<OrganizationEntity> findByCvrHash(String cvrHash);

  boolean existsByCvrHash(String cvrHash);
}
