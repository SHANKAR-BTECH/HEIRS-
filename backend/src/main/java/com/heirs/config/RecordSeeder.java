package com.heirs.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.heirs.dto.CreateRecordRequestDto;
import com.heirs.mapper.RecordMapper;
import com.heirs.repository.RecordRepository;
import jakarta.validation.Validator;
import java.util.List;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "heirs.seed.enabled", havingValue = "true")
public class RecordSeeder implements CommandLineRunner {
  private final RecordRepository repository;
  private final RecordMapper mapper;
  private final ObjectMapper json;
  private final Validator validator;

  public RecordSeeder(
      RecordRepository repository, RecordMapper mapper, ObjectMapper json, Validator validator) {
    this.repository = repository;
    this.mapper = mapper;
    this.json = json;
    this.validator = validator;
  }

  @Override
  @Transactional
  public void run(String... args) throws Exception {
    int inserted = 0;
    try (var input = new ClassPathResource("seed/records.json").getInputStream()) {
      for (CreateRecordRequestDto dto :
          json.readValue(input, new TypeReference<List<CreateRecordRequestDto>>() {})) {
        var violations = validator.validate(dto);
        if (!violations.isEmpty())
          throw new jakarta.validation.ConstraintViolationException(violations);
        if (!repository.existsByReferenceNumber(dto.referenceNumber())) {
          repository.save(mapper.toEntity(dto));
          inserted++;
        }
      }
    }
    LoggerFactory.getLogger(RecordSeeder.class)
        .info("Inserted {} HEIRS seed records; existing references preserved", inserted);
  }
}
