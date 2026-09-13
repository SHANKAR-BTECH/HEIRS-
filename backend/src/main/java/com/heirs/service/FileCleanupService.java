package com.heirs.service;

import com.heirs.entity.FileCleanup;
import com.heirs.repository.FileCleanupRepository;
import com.heirs.storage.FileStorageService;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.*;

@Service
@EnableScheduling
public class FileCleanupService {
  private final FileCleanupRepository repository;
  private final FileStorageService storage;
  private final TransactionTemplate independent;

  public FileCleanupService(
      FileCleanupRepository repository,
      FileStorageService storage,
      PlatformTransactionManager manager) {
    this.repository = repository;
    this.storage = storage;
    independent = new TransactionTemplate(manager);
    independent.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  public void afterCommit(String key) {
    repository.save(new FileCleanup(key));
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            clean(key);
          }
        });
  }

  public void onRollback(String key) {
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCompletion(int status) {
            if (status == STATUS_ROLLED_BACK) {
              try {
                storage.delete(key);
              } catch (RuntimeException failure) {
                LoggerFactory.getLogger(getClass())
                    .error("Rollback file cleanup failed; recording retry", failure);
                independent.executeWithoutResult(
                    s -> repository.saveAndFlush(new FileCleanup(key)));
              }
            }
          }
        });
  }

  private void clean(String key) {
    try {
      independent.executeWithoutResult(
          s -> {
            storage.delete(key);
            repository.deleteById(key);
          });
    } catch (RuntimeException e) {
      LoggerFactory.getLogger(getClass())
          .error("Document file cleanup pending; automatic retry will follow", e);
    }
  }

  @Scheduled(fixedDelayString = "${heirs.storage.cleanup-interval-ms:60000}")
  public void retryPending() {
    repository.findAll(PageRequest.of(0, 100)).forEach(job -> clean(job.getStorageKey()));
  }
}
