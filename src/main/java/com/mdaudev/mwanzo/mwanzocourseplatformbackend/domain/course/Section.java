package com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.course;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Section Entity (Course Module/Chapter)
 *
 * Organizes course videos into logical sections/modules.
 * Example: "Introduction to Python" course might have sections:
 * - Section 1: Getting Started (5 videos)
 * - Section 2: Variables and Data Types (8 videos)
 * - Section 3: Control Flow (10 videos)
 *
 * Database Table: sections
 *
 * Business Rules:
 * - Sections belong to one course
 * - Display order determines section sequence
 * - Section duration calculated from video durations
 *
 * @author Mwanzo Development Team
 * @version 1.0
 * @since 2026-01-12
 */
@Entity
@Table(name = "sections", indexes = {
        @Index(name = "idx_section_course", columnList = "course_id"),
        @Index(name = "idx_section_order", columnList = "course_id, display_order")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Section {

    /**
     * Unique identifier for the section.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * Course this section belongs to.
     */
    @Column(name = "course_id", nullable = false)
    private UUID courseId;

    /**
     * Section title (e.g., "Getting Started with Python").
     */
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    /**
     * Section description/objectives (optional).
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Display order (1, 2, 3...).
     * Lower numbers appear first.
     */
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    /**
     * Total duration of all videos in this section (in minutes).
     * Calculated/updated when videos are added.
     */
    @Column(name = "total_duration_minutes", nullable = false)
    @Builder.Default
    private Integer totalDurationMinutes = 0;

    /**
     * Number of videos in this section.
     * Denormalized for performance.
     */
    @Column(name = "video_count", nullable = false)
    @Builder.Default
    private Integer videoCount = 0;

    /**
     * Whether section is published (visible to students).
     */
    @Column(name = "is_published", nullable = false)
    @Builder.Default
    private Boolean isPublished = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Increment video count and update duration.
     * Called when a video is added to this section.
     *
     * @param videoDurationMinutes Duration of added video
     */
    public void addVideo(Integer videoDurationMinutes) {
        this.videoCount++;
        this.totalDurationMinutes += (videoDurationMinutes != null ? videoDurationMinutes : 0);
    }

    /**
     * Decrement video count and update duration.
     * Called when a video is removed from this section.
     *
     * @param videoDurationMinutes Duration of removed video
     */
    public void removeVideo(Integer videoDurationMinutes) {
        this.videoCount = Math.max(0, this.videoCount - 1);
        this.totalDurationMinutes = Math.max(0,
                this.totalDurationMinutes - (videoDurationMinutes != null ? videoDurationMinutes : 0));
    }
}