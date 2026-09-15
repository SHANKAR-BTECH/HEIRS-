package com.heirs.desktop.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Category representation matching GET /api/categories.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CategoryResponse(
        String code,
        String name
) {}
