package com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.course.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Update Course Request DTO
 *
 * Request body for updating an existing course.
 * All fields are optional - only provided fields will be updated.
 *
 * @author Mwanzo Development Team
 * @version 1.0
 * @since 2026-01-20
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCourseRequest {

    @Size(min = 10, max = 200, message = "Title must be between 10 and 200 characters")
    private String title;

    @Size(min = 50, max = 5000, message = "Description must be between 50 and 5000 characters")
    private String description;

    @Size(max = 300, message = "Short description cannot exceed 300 characters")
    private String shortDescription;

    private UUID categoryId;

    @DecimalMin(value = "0.0", message = "Price cannot be negative")
    private BigDecimal price;

    @DecimalMin(value = "0.0", message = "Original price cannot be negative")
    private BigDecimal originalPrice;

    @Pattern(regexp = "BEGINNER|INTERMEDIATE|ADVANCED|ALL_LEVELS",
            message = "Level must be BEGINNER, INTERMEDIATE, ADVANCED, or ALL_LEVELS")
    private String level;

    @Size(max = 50, message = "Language cannot exceed 50 characters")
    private String language;

    @Size(min = 1, max = 20, message = "Must have between 1 and 20 learning objectives")
    private List<String> learningObjectives;

    @Size(max = 20, message = "Cannot have more than 20 requirements")
    private List<String> requirements;

    @Pattern(regexp = "DRAFT|PUBLISHED|ARCHIVED",
            message = "Status must be DRAFT, PUBLISHED, or ARCHIVED")
    private String status;

    private String thumbnailUrl;

    private String previewVideoUrl;

    private Boolean isFeatured;
}