package com.mdaudev.mwanzo.mwanzocourseplatformbackend.controller;

import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.course.dto.CategoryDTO;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Category REST Controller
 *
 * Handles HTTP requests for category operations.
 * All endpoints are public (no authentication required for browsing).
 *
 * Base URL: /api/v1/categories
 *
 * Endpoints:
 * - GET    /api/v1/categories              - List all active categories
 * - GET    /api/v1/categories/popular      - List popular categories
 * - GET    /api/v1/categories/with-courses - List categories with courses
 * - GET    /api/v1/categories/{id}         - Get category by ID
 * - GET    /api/v1/categories/slug/{slug}  - Get category by slug
 *
 * Admin endpoints (to be secured later):
 * - POST   /api/v1/categories              - Create category
 * - PUT    /api/v1/categories/{id}         - Update category
 * - DELETE /api/v1/categories/{id}         - Delete category
 *
 * @author Mwanzo Development Team
 * @version 1.0
 * @since 2026-01-09
 */
@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Slf4j
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * Get all active categories.
     * Public endpoint - no authentication required.
     *
     * @return List of category DTOs
     */
    @GetMapping
    public ResponseEntity<List<CategoryDTO>> getAllCategories() {
        log.info("GET /api/v1/categories - Fetching all active categories");

        List<CategoryDTO> categories = categoryService.getAllActiveCategories();

        return ResponseEntity.ok(categories);
    }

    /**
     * Get popular categories (ordered by course count).
     * Used for homepage category display.
     *
     * @return List of popular category DTOs
     */
    @GetMapping("/popular")
    public ResponseEntity<List<CategoryDTO>> getPopularCategories() {
        log.info("GET /api/v1/categories/popular - Fetching popular categories");

        List<CategoryDTO> categories = categoryService.getPopularCategories();

        return ResponseEntity.ok(categories);
    }

    /**
     * Get categories that have at least one course.
     * Filters out empty categories.
     *
     * @return List of non-empty category DTOs
     */
    @GetMapping("/with-courses")
    public ResponseEntity<List<CategoryDTO>> getCategoriesWithCourses() {
        log.info("GET /api/v1/categories/with-courses - Fetching categories with courses");

        List<CategoryDTO> categories = categoryService.getCategoriesWithCourses();

        return ResponseEntity.ok(categories);
    }

    /**
     * Get category by ID.
     *
     * @param id Category UUID
     * @return Category DTO
     */
    @GetMapping("/{id}")
    public ResponseEntity<CategoryDTO> getCategoryById(@PathVariable UUID id) {
        log.info("GET /api/v1/categories/{} - Fetching category by ID", id);

        CategoryDTO category = categoryService.getCategoryById(id);

        return ResponseEntity.ok(category);
    }

    /**
     * Get category by slug (SEO-friendly URL).
     * Example: /api/v1/categories/slug/digital-marketing
     *
     * @param slug Category slug
     * @return Category DTO
     */
    @GetMapping("/slug/{slug}")
    public ResponseEntity<CategoryDTO> getCategoryBySlug(@PathVariable String slug) {
        log.info("GET /api/v1/categories/slug/{} - Fetching category by slug", slug);

        CategoryDTO category = categoryService.getCategoryBySlug(slug);

        return ResponseEntity.ok(category);
    }

    // ==========================================
    // ADMIN ENDPOINTS (To be secured later)
    // ==========================================

    /**
     * Create a new category (Admin only).
     * TODO: Add @PreAuthorize("hasRole('ADMIN')") when security is implemented
     *
     * @param name Category name
     * @param description Category description
     * @return Created category DTO
     */
    @PostMapping
    public ResponseEntity<CategoryDTO> createCategory(
            @RequestParam String name,
            @RequestParam(required = false) String description) {

        log.info("POST /api/v1/categories - Creating category: {}", name);

        CategoryDTO category = categoryService.createCategory(name, description);

        return ResponseEntity.status(HttpStatus.CREATED).body(category);
    }

    /**
     * Update category (Admin only).
     * TODO: Add @PreAuthorize("hasRole('ADMIN')") when security is implemented
     *
     * @param id Category UUID
     * @param name New category name (optional)
     * @param description New description (optional)
     * @return Updated category DTO
     */
    @PutMapping("/{id}")
    public ResponseEntity<CategoryDTO> updateCategory(
            @PathVariable UUID id,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String description) {

        log.info("PUT /api/v1/categories/{} - Updating category", id);

        CategoryDTO category = categoryService.updateCategory(id, name, description);

        return ResponseEntity.ok(category);
    }

    /**
     * Soft delete category (Admin only).
     * TODO: Add @PreAuthorize("hasRole('ADMIN')") when security is implemented
     *
     * @param id Category UUID
     * @return No content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable UUID id) {
        log.info("DELETE /api/v1/categories/{} - Deleting category", id);

        categoryService.deleteCategory(id);

        return ResponseEntity.noContent().build();
    }
}