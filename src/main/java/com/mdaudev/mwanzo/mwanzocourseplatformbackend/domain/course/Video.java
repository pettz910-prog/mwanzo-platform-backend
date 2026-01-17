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
 * Video Entity (Course Lesson)
 *
 * Represents individual video lessons within a course section.
 * Videos are stored in S3/CloudFront and streamed to students.
 *
 * Database Table: videos
 *
 * Business Rules:
 * - Videos belong to one section
 * - Only enrolled students can watch videos
 * - Video progress tracked per student
 * - Videos can be marked as preview (free for everyone)
 *
 * Video Storage:
 * - Original uploaded to S3
 * - Transcoded by AWS MediaConvert (multiple qualities)
 * - Streamed via CloudFront CDN
 *
 * @author Mwanzo Development Team
 * @version 1.0
 * @since 2026-01-12
 */
@Entity
@Table(name = "videos", indexes = {
        @Index(name = "idx_video_section", columnList = "section_id"),
        @Index(name = "idx_video_course", columnList = "course_id"),
        @Index(name = "idx_video_order", columnList = "section_id, display_order")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Video {

    /**
     * Unique identifier for the video.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

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
     * Video title (e.g., "Installing Python on Windows").
     */
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    /**
     * Video description/what students will learn.
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Display order within section (1, 2, 3...).
     */
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    /**
     * Video duration in seconds.
     */
    @Column(name = "duration_seconds", nullable = false)
    private Integer durationSeconds;

    /**
     * S3 key for the video file.
     * Example: "videos/uuid/filename.mp4"
     */
    @Column(name = "s3_key", nullable = false, length = 500)
    private String s3Key;

    /**
     * S3 URL for original uploaded video.
     */
    @Column(name = "video_url", nullable = false, length = 500)
    private String videoUrl;

    /**
     * CloudFront streaming URL (HLS/DASH).
     * Used for adaptive bitrate streaming.
     */
    @Column(name = "streaming_url", length = 500)
    private String streamingUrl;

    /**
     * S3 key for video thumbnail.
     * Example: "thumbnails/uuid/thumbnail.jpg"
     */
    @Column(name = "thumbnail_s3_key", length = 500)
    private String thumbnailS3Key;

    /**
     * S3 URL for video thumbnail/poster image.
     * REQUIRED for all videos.
     */
    @Column(name = "thumbnail_url", nullable = false, length = 500)
    private String thumbnailUrl;

    /**
     * Video quality (e.g., "1080p", "720p", "480p").
     */
    @Column(name = "quality", length = 20)
    private String quality;

    /**
     * Video file size in bytes.
     */
    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    /**
     * Whether this is a preview video (free for everyone).
     * Preview videos can be watched without enrollment.
     */
    @Column(name = "is_preview", nullable = false)
    @Builder.Default
    private Boolean isPreview = false;

    /**
     * Whether video is published (visible to students).
     */
    @Column(name = "is_published", nullable = false)
    @Builder.Default
    private Boolean isPublished = true;

    /**
     * Video processing status.
     * UPLOADED → PROCESSING → READY → FAILED
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 20)
    @Builder.Default
    private VideoProcessingStatus processingStatus = VideoProcessingStatus.UPLOADED;

    /**
     * Video transcoding job ID (AWS MediaConvert).
     */
    @Column(name = "transcoding_job_id", length = 100)
    private String transcodingJobId;

    /**
     * Number of times this video has been watched.
     * Incremented when watch progress >= 80%.
     */
    @Column(name = "view_count", nullable = false)
    @Builder.Default
    private Integer viewCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Get duration in minutes (rounded up).
     */
    public Integer getDurationMinutes() {
        return (int) Math.ceil(durationSeconds / 60.0);
    }

    /**
     * Increment view count.
     */
    public void incrementViewCount() {
        this.viewCount++;
    }

    /**
     * Mark video as ready for streaming.
     */
    public void markAsReady(String streamingUrl) {
        this.processingStatus = VideoProcessingStatus.READY;
        this.streamingUrl = streamingUrl;
    }

    /**
     * Mark video processing as failed.
     */
    public void markAsFailed() {
        this.processingStatus = VideoProcessingStatus.FAILED;
    }
}