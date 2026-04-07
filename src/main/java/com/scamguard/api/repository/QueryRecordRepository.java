package com.scamguard.api.repository;

import com.scamguard.api.entity.QueryRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QueryRecordRepository extends JpaRepository<QueryRecord, Long> {
}