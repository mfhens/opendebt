package dk.ufst.opendebt.debtservice.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import dk.ufst.opendebt.debtservice.entity.DebtTypeEntity;

@Repository
public interface DebtTypeRepository extends JpaRepository<DebtTypeEntity, String> {

  Optional<DebtTypeEntity> findByCode(String code);
}
