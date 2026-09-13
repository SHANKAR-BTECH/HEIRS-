package com.heirs.repository;

import com.heirs.entity.Record;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RecordRepository
    extends JpaRepository<Record, Long>, JpaSpecificationExecutor<Record> {
  Optional<Record> findByReferenceNumber(String referenceNumber);

  boolean existsByReferenceNumber(String referenceNumber);

  boolean existsByReferenceNumberAndIdNot(String referenceNumber, Long id);
}
