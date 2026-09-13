package com.heirs.repository;

import com.heirs.entity.Record;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RecordRepository
    extends JpaRepository<Record, Long>, JpaSpecificationExecutor<Record> {
  @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @org.springframework.data.jpa.repository.Query("select r from Record r where r.id = :id")
  Optional<Record> lockById(@org.springframework.data.repository.query.Param("id") Long id);

  Optional<Record> findByReferenceNumber(String referenceNumber);

  boolean existsByReferenceNumber(String referenceNumber);

  boolean existsByReferenceNumberAndIdNot(String referenceNumber, Long id);
}
