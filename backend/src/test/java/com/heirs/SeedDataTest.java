package com.heirs;

import static org.assertj.core.api.Assertions.*;

import com.heirs.config.RecordSeeder;
import com.heirs.repository.RecordRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "heirs.seed.enabled=true")
@ActiveProfiles(resolver = TestDatabaseProfileResolver.class)
@Transactional
class SeedDataTest {
  @Autowired RecordRepository repository;
  @Autowired RecordSeeder seeder;

  @Test
  void seedsAllCategoriesAndDoesNotOverwriteExistingReferences() throws Exception {
    assertThat(repository.count()).isEqualTo(60);
    assertThat(repository.findAll())
        .extracting(r -> r.getCategory())
        .containsAll(java.util.List.of(com.heirs.entity.Category.values()));
    var record = repository.findByReferenceNumber("HEIRS/POL/2026/014").orElseThrow();
    record.setTitle("Locally revised title");
    repository.flush();
    seeder.run();
    assertThat(repository.count()).isEqualTo(60);
    assertThat(repository.findByReferenceNumber("HEIRS/POL/2026/014").orElseThrow().getTitle())
        .isEqualTo("Locally revised title");
  }
}
