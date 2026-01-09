package com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.course.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Category Data Transfer Object
 *
 * Used for API responses when returning category information.
 * Keeps internal entity structure hidden from clients.
 *
 * @author Mwanzo Development Team
 * @version 1.0
 * @since 2026-01-09
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryDTO {

    private UUID id;
    private String name;
    private String description;
    private String slug;
    private Integer courseCount;
    private LocalDateTime createdAt;
}