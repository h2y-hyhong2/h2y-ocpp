package com.lselink.elvis.control.repository;

import com.lselink.elvis.control.entity.ConnectorStatusEntity;
import com.lselink.elvis.control.entity.ConnectorStatusId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConnectorStatusRepository extends JpaRepository<ConnectorStatusEntity, ConnectorStatusId> {
    List<ConnectorStatusEntity> findByIdChargeBoxId(String chargeBoxId);
}
