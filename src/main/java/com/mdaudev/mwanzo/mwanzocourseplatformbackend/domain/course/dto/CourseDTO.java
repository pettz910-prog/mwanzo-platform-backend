package com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.course.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Course Data Transfer Object (List View)
 *
 * Lightweight DTO for course listings/catalog.
 * Contains essential information for course cards.
 * Does not include full description or all fields.
 *
 * @author Mwanzo Development Team
 * @version 1.0
 * @since 2026-01-09
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseDTO {

    private UUID id;
    private String title;
    private String slug;
    private String shortDescription;
    private String thumbnailUrl;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer discountPercentage;
    private String level;
    private String language;
    private Integer durationMinutes;
    private Integer lectureCount;
    private Integer enrollmentCount;
    private BigDecimal averageRating;
    private Integer ratingCount;
    private Boolean isFeatured;
    private Boolean isFree;

    // Category info (nested)
    private CategoryDTO category;

    // Instructor info (simplified - full instructor entity comes later)
    private UUID instructorId;
    private String instructorName;  // We'll populate this later from User entity
}