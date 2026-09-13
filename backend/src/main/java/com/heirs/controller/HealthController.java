package com.heirs.controller;

import java.util.Map;
import javax.sql.DataSource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class HealthController {
  private final DataSource dataSource;

  public HealthController(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @GetMapping("/api/health")
  public ResponseEntity<Map<String, String>> health() {
    try (var connection = dataSource.getConnection()) {
      if (connection.isValid(2)) return ResponseEntity.ok(Map.of("status", "UP", "database", "UP"));
    } catch (java.sql.SQLException ignored) {
      /* Report readiness without exposing database details. */
    }
    return ResponseEntity.status(503).body(Map.of("status", "DOWN", "database", "DOWN"));
  }
}
