package com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.course.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Enrollment Data Transfer Object
 *
 * Response DTO for enrollment information.
 * Includes course details and progress tracking.
 *
 * @author Mwanzo Development Team
 * @version 1.0
 * @since 2026-01-09
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnrollmentDTO {

    private UUID id;
    private UUID studentId;
    private UUID courseId;

    // Course details (denormalized for convenience)
    private String courseTitle;
    private String courseSlug;
    private String courseThumbnailUrl;
    private String instructorName;

    private BigDecimal pricePaid;
    private String status;

    // Progress tracking
    private Integer progressPercentage;
    private Boolean videosCompleted;
    private Boolean quizzesCompleted;
    private Boolean isCompleted;
    private LocalDateTime completedAt;

    private UUID certificateId;
    private LocalDateTime lastAccessedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
}