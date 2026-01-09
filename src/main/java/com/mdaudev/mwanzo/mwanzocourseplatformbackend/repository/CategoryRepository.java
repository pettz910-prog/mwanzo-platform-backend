package com.mdaudev.mwanzo.mwanzocourseplatformbackend.repository;

import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.course.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Category Repository
 *
 * Data access layer for Category entity.
 * Provides CRUD operations and custom queries.
 *
 * Spring Data JPA automatically implements these methods.
 *
 * @author Mwanzo Development Team
 * @version 1.0
 * @since 2026-01-09
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    /**
     * Find category by name (case-insensitive).
     * Useful for checking duplicates during creation.
     *
     * @param name Category name
     * @return Optional containing category if found
     */
    Optional<Category> findByNameIgnoreCase(String name);

    /**
     * Find category by slug.
     * Used for SEO-friendly URLs.
     *
     * @param slug URL slug
     * @return Optional containing category if found
     */
    Optional<Category> findBySlug(String slug);

    /**
     * Find all active categories.
     * Only returns categories with isActive = true.
     *
     * @return List of active categories
     */
    List<Category> findByIsActiveTrue();

    /**
     * Find all active categories ordered by course count (most popular first).
     * Useful for displaying popular categories on homepage.
     *
     * @return List of active categories ordered by popularity
     */
    @Query("SELECT c FROM Category c WHERE c.isActive = true ORDER BY c.courseCount DESC")
    List<Category> findActiveOrderedByPopularity();

    /**
     * Find all active categories with at least one course.
     * Filters out empty categories from public view.
     *
     * @return List of non-empty active categories
     */
    @Query("SELECT c FROM Category c WHERE c.isActive = true AND c.courseCount > 0")
    List<Category> findActiveWithCourses();

    /**
     * Check if category name already exists (case-insensitive).
     * Used for validation during creation/update.
     *
     * @param name Category name
     * @return true if name exists
     */
    boolean existsByNameIgnoreCase(String name);
}