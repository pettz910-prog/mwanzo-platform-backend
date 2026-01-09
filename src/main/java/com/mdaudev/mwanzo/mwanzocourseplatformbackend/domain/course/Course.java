package com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.course;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Course Entity
 *
 * Represents a complete course offering on the platform.
 * Contains all course metadata, pricing, instructor info, and status tracking.
 *
 * Database Table: courses
 *
 * Business Rules:
 * - Course must belong to a category
 * - Price must be >= 0 (free courses allowed)
 * - Must be approved by admin before going live
 * - Instructor ID tracks course owner
 * - Rating calculated from student reviews
 * - Enrollment count tracks popularity
 *
 * @author Mwanzo Development Team
 * @version 1.0
 * @since 2026-01-09
 */
@Entity
@Table(name = "courses", indexes = {
        @Index(name = "idx_course_status", columnList = "status"),
        @Index(name = "idx_course_category", columnList = "category_id"),
        @Index(name = "idx_course_instructor", columnList = "instructor_id"),
        @Index(name = "idx_course_featured", columnList = "isFeatured"),
        @Index(name = "idx_course_created", columnList = "createdAt")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Course {

    /**
     * Unique identifier for the course.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * Course title/name.
     * Must be descriptive and unique per instructor.
     */
    @NotBlank(message = "Course title is required")
    @Size(min = 10, max = 200, message = "Course title must be between 10 and 200 characters")
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    /**
     * URL-friendly slug for the course (e.g., "digital-marketing-masterclass").
     * Used in URLs: /courses/{slug}
     */
    @Column(name = "slug", unique = true, length = 250)
    private String slug;

    /**
     * Comprehensive course description.
     * Explains what students will learn, prerequisites, etc.
     */
    @NotBlank(message = "Course description is required")
    @Size(min = 50, max = 5000, message = "Description must be between 50 and 5000 characters")
    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    /**
     * Short summary for course cards and listings (1-2 sentences).
     */
    @Size(max = 300, message = "Short description cannot exceed 300 characters")
    @Column(name = "short_description", length = 300)
    private String shortDescription;

    /**
     * What students will learn/achieve (learning outcomes).
     * Stored as JSON array of strings.
     * Example: ["Build REST APIs", "Deploy to AWS", "Write unit tests"]
     */
    @Column(name = "learning_objectives", columnDefinition = "TEXT")
    private String learningObjectives;

    /**
     * Course requirements/prerequisites.
     * Stored as JSON array of strings.
     * Example: ["Basic programming knowledge", "Computer with internet"]
     */
    @Column(name = "requirements", columnDefinition = "TEXT")
    private String requirements;

    /**
     * Category this course belongs to.
     * Many courses can belong to one category.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    /**
     * Instructor who created this course.
     * Stored as UUID reference to instructor profile.
     * We'll create the full User/Instructor entity later.
     */
    @Column(name = "instructor_id", nullable = false)
    private UUID instructorId;

    /**
     * Course price in KSH (Kenyan Shillings).
     * Must be >= 0 (free courses allowed).
     */
    @NotNull(message = "Course price is required")
    @DecimalMin(value = "0.0", message = "Price cannot be negative")
    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /**
     * Original price (for displaying discounts).
     * If null, no discount is shown.
     */
    @DecimalMin(value = "0.0", message = "Original price cannot be negative")
    @Column(name = "original_price", precision = 10, scale = 2)
    private BigDecimal originalPrice;

    /**
     * Course thumbnail/cover image URL.
     * Stored in S3, URL saved here.
     */
    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    /**
     * Preview video URL (free sample for potential students).
     * Stored in S3, URL saved here.
     */
    @Column(name = "preview_video_url", length = 500)
    private String previewVideoUrl;

    /**
     * Course difficulty level.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "level", nullable = false, length = 20)
    @Builder.Default
    private CourseLevel level = CourseLevel.BEGINNER;

    /**
     * Primary language of course content.
     */
    @Column(name = "language", length = 50)
    @Builder.Default
    private String language = "English";

    /**
     * Course approval and publishing status.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private CourseStatus status = CourseStatus.DRAFT;

    /**
     * Total duration in minutes (calculated from video lengths).
     */
    @Column(name = "duration_minutes")
    @Builder.Default
    private Integer durationMinutes = 0;

    /**
     * Total number of lectures/videos in course.
     */
    @Column(name = "lecture_count")
    @Builder.Default
    private Integer lectureCount = 0;

    /**
     * Total number of students enrolled.
     * Denormalized for performance.
     */
    @Column(name = "enrollment_count")
    @Builder.Default
    private Integer enrollmentCount = 0;

    /**
     * Average rating from student reviews (0.0 to 5.0).
     * Recalculated when new reviews are submitted.
     */
    @DecimalMin(value = "0.0")
    @DecimalMax(value = "5.0")
    @Column(name = "average_rating", precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal averageRating = BigDecimal.ZERO;

    /**
     * Total number of ratings/reviews.
     */
    @Column(name = "rating_count")
    @Builder.Default
    private Integer ratingCount = 0;

    /**
     * Whether course is featured on homepage.
     * Manually set by admin for high-quality courses.
     */
    @Column(name = "is_featured")
    @Builder.Default
    private Boolean isFeatured = false;

    /**
     * Whether course is published and visible to students.
     */
    @Column(name = "is_published")
    @Builder.Default
    private Boolean isPublished = false;

    /**
     * When course was published (null if never published).
     */
    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    /**
     * Admin who approved the course (null if not yet approved).
     */
    @Column(name = "approved_by")
    private UUID approvedBy;

    /**
     * When course was approved.
     */
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    /**
     * Admin rejection reason (if status = REJECTED).
     */
    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    /**
     * Timestamp when course was created.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when course was last updated.
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Generate URL slug from course title before persisting.
     */
    @PrePersist
    public void generateSlug() {
        if (this.slug == null && this.title != null) {
            this.slug = this.title
                    .toLowerCase()
                    .trim()
                    .replaceAll("\\s+", "-")
                    .replaceAll("[^a-z0-9-]", "")
                    .replaceAll("-+", "-")
                    .substring(0, Math.min(this.title.length(), 200));
        }
    }

    /**
     * Check if course is free.
     */
    public boolean isFree() {
        return this.price.compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * Check if course has a discount.
     */
    public boolean hasDiscount() {
        return this.originalPrice != null &&
                this.originalPrice.compareTo(this.price) > 0;
    }

    /**
     * Calculate discount percentage.
     */
    public Integer getDiscountPercentage() {
        if (!hasDiscount()) return 0;

        BigDecimal discount = this.originalPrice.subtract(this.price);
        BigDecimal percentage = discount
                .divide(this.originalPrice, 2, BigDecimal.ROUND_HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        return percentage.intValue();
    }

    /**
     * Increment enrollment count when student enrolls.
     */
    public void incrementEnrollmentCount() {
        this.enrollmentCount++;
    }

    /**
     * Update rating when new review is submitted.
     */
    public void updateRating(BigDecimal newRating) {
        if (this.ratingCount == 0) {
            this.averageRating = newRating;
            this.ratingCount = 1;
        } else {
            BigDecimal totalRating = this.averageRating
                    .multiply(BigDecimal.valueOf(this.ratingCount))
                    .add(newRating);
            this.ratingCount++;
            this.averageRating = totalRating
                    .divide(BigDecimal.valueOf(this.ratingCount), 2, BigDecimal.ROUND_HALF_UP);
        }
    }

    /**
     * Publish the course (make visible to students).
     */
    public void publish() {
        this.isPublished = true;
        this.publishedAt = LocalDateTime.now();
        this.status = CourseStatus.PUBLISHED;
    }

    /**
     * Unpublish the course (hide from students).
     */
    public void unpublish() {
        this.isPublished = false;
        this.status = CourseStatus.DRAFT;
    }
}

