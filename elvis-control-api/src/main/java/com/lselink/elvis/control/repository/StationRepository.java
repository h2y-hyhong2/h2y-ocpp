package com.lselink.elvis.control.repository;

import com.lselink.elvis.control.entity.StationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StationRepository extends JpaRepository<StationEntity, String> {
    List<StationEntity> findByCorpIdOrderByStIdAsc(String corpId);
    List<StationEntity> findAllByOrderByStIdAsc();
}
