package com.lselink.elvis.control.repository;

import com.lselink.elvis.control.entity.CorpEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CorpRepository extends JpaRepository<CorpEntity, String> {
    List<CorpEntity> findByUseYnOrderByCorpIdAsc(String useYn);
}
