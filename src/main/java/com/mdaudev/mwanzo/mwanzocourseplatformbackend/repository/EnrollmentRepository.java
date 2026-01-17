package com.mdaudev.mwanzo.mwanzocourseplatformbackend.repository;

import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.course.Enrollment;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.course.EnrollmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Enrollment Repository
 *
 * Data access layer for Enrollment entity.
 * Manages student-course enrollments and progress tracking.
 *
 * @author Mwanzo Development Team
 * @version 1.0
 * @since 2026-01-09
 */
@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, UUID> {

    /**
     * Find enrollment by student and course.
     * Used to check if student is already enrolled.
     *
     * @param studentId Student UUID
     * @param courseId Course UUID
     * @return Optional containing enrollment if found
     */
    @Query("SELECT e FROM Enrollment e WHERE e.studentId = :studentId AND e.courseId = :courseId")
    Optional<Enrollment> findByStudentIdAndCourseId(
            @Param("studentId") UUID studentId,
            @Param("courseId") UUID courseId
    );

    /**
     * Check if student is enrolled in course.
     *
     * @param studentId Student UUID
     * @param courseId Course UUID
     * @return true if enrolled
     */
    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM Enrollment e " +
            "WHERE e.studentId = :studentId AND e.courseId = :courseId")
    boolean existsByStudentIdAndCourseId(
            @Param("studentId") UUID studentId,
            @Param("courseId") UUID courseId
    );

    /**
     * Find all enrollments for a student.
     *
     * @param studentId Student UUID
     * @param pageable Pagination parameters
     * @return Page of enrollments
     */
    Page<Enrollment> findByStudentId(UUID studentId, Pageable pageable);

    /**
     * Find active enrollments for a student.
     *
     * @param studentId Student UUID
     * @param pageable Pagination parameters
     * @return Page of active enrollments
     */
    @Query("SELECT e FROM Enrollment e WHERE e.studentId = :studentId AND e.status = 'ACTIVE'")
    Page<Enrollment> findActiveEnrollmentsByStudentId(@Param("studentId") UUID studentId, Pageable pageable);

    /**
     * Find completed enrollments for a student.
     *
     * @param studentId Student UUID
     * @param pageable Pagination parameters
     * @return Page of completed enrollments
     */
    @Query("SELECT e FROM Enrollment e WHERE e.studentId = :studentId AND e.status = 'COMPLETED'")
    Page<Enrollment> findCompletedEnrollmentsByStudentId(@Param("studentId") UUID studentId, Pageable pageable);

    /**
     * Find all enrollments for a course.
     * Used by instructors to see who enrolled.
     *
     * @param courseId Course UUID
     * @param pageable Pagination parameters
     * @return Page of enrollments
     */
    @Query("SELECT e FROM Enrollment e WHERE e.courseId = :courseId")
    Page<Enrollment> findByCourseId(@Param("courseId") UUID courseId, Pageable pageable);

    /**
     * Count total enrollments for a course.
     *
     * @param courseId Course UUID
     * @return Enrollment count
     */
    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.courseId = :courseId")
    long countByCourseId(@Param("courseId") UUID courseId);

    /**
     * Count active enrollments for a student.
     *
     * @param studentId Student UUID
     * @return Active enrollment count
     */
    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.studentId = :studentId AND e.status = 'ACTIVE'")
    long countActiveEnrollmentsByStudentId(@Param("studentId") UUID studentId);

    /**
     * Find enrollments by status.
     *
     * @param status Enrollment status
     * @param pageable Pagination parameters
     * @return Page of enrollments
     */
    Page<Enrollment> findByStatus(EnrollmentStatus status, Pageable pageable);

    /**
     * Find recently accessed enrollments for a student.
     * Used for "Continue Learning" section.
     *
     * @param studentId Student UUID
     * @param pageable Pagination parameters (should sort by lastAccessedAt DESC)
     * @return Page of recently accessed enrollments
     */
    @Query("SELECT e FROM Enrollment e WHERE e.studentId = :studentId AND e.status = 'ACTIVE' " +
            "ORDER BY e.lastAccessedAt DESC NULLS LAST")
    Page<Enrollment> findRecentlyAccessedByStudentId(@Param("studentId") UUID studentId, Pageable pageable);
}