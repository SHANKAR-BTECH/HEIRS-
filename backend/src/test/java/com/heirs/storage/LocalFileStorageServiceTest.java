package com.heirs.storage;

import static org.assertj.core.api.Assertions.*;

import com.heirs.exception.StorageException;
import java.io.*;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalFileStorageServiceTest {
  @TempDir Path directory;

  @Test
  void storesUniqueKeysAndPersistsAcrossServiceRestart() throws Exception {
    var storage = new LocalFileStorageService(directory.toString());
    String first = storage.store(new ByteArrayInputStream("first".getBytes()));
    String second = storage.store(new ByteArrayInputStream("second".getBytes()));
    assertThat(first).isNotEqualTo(second);
    var restarted = new LocalFileStorageService(directory.toString());
    assertThat(restarted.load(first).getContentAsString(java.nio.charset.StandardCharsets.UTF_8))
        .isEqualTo("first");
    restarted.delete(first);
    restarted.delete(first);
    assertThat(restarted.exists(first)).isFalse();
    assertThat(restarted.exists(second)).isTrue();
  }

  @Test
  void rejectsTraversalAndAbsoluteKeys() {
    var storage = new LocalFileStorageService(directory.toString());
    for (String key :
        new String[] {"../secret.pdf", "C:\\secret.pdf", "/secret.pdf", "policy.pdf"}) {
      assertThatThrownBy(() -> storage.load(key)).isInstanceOf(StorageException.class);
      assertThatThrownBy(() -> storage.delete(key)).isInstanceOf(StorageException.class);
    }
  }

  @Test
  void failedWriteRemovesPartialFile() throws Exception {
    var storage = new LocalFileStorageService(directory.toString());
    assertThatThrownBy(
            () ->
                storage.store(
                    new InputStream() {
                      int reads;

                      public int read() throws IOException {
                        if (reads++ > 5) throw new IOException("interrupted");
                        return 1;
                      }
                    }))
        .isInstanceOf(StorageException.class);
    try (var files = Files.list(directory)) {
      assertThat(files.count()).isZero();
    }
  }
}
