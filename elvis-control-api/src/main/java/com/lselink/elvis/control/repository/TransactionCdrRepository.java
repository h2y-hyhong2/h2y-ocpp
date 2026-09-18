package com.lselink.elvis.control.repository;

import com.lselink.elvis.control.entity.TransactionCdrEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionCdrRepository extends JpaRepository<TransactionCdrEntity, Long> {
    List<TransactionCdrEntity> findAllByOrderByStartTimeDesc();

    @Query("SELECT COUNT(t), COALESCE(SUM(t.totalKwh), 0), COALESCE(SUM(t.totalAmount), 0) FROM TransactionCdrEntity t")
    Object[] getCdrAggregateSummary();
}
