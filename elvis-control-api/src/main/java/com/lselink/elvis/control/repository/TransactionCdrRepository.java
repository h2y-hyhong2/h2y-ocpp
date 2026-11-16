package com.lselink.elvis.control.repository;

import com.lselink.elvis.control.entity.TransactionCdrEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionCdrRepository extends JpaRepository<TransactionCdrEntity, Long>, TransactionCdrRepositoryCustom {
}
