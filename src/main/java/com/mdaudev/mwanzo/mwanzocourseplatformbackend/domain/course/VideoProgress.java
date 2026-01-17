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
 * Video Progress Entity
 *
 * Tracks individual student's progress watching each video.
 * Enables "Continue Watching" and course completion features.
 *
 * Database Table: video_progress
 *
 * Business Rules:
 * - One record per student per video
 * - Video marked "completed" when watchedSeconds >= 80% of duration
 * - Last watched position saved for resume playback
 *
 * @author Mwanzo Development Team
 * @version 1.0
 * @since 2026-01-12
 */
@Entity
@Table(name = "video_progress", indexes = {
        @Index(name = "idx_progress_student", columnList = "student_id"),
        @Index(name = "idx_progress_enrollment", columnList = "enrollment_id"),
        @Index(name = "idx_progress_video", columnList = "video_id"),
        @Index(name = "idx_progress_student_video", columnList = "student_id, video_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_student_video", columnNames = {"student_id", "video_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoProgress {

    /**
     * Unique identifier for the progress record.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * Student watching the video.
     */
    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    /**
     * Enrollment this progress belongs to.
     */
    @Column(name = "enrollment_id", nullable = false)
    private UUID enrollmentId;

    /**
     * Video being watched.
     */
    @Column(name = "video_id", nullable = false)
    private UUID videoId;

    /**
     * Course this video belongs to.
     */
    @Column(name = "course_id", nullable = false)
    private UUID courseId;

    /**
     * Section this video belongs to.
     */
    @Column(name = "section_id", nullable = false)
    private UUID sectionId;

    /**
     * Total seconds watched (can be > video duration if rewatched).
     */
    @Column(name = "watched_seconds", nullable = false)
    @Builder.Default
    private Integer watchedSeconds = 0;

    /**
     * Last playback position in seconds (for resume).
     */
    @Column(name = "last_position_seconds", nullable = false)
    @Builder.Default
    private Integer lastPositionSeconds = 0;

    /**
     * Watch progress percentage (0-100).
     * Calculated: (watchedSeconds / videoDuration) * 100
     */
    @Column(name = "progress_percentage", nullable = false)
    @Builder.Default
    private Integer progressPercentage = 0;

    /**
     * Whether video has been completed (watched >= 80%).
     */
    @Column(name = "is_completed", nullable = false)
    @Builder.Default
    private Boolean isCompleted = false;

    /**
     * When video was completed.
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /**
     * Last time student watched this video.
     */
    @Column(name = "last_watched_at")
    private LocalDateTime lastWatchedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Update watch progress.
     *
     * @param positionSeconds Current playback position
     * @param videoDurationSeconds Total video duration
     */
    public void updateProgress(Integer positionSeconds, Integer videoDurationSeconds) {
        this.lastPositionSeconds = positionSeconds;
        this.lastWatchedAt = LocalDateTime.now();

        // Calculate progress percentage
        if (videoDurationSeconds > 0) {
            this.progressPercentage = Math.min(100,
                    (int) ((positionSeconds * 100.0) / videoDurationSeconds));
        }

        // Mark as completed if watched >= 80%
        if (this.progressPercentage >= 80 && !this.isCompleted) {
            this.isCompleted = true;
            this.completedAt = LocalDateTime.now();
        }
    }

    /**
     * Increment watched seconds (called periodically during playback).
     *
     * @param seconds Seconds watched since last update
     */
    public void addWatchedSeconds(Integer seconds) {
        this.watchedSeconds += seconds;
        this.lastWatchedAt = LocalDateTime.now();
    }
}