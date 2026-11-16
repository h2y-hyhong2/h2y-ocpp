package com.lselink.elvis.control.repository;

import com.lselink.elvis.control.entity.ConnectorStatusEntity;
import com.lselink.elvis.control.entity.ConnectorStatusId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConnectorStatusRepository extends JpaRepository<ConnectorStatusEntity, ConnectorStatusId> {
    List<ConnectorStatusEntity> findByChargeBoxId(String chargeBoxId);
    List<ConnectorStatusEntity> findByChargeBoxIdIn(Collection<String> chargeBoxIds);
    Optional<ConnectorStatusEntity> findByChargeBoxIdAndConnectorId(String chargeBoxId, Integer connectorId);
}
