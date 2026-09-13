package com.heirs.storage;

import com.heirs.exception.StorageException;
import java.io.*;
import java.nio.file.*;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.*;
import org.springframework.stereotype.Service;

@Service
public class LocalFileStorageService implements FileStorageService {
  private final Path root;

  public LocalFileStorageService(@Value("${heirs.storage.path}") String directory) {
    root = Path.of(directory).toAbsolutePath().normalize();
    try {
      Files.createDirectories(root);
    } catch (IOException e) {
      throw new StorageException(e);
    }
  }

  private Path resolve(String key) {
    if (key == null || !key.matches("[a-f0-9-]{36}\\.pdf"))
      throw new StorageException(new IllegalArgumentException("Invalid storage key"));
    Path path = root.resolve(key).normalize();
    if (!path.getParent().equals(root) || Files.isSymbolicLink(path))
      throw new StorageException(new IllegalArgumentException("Invalid storage key"));
    return path;
  }

  public String store(InputStream content) {
    String key = UUID.randomUUID() + ".pdf";
    Path path = resolve(key);
    try (content) {
      Files.copy(content, path);
      return key;
    } catch (IOException e) {
      try {
        Files.deleteIfExists(path);
      } catch (IOException cleanup) {
        e.addSuppressed(cleanup);
      }
      throw new StorageException(e);
    }
  }

  public Resource load(String key) {
    return new FileSystemResource(resolve(key));
  }

  public boolean exists(String key) {
    return Files.isRegularFile(resolve(key), LinkOption.NOFOLLOW_LINKS);
  }

  public void delete(String key) {
    try {
      Files.deleteIfExists(resolve(key));
    } catch (IOException e) {
      throw new StorageException(e);
    }
  }
}
