package com.heirs.repository;

import com.heirs.entity.FileCleanup;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileCleanupRepository extends JpaRepository<FileCleanup, String> {}
