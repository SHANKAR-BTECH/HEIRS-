package com.heirs.config;

import com.heirs.entity.Category;
import com.heirs.entity.RecordStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class WebConfig implements WebMvcConfigurer {
  private final String[] origins;

  public WebConfig(@Value("${heirs.cors.allowed-origins}") String[] origins) {
    this.origins = origins;
  }

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry
        .addMapping("/api/**")
        .allowedOrigins(origins)
        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
        .allowedHeaders("Content-Type", "Accept")
        .exposedHeaders("Location")
        .maxAge(3600);
  }

  @Override
  public void addFormatters(FormatterRegistry registry) {
    registry.addConverter(String.class, Category.class, Category::fromValue);
    registry.addConverter(String.class, RecordStatus.class, RecordStatus::fromValue);
  }
}
