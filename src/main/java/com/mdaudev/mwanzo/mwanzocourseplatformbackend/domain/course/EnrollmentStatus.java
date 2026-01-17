package com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.course;

/**
 * Enrollment Status Enum
 *
 * Represents the lifecycle status of a student's course enrollment.
 *
 * Workflow:
 * PENDING_PAYMENT → ACTIVE → COMPLETED
 *                ↓
 *            CANCELLED
 *
 * @author Mwanzo Development Team
 * @version 1.0
 * @since 2026-01-09
 */
public enum EnrollmentStatus {
    /**
     * Enrollment created but payment not yet completed.
     * Student added course to cart but hasn't paid yet.
     * Course content is NOT accessible in this state.
     */
    PENDING_PAYMENT,

    /**
     * Active enrollment - payment completed, student can access course content.
     */
    ACTIVE,

    /**
     * Course completed - all videos watched and quizzes passed.
     */
    COMPLETED,

    /**
     * Enrollment suspended (e.g., payment dispute, policy violation).
     * Course content temporarily inaccessible.
     */
    SUSPENDED,

    /**
     * Enrollment cancelled - refunded or voluntarily withdrawn.
     * Course content no longer accessible.
     */
    CANCELLED,

    /**
     * Enrollment expired - time-limited access period ended.
     * Course content no longer accessible.
     */
    EXPIRED
}