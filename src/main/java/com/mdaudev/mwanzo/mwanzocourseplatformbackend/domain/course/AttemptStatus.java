package com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.course;

/**
 * Attempt Status Enum
 *
 * Tracks the lifecycle of a quiz attempt.
 *
 * @author Mwanzo Development Team
 * @version 1.0
 * @since 2026-01-13
 */
public enum AttemptStatus {
    /**
     * Quiz attempt started but not yet submitted.
     */
    IN_PROGRESS,

    /**
     * Quiz submitted and graded.
     */
    SUBMITTED,

    /**
     * Quiz attempt abandoned (timed out).
     */
    ABANDONED,

    /**
     * Quiz attempt flagged for cheating (future).
     */
    FLAGGED
}