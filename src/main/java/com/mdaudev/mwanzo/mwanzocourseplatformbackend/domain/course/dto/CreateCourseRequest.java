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
 * Create Course Request DTO
 *
 * Request body for creating a new course.
 * Contains validation rules for all required fields.
 *
 * @author Mwanzo Development Team
 * @version 1.0
 * @since 2026-01-09
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCourseRequest {

    @NotBlank(message = "Course title is required")
    @Size(min = 10, max = 200, message = "Title must be between 10 and 200 characters")
    private String title;

    @NotBlank(message = "Course description is required")
    @Size(min = 50, max = 5000, message = "Description must be between 50 and 5000 characters")
    private String description;

    @Size(max = 300, message = "Short description cannot exceed 300 characters")
    private String shortDescription;

    @NotNull(message = "Category is required")
    private UUID categoryId;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", message = "Price cannot be negative")
    private BigDecimal price;

    @DecimalMin(value = "0.0", message = "Original price cannot be negative")
    private BigDecimal originalPrice;

    @NotNull(message = "Course level is required")
    @Pattern(regexp = "BEGINNER|INTERMEDIATE|ADVANCED|ALL_LEVELS",
            message = "Level must be BEGINNER, INTERMEDIATE, ADVANCED, or ALL_LEVELS")
    private String level;

    @NotBlank(message = "Language is required")
    @Size(max = 50, message = "Language cannot exceed 50 characters")
    private String language;

    @NotEmpty(message = "Learning objectives are required")
    @Size(min = 1, max = 20, message = "Must have between 1 and 20 learning objectives")
    private List<String> learningObjectives;

    @Size(max = 20, message = "Cannot have more than 20 requirements")
    private List<String> requirements;
}