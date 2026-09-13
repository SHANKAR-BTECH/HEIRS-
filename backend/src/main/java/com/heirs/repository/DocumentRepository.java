package com.heirs.repository;

import com.heirs.entity.Document;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, Long> {
  List<Document> findAllByRecordIdOrderByIdAsc(Long recordId);
}
