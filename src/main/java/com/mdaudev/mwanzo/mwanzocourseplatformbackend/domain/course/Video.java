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
 * Updated to support multi-quality video streaming with AWS MediaConvert.
 *
 * NEW FEATURES (v2.0):
 * - Multi-quality transcoding (360p, 720p, 1080p)
 * - Quality URLs stored as JSON
 * - Processing job tracking
 * - Error handling for failed transcoding
 *
 * @author Mwanzo Development Team
 * @version 2.0
 * @since 2026-01-17
 */
@Entity
@Table(name = "videos", indexes = {
        @Index(name = "idx_video_section", columnList = "section_id"),
        @Index(name = "idx_video_course", columnList = "course_id"),
        @Index(name = "idx_video_order", columnList = "section_id, display_order"),
        @Index(name = "idx_video_processing", columnList = "processing_status")
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
     * Automatically extracted by MediaConvert during transcoding.
     */
    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    /**
     * S3 key for the ORIGINAL uploaded video file.
     * Example: "videos/uuid/filename.mp4"
     */
    @Column(name = "s3_key", nullable = false, length = 500)
    private String s3Key;

    /**
     * Primary video URL (defaults to highest quality available).
     * Updated after transcoding completes.
     */
    @Column(name = "video_url", nullable = false, length = 500)
    private String videoUrl;

    /**
     * JSON object containing URLs for each quality level.
     * Example: {"360p":"https://...", "720p":"https://...", "1080p":"https://..."}
     *
     * NEW: Supports adaptive streaming
     */
    @Column(name = "quality_urls_json", columnDefinition = "TEXT")
    private String qualityUrlsJson;

    /**
     * CloudFront streaming URL (HLS/DASH manifest).
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
     * S3/CloudFront URL for video thumbnail/poster image.
     * REQUIRED for all videos (instructor uploads).
     */
    @Column(name = "thumbnail_url", nullable = false, length = 500)
    private String thumbnailUrl;

    /**
     * Video quality (e.g., "1080p", "720p", "480p").
     * Deprecated: Use qualityUrlsJson instead
     */
    @Column(name = "quality", length = 20)
    @Deprecated
    private String quality;

    /**
     * Original video file size in bytes.
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
     * AWS MediaConvert job ID.
     * NEW: Tracks transcoding job for status polling.
     */
    @Column(name = "processing_job_id", length = 200)
    private String processingJobId;

    /**
     * Error message if processing failed.
     * NEW: Helps debug transcoding failures.
     */
    @Column(name = "processing_error", columnDefinition = "TEXT")
    private String processingError;

    /**
     * Transcoding job ID (AWS MediaConvert).
     * @deprecated Use processingJobId instead
     */
    @Column(name = "transcoding_job_id", length = 100)
    @Deprecated
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
        if (durationSeconds == null || durationSeconds == 0) {
            return 0;
        }
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

    /**
     * Check if video is ready for playback.
     */
    public boolean isReady() {
        return processingStatus == VideoProcessingStatus.READY;
    }

    /**
     * Check if video is still processing.
     */
    public boolean isProcessing() {
        return processingStatus == VideoProcessingStatus.PROCESSING;
    }

    /**
     * Check if video processing failed.
     */
    public boolean hasFailed() {
        return processingStatus == VideoProcessingStatus.FAILED;
    }
}