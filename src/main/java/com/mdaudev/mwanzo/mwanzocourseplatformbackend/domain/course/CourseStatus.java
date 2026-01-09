package com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.course;

/**
 * Course lifecycle status.
 */
public enum CourseStatus {
    DRAFT,              // Being created by instructor
    PENDING_REVIEW,     // Submitted for admin approval
    APPROVED,           // Approved by admin (ready to publish)
    PUBLISHED,          // Live and visible to students
    REJECTED,           // Rejected by admin (needs changes)
    ARCHIVED            // Hidden but preserved (soft delete)
}
