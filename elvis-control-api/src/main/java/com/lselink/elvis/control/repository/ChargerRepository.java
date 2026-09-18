package com.lselink.elvis.control.repository;

import com.lselink.elvis.control.entity.ChargerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChargerRepository extends JpaRepository<ChargerEntity, String> {
    List<ChargerEntity> findByStIdOrderByCpIdAsc(String stId);
    List<ChargerEntity> findAllByOrderByChargeBoxIdAsc();
}
