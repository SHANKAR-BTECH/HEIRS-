package com.heirs.controller;

import com.heirs.dto.CategoryResponseDto;
import com.heirs.service.RecordService;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {
  private final RecordService service;

  public CategoryController(RecordService service) {
    this.service = service;
  }

  @GetMapping
  public List<CategoryResponseDto> getCategories() {
    return service.getCategories();
  }
}
