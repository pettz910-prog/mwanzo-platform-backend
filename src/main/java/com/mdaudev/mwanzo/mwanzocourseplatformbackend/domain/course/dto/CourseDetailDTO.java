package com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.course.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Course Detail Data Transfer Object
 *
 * Complete DTO for single course detail page.
 * Contains all course information including full description,
 * learning objectives, requirements, etc.
 *
 * @author Mwanzo Development Team
 * @version 1.0
 * @since 2026-01-09
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseDetailDTO {

    private UUID id;
    private String title;
    private String slug;
    private String description;
    private String shortDescription;
    private List<String> learningObjectives;
    private List<String> requirements;
    private String thumbnailUrl;
    private String previewVideoUrl;

    // Pricing
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer discountPercentage;
    private Boolean isFree;

    // Course metadata
    private String level;
    private String language;
    private String status;
    private Integer durationMinutes;
    private Integer lectureCount;

    // Engagement metrics
    private Integer enrollmentCount;
    private BigDecimal averageRating;
    private Integer ratingCount;
    private Boolean isFeatured;
    private Boolean isPublished;

    // Category
    private CategoryDTO category;

    // Instructor (simplified for now)
    private UUID instructorId;
    private String instructorName;
    private String instructorBio;

    // Timestamps
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}