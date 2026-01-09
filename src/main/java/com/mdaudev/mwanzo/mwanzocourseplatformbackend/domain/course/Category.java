package com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.course;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Category Entity
 *
 * Represents a course category in the platform.
 * Categories help organize courses into logical groups (e.g., "Programming", "Business", "Design").
 *
 * Database Table: categories
 *
 * Business Rules:
 * - Category names must be unique
 * - Categories can have descriptions
 * - Each category tracks total course count
 * - Soft delete support (isActive flag)
 *
 * @author Mwanzo Development Team
 * @version 1.0
 * @since 2026-01-09
 */
@Entity
@Table(name = "categories", indexes = {
        @Index(name = "idx_category_name", columnList = "name"),
        @Index(name = "idx_category_active", columnList = "isActive")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

    /**
     * Unique identifier for the category.
     * Using UUID for better distributed system compatibility.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * Category name (e.g., "Digital Marketing", "Web Development").
     * Must be unique across the platform.
     */
    @NotBlank(message = "Category name is required")
    @Size(min = 2, max = 100, message = "Category name must be between 2 and 100 characters")
    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    /**
     * Detailed description of what courses this category contains.
     * Helps users understand the category scope.
     */
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    @Column(name = "description", length = 500)
    private String description;

    /**
     * URL slug for SEO-friendly URLs (e.g., "digital-marketing").
     * Generated from category name.
     */
    @Column(name = "slug", unique = true, length = 120)
    private String slug;

    /**
     * Total number of courses in this category.
     * Updated when courses are added/removed.
     * Denormalized for performance (avoids JOIN queries).
     */
    @Column(name = "course_count", nullable = false)
    @Builder.Default
    private Integer courseCount = 0;

    /**
     * Soft delete flag.
     * When false, category is hidden from public view but data preserved.
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Timestamp when category was created.
     * Automatically set by Hibernate.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when category was last updated.
     * Automatically updated by Hibernate on any change.
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Pre-persist callback to generate slug from name.
     * Executes before entity is first saved to database.
     */
    @PrePersist
    public void generateSlug() {
        if (this.slug == null && this.name != null) {
            this.slug = this.name
                    .toLowerCase()
                    .trim()
                    .replaceAll("\\s+", "-")           // Replace spaces with hyphens
                    .replaceAll("[^a-z0-9-]", "")      // Remove special characters
                    .replaceAll("-+", "-");             // Remove duplicate hyphens
        }
    }

    /**
     * Increment course count when a course is added to this category.
     */
    public void incrementCourseCount() {
        this.courseCount++;
    }

    /**
     * Decrement course count when a course is removed from this category.
     */
    public void decrementCourseCount() {
        if (this.courseCount > 0) {
            this.courseCount--;
        }
    }
}