package com.mdaudev.mwanzo.mwanzocourseplatformbackend.service;

import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.course.Category;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.course.dto.CategoryDTO;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Category Service
 *
 * Business logic layer for category operations.
 * Handles category CRUD, validation, and DTO mapping.
 *
 * Microservices-Ready:
 * - No dependencies on other domain services
 * - Can be extracted to "Course Service" independently
 * - Uses DTOs for clean API contracts
 *
 * @author Mwanzo Development Team
 * @version 1.0
 * @since 2026-01-09
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;

    /**
     * Get all active categories.
     * Returns categories available for public viewing.
     *
     * @return List of category DTOs
     */
    public List<CategoryDTO> getAllActiveCategories() {
        log.debug("Fetching all active categories");

        List<Category> categories = categoryRepository.findByIsActiveTrue();

        log.info("Found {} active categories", categories.size());

        return categories.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get all active categories ordered by popularity (course count).
     * Used for homepage category display.
     *
     * @return List of category DTOs ordered by popularity
     */
    public List<CategoryDTO> getPopularCategories() {
        log.debug("Fetching popular categories");

        List<Category> categories = categoryRepository.findActiveOrderedByPopularity();

        log.info("Found {} popular categories", categories.size());

        return categories.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get all active categories that have at least one course.
     * Filters out empty categories from public view.
     *
     * @return List of non-empty category DTOs
     */
    public List<CategoryDTO> getCategoriesWithCourses() {
        log.debug("Fetching categories with courses");

        List<Category> categories = categoryRepository.findActiveWithCourses();

        log.info("Found {} categories with courses", categories.size());

        return categories.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get category by ID.
     *
     * @param id Category UUID
     * @return Category DTO
     * @throws RuntimeException if category not found
     */
    public CategoryDTO getCategoryById(UUID id) {
        log.debug("Fetching category by ID: {}", id);

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Category not found with ID: {}", id);
                    return new RuntimeException("Category not found with ID: " + id);
                });

        log.info("Found category: {}", category.getName());

        return mapToDTO(category);
    }

    /**
     * Get category by slug (SEO-friendly URL).
     *
     * @param slug Category slug
     * @return Category DTO
     * @throws RuntimeException if category not found
     */
    public CategoryDTO getCategoryBySlug(String slug) {
        log.debug("Fetching category by slug: {}", slug);

        Category category = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> {
                    log.error("Category not found with slug: {}", slug);
                    return new RuntimeException("Category not found with slug: " + slug);
                });

        log.info("Found category: {}", category.getName());

        return mapToDTO(category);
    }

    /**
     * Create a new category (Admin only).
     * Validates that category name is unique.
     *
     * @param name Category name
     * @param description Category description
     * @return Created category DTO
     * @throws RuntimeException if category name already exists
     */
    @Transactional
    public CategoryDTO createCategory(String name, String description) {
        log.debug("Creating new category: {}", name);

        // Validate unique name
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            log.error("Category already exists with name: {}", name);
            throw new RuntimeException("Category already exists with name: " + name);
        }

        // Create category
        Category category = Category.builder()
                .name(name.trim())
                .description(description != null ? description.trim() : null)
                .courseCount(0)
                .isActive(true)
                .build();

        // Save to database
        Category savedCategory = categoryRepository.save(category);

        log.info("Created category with ID: {} and slug: {}",
                savedCategory.getId(), savedCategory.getSlug());

        return mapToDTO(savedCategory);
    }

    /**
     * Update category (Admin only).
     *
     * @param id Category UUID
     * @param name New category name (optional)
     * @param description New description (optional)
     * @return Updated category DTO
     * @throws RuntimeException if category not found
     */
    @Transactional
    public CategoryDTO updateCategory(UUID id, String name, String description) {
        log.debug("Updating category: {}", id);

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Category not found with ID: {}", id);
                    return new RuntimeException("Category not found with ID: " + id);
                });

        // Update name if provided
        if (name != null && !name.trim().isEmpty()) {
            // Check if new name already exists (excluding current category)
            categoryRepository.findByNameIgnoreCase(name.trim())
                    .ifPresent(existing -> {
                        if (!existing.getId().equals(id)) {
                            log.error("Category name already exists: {}", name);
                            throw new RuntimeException("Category name already exists: " + name);
                        }
                    });

            category.setName(name.trim());
            // Regenerate slug
            category.setSlug(null);
            category.generateSlug();
        }

        // Update description if provided
        if (description != null) {
            category.setDescription(description.trim());
        }

        Category updatedCategory = categoryRepository.save(category);

        log.info("Updated category: {}", updatedCategory.getName());

        return mapToDTO(updatedCategory);
    }

    /**
     * Soft delete category (Admin only).
     * Sets isActive to false instead of deleting.
     *
     * @param id Category UUID
     * @throws RuntimeException if category not found
     */
    @Transactional
    public void deleteCategory(UUID id) {
        log.debug("Soft deleting category: {}", id);

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Category not found with ID: {}", id);
                    return new RuntimeException("Category not found with ID: " + id);
                });

        category.setIsActive(false);
        categoryRepository.save(category);

        log.info("Soft deleted category: {}", category.getName());
    }

    /**
     * Increment course count for a category.
     * Called when a course is published in this category.
     *
     * @param categoryId Category UUID
     */
    @Transactional
    public void incrementCourseCount(UUID categoryId) {
        log.debug("Incrementing course count for category: {}", categoryId);

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found with ID: " + categoryId));

        category.incrementCourseCount();
        categoryRepository.save(category);

        log.info("Incremented course count for category: {} to {}",
                category.getName(), category.getCourseCount());
    }

    /**
     * Decrement course count for a category.
     * Called when a course is unpublished/deleted from this category.
     *
     * @param categoryId Category UUID
     */
    @Transactional
    public void decrementCourseCount(UUID categoryId) {
        log.debug("Decrementing course count for category: {}", categoryId);

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found with ID: " + categoryId));

        category.decrementCourseCount();
        categoryRepository.save(category);

        log.info("Decremented course count for category: {} to {}",
                category.getName(), category.getCourseCount());
    }

    /**
     * Map Category entity to CategoryDTO.
     * Keeps internal entity structure hidden from API layer.
     *
     * @param category Category entity
     * @return Category DTO
     */
    private CategoryDTO mapToDTO(Category category) {
        return CategoryDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .slug(category.getSlug())
                .courseCount(category.getCourseCount())
                .createdAt(category.getCreatedAt())
                .build();
    }
}