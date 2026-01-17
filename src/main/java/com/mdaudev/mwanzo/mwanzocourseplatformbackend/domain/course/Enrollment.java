package com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.course;

import jakarta.persistence.*;
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
 * Enrollment Entity
 *
 * Represents a student's enrollment in a course.
 * Created when a student purchases/enrolls in a course.
 * Tracks progress, completion status, and access details.
 *
 * Database Table: enrollments
 *
 * Business Rules:
 * - One student can enroll in a course only once
 * - Enrollment is created after successful payment
 * - Tracks completion percentage and certificate status
 * - Stores price paid (for revenue tracking)
 *
 * @author Mwanzo Development Team
 * @version 1.0
 * @since 2026-01-09
 */
@Entity
@Table(name = "enrollments", indexes = {
        @Index(name = "idx_enrollment_student", columnList = "student_id"),
        @Index(name = "idx_enrollment_course", columnList = "course_id"),
        @Index(name = "idx_enrollment_status", columnList = "status")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_student_course", columnNames = {"student_id", "course_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Enrollment {

    /**
     * Unique identifier for the enrollment.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * Student who enrolled.
     * Reference to User entity (student role).
     */
    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    /**
     * Course enrolled in.
     * Stored as UUID for microservices compatibility.
     * Course details fetched via CourseService when needed.
     */
    @Column(name = "course_id", nullable = false)
    private UUID courseId;

    /**
     * Price paid for the course (at time of purchase).
     * Stored for revenue tracking and refund calculations.
     */
    @Column(name = "price_paid", nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePaid;

    /**
     * Payment transaction ID.
     * Reference to payment record.
     */
    @Column(name = "payment_id")
    private UUID paymentId;

    /**
     * Enrollment status.
     * Starts as PENDING_PAYMENT, becomes ACTIVE after successful payment.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private EnrollmentStatus status = EnrollmentStatus.PENDING_PAYMENT;

    /**
     * Course progress percentage (0-100).
     * Calculated based on completed videos and quizzes.
     */
    @Column(name = "progress_percentage", nullable = false)
    @Builder.Default
    private Integer progressPercentage = 0;

    /**
     * Whether all videos have been watched.
     */
    @Column(name = "videos_completed", nullable = false)
    @Builder.Default
    private Boolean videosCompleted = false;

    /**
     * Whether all quizzes have been passed.
     */
    @Column(name = "quizzes_completed", nullable = false)
    @Builder.Default
    private Boolean quizzesCompleted = false;

    /**
     * Whether course has been fully completed.
     * Set to true when all videos watched and all quizzes passed.
     */
    @Column(name = "is_completed", nullable = false)
    @Builder.Default
    private Boolean isCompleted = false;

    /**
     * When course was completed.
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /**
     * Certificate ID (if certificate issued).
     */
    @Column(name = "certificate_id")
    private UUID certificateId;

    /**
     * Last activity timestamp (last time student accessed course).
     */
    @Column(name = "last_accessed_at")
    private LocalDateTime lastAccessedAt;

    /**
     * Enrollment expiry date (for time-limited courses).
     * Null = lifetime access.
     */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    /**
     * Timestamp when enrollment was created.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when enrollment was last updated.
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Update progress percentage.
     *
     * @param percentage New progress percentage (0-100)
     */
    public void updateProgress(Integer percentage) {
        this.progressPercentage = Math.min(Math.max(percentage, 0), 100);
        this.lastAccessedAt = LocalDateTime.now();
    }

    /**
     * Activate enrollment after successful payment.
     * Changes status from PENDING_PAYMENT to ACTIVE.
     *
     * @param paymentId Payment transaction ID
     */
    public void activate(UUID paymentId) {
        if (this.status == EnrollmentStatus.PENDING_PAYMENT) {
            this.status = EnrollmentStatus.ACTIVE;
            this.paymentId = paymentId;
        }
    }

    /**
     * Mark course as completed.
     * Sets completion flag and timestamp.
     */
    public void markAsCompleted() {
        this.isCompleted = true;
        this.completedAt = LocalDateTime.now();
        this.progressPercentage = 100;
        this.status = EnrollmentStatus.COMPLETED;
    }

    /**
     * Check if enrollment has expired.
     *
     * @return true if expired
     */
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Check if course content is accessible.
     * Only ACTIVE and COMPLETED enrollments allow access.
     *
     * @return true if student can access course content
     */
    public boolean isAccessible() {
        return (status == EnrollmentStatus.ACTIVE || status == EnrollmentStatus.COMPLETED)
                && !isExpired();
    }

    /**
     * Record course access.
     * Updates last accessed timestamp.
     */
    public void recordAccess() {
        this.lastAccessedAt = LocalDateTime.now();
    }
}