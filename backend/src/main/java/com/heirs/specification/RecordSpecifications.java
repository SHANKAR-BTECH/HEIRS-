package com.heirs.specification;

import com.heirs.dto.RecordSearchRequestDto;
import com.heirs.entity.Record;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

public final class RecordSpecifications {
  private RecordSpecifications() {}

  public static Specification<Record> matching(RecordSearchRequestDto filter) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();
      if (filter.q() != null && !filter.q().isBlank()) {
        String pattern = "%" + escapeLike(filter.q().trim().toLowerCase(Locale.ROOT)) + "%";
        predicates.add(
            cb.or(
                List.of("title", "description", "keywords", "department", "referenceNumber")
                    .stream()
                    .map(field -> cb.like(cb.lower(root.get(field)), pattern, '!'))
                    .toArray(Predicate[]::new)));
      }
      if (filter.category() != null)
        predicates.add(cb.equal(root.get("category"), filter.category()));
      if (filter.status() != null) predicates.add(cb.equal(root.get("status"), filter.status()));
      if (filter.year() != null)
        predicates.add(cb.equal(root.get("publicationYear"), filter.year()));
      if (filter.department() != null && !filter.department().isBlank())
        predicates.add(
            cb.equal(
                cb.lower(root.get("department")),
                filter.department().trim().toLowerCase(Locale.ROOT)));
      return cb.and(predicates.toArray(Predicate[]::new));
    };
  }

  private static String escapeLike(String input) {
    return input.replace("!", "!!").replace("%", "!%").replace("_", "!_");
  }
}
