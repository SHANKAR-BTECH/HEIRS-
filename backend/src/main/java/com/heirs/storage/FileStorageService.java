package com.heirs.storage;

import java.io.InputStream;
import org.springframework.core.io.Resource;

/**
 * Keys are opaque. Implementations must remove partial writes on failure; deletion is idempotent.
 */
public interface FileStorageService {
  String store(InputStream content);

  Resource load(String key);

  boolean exists(String key);

  void delete(String key);
}
