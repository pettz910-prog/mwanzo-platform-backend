package com.mdaudev.mwanzo.mwanzocourseplatformbackend.repository;

import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.course.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Course Repository
 *
 * Data access layer for Course entity.
 * Provides comprehensive query methods for course operations.
 *
 * Features:
 * - Pagination support
 * - Search and filtering
 * - Category-based queries
 * - Instructor queries
 * - Featured/popular courses
 *
 * @author Mwanzo Development Team
 * @version 1.0
 * @since 2026-01-09
 */
@Repository
public interface CourseRepository extends JpaRepository<Course, UUID> {

    // ==========================================
    // BASIC LOOKUPS
    // ==========================================

    /**
     * Find course by slug (SEO-friendly URL).
     *
     * @param slug Course slug
     * @return Optional containing course if found
     */
    Optional<Course> findBySlug(String slug);

    /**
     * Check if course with slug exists.
     *
     * @param slug Course slug
     * @return true if exists
     */
    boolean existsBySlug(String slug);

    // ==========================================
    // PUBLIC COURSE LISTINGS (Student View)
    // ==========================================

    /**
     * Find all published courses with pagination.
     * Used for main course catalog.
     *
     * @param pageable Pagination parameters
     * @return Page of published courses
     */
    Page<Course> findByIsPublishedTrue(Pageable pageable);

    /**
     * Find published courses by category with pagination.
     *
     * @param categoryId Category UUID
     * @param pageable Pagination parameters
     * @return Page of courses in category
     */
    Page<Course> findByCategoryIdAndIsPublishedTrue(UUID categoryId, Pageable pageable);

    /**
     * Search courses by title or description (case-insensitive).
     * Only returns published courses.
     *
     * @param keyword Search keyword
     * @param pageable Pagination parameters
     * @return Page of matching courses
     */
    @Query("SELECT c FROM Course c WHERE c.isPublished = true AND " +
            "(LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(c.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Course> searchPublishedCourses(@Param("keyword") String keyword, Pageable pageable);

    /**
     * Find featured courses (curated by admin).
     * Limited to top N courses.
     *
     * @param pageable Pagination parameters
     * @return Page of featured courses
     */
    Page<Course> findByIsFeaturedTrueAndIsPublishedTrue(Pageable pageable);

    /**
     * Find free courses (price = 0).
     *
     * @param price Should be BigDecimal.ZERO
     * @param pageable Pagination parameters
     * @return Page of free courses
     */
    Page<Course> findByPriceAndIsPublishedTrue(BigDecimal price, Pageable pageable);

    /**
     * Find courses by level (BEGINNER, INTERMEDIATE, ADVANCED).
     *
     * @param level Course level
     * @param pageable Pagination parameters
     * @return Page of courses at specified level
     */
    @Query("SELECT c FROM Course c WHERE c.level = :level AND c.isPublished = true")
    Page<Course> findByLevelAndPublished(@Param("level") String level, Pageable pageable);

    /**
     * Find popular courses (most enrollments).
     *
     * @param pageable Pagination parameters (should include sort by enrollmentCount DESC)
     * @return Page of popular courses
     */
    @Query("SELECT c FROM Course c WHERE c.isPublished = true ORDER BY c.enrollmentCount DESC")
    Page<Course> findPopularCourses(Pageable pageable);

    /**
     * Find highly rated courses (rating >= threshold).
     *
     * @param minRating Minimum rating (e.g., 4.0)
     * @param pageable Pagination parameters
     * @return Page of highly rated courses
     */
    @Query("SELECT c FROM Course c WHERE c.isPublished = true AND c.averageRating >= :minRating " +
            "ORDER BY c.averageRating DESC, c.ratingCount DESC")
    Page<Course> findHighlyRatedCourses(@Param("minRating") BigDecimal minRating, Pageable pageable);

    /**
     * Find new courses (recently published).
     *
     * @param pageable Pagination parameters (should sort by publishedAt DESC)
     * @return Page of new courses
     */
    @Query("SELECT c FROM Course c WHERE c.isPublished = true ORDER BY c.publishedAt DESC")
    Page<Course> findNewCourses(Pageable pageable);

    // ==========================================
    // INSTRUCTOR QUERIES
    // ==========================================

    /**
     * Find all courses by instructor.
     * Returns all statuses (draft, pending, published, etc.).
     *
     * @param instructorId Instructor UUID
     * @param pageable Pagination parameters
     * @return Page of instructor's courses
     */
    Page<Course> findByInstructorId(UUID instructorId, Pageable pageable);

    /**
     * Find published courses by instructor.
     *
     * @param instructorId Instructor UUID
     * @param pageable Pagination parameters
     * @return Page of instructor's published courses
     */
    Page<Course> findByInstructorIdAndIsPublishedTrue(UUID instructorId, Pageable pageable);

    /**
     * Count instructor's courses.
     *
     * @param instructorId Instructor UUID
     * @return Total course count
     */
    long countByInstructorId(UUID instructorId);

    /**
     * Count instructor's published courses.
     *
     * @param instructorId Instructor UUID
     * @return Published course count
     */
    long countByInstructorIdAndIsPublishedTrue(UUID instructorId);

    // ==========================================
    // ADMIN QUERIES
    // ==========================================

    /**
     * Find courses by status (DRAFT, PENDING_REVIEW, APPROVED, etc.).
     *
     * @param status Course status
     * @param pageable Pagination parameters
     * @return Page of courses with specified status
     */
    @Query("SELECT c FROM Course c WHERE c.status = :status")
    Page<Course> findByStatus(@Param("status") String status, Pageable pageable);

    /**
     * Find courses pending admin review.
     *
     * @param pageable Pagination parameters
     * @return Page of courses awaiting approval
     */
    @Query("SELECT c FROM Course c WHERE c.status = 'PENDING_REVIEW' ORDER BY c.createdAt ASC")
    Page<Course> findCoursesAwaitingReview(Pageable pageable);

    /**
     * Count courses by status.
     *
     * @param status Course status
     * @return Count of courses
     */
    @Query("SELECT COUNT(c) FROM Course c WHERE c.status = :status")
    long countByStatus(@Param("status") String status);

    // ==========================================
    // CATEGORY QUERIES
    // ==========================================

    /**
     * Count published courses in category.
     * Used to update category.courseCount.
     *
     * @param categoryId Category UUID
     * @return Course count
     */
    long countByCategoryIdAndIsPublishedTrue(UUID categoryId);

    /**
     * Find all courses in category (any status).
     * Admin use only.
     *
     * @param categoryId Category UUID
     * @return List of all courses in category
     */
    List<Course> findByCategoryId(UUID categoryId);

    // ==========================================
    // STATISTICS QUERIES
    // ==========================================

    /**
     * Get total published course count.
     *
     * @return Total published courses
     */
    long countByIsPublishedTrue();

    /**
     * Get total enrollment count across all courses.
     *
     * @return Sum of all enrollments
     */
    @Query("SELECT COALESCE(SUM(c.enrollmentCount), 0) FROM Course c WHERE c.isPublished = true")
    Long getTotalEnrollmentCount();

    /**
     * Get average course price.
     *
     * @return Average price of published courses
     */
    @Query("SELECT AVG(c.price) FROM Course c WHERE c.isPublished = true")
    BigDecimal getAverageCoursePrice();
}