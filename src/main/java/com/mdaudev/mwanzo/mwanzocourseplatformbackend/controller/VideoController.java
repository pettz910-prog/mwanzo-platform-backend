package com.mdaudev.mwanzo.mwanzocourseplatformbackend.controller;

import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.course.Section;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.course.Video;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.course.VideoProgress;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.service.S3Service;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.service.VideoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Video REST Controller
 *
 * Handles course video management, streaming, and progress tracking.
 *
 * Base URL: /api/v1/videos
 *
 * Endpoints:
 * - GET    /api/v1/videos/courses/{courseId}/sections     - Get course sections
 * - GET    /api/v1/videos/sections/{sectionId}/videos     - Get section videos
 * - GET    /api/v1/videos/{videoId}                       - Get video details
 * - POST   /api/v1/videos/{videoId}/progress              - Update watch progress
 * - GET    /api/v1/videos/{videoId}/access                - Check video access
 *
 * Instructor Endpoints (to be secured):
 * - POST   /api/v1/videos/courses/{courseId}/sections     - Create section
 * - POST   /api/v1/videos/sections/{sectionId}/videos     - Upload video
 *
 * @author Mwanzo Development Team
 * @version 1.0
 * @since 2026-01-12
 */
@RestController
@RequestMapping("/api/v1/videos")
@RequiredArgsConstructor
@Slf4j
public class VideoController {

    private final VideoService videoService;
    private final S3Service s3Service;

    // ==========================================
    // UPLOAD ENDPOINTS (Presigned URLs)
    // ==========================================

    /**
     * Get presigned URL for video upload.
     * Frontend uses this URL to upload video directly to S3.
     *
     * Request:
     * POST /api/v1/videos/upload-url/video
     * {
     *   "fileName": "lesson1.mp4",
     *   "contentType": "video/mp4"
     * }
     *
     * Response:
     * {
     *   "presignedUrl": "http://localhost:4566/...",
     *   "s3Key": "videos/uuid/lesson1.mp4",
     *   "bucket": "mwanzo-videos",
     *   "expiresAt": "2026-01-12T15:30:00Z"
     * }
     */
    @PostMapping("/upload-url/video")
    public ResponseEntity<?> getVideoUploadUrl(@RequestBody UploadUrlRequest request) {
        log.info("POST /api/v1/videos/upload-url/video - Generating upload URL for: {}",
                request.getFileName());

        var uploadUrl = s3Service.generateVideoUploadUrl(
                request.getFileName(),
                request.getContentType(),
                java.time.Duration.ofMinutes(15)
        );

        return ResponseEntity.ok(uploadUrl);
    }

    /**
     * Get presigned URL for thumbnail upload.
     */
    @PostMapping("/upload-url/thumbnail")
    public ResponseEntity<?> getThumbnailUploadUrl(@RequestBody UploadUrlRequest request) {
        log.info("POST /api/v1/videos/upload-url/thumbnail - Generating upload URL for: {}",
                request.getFileName());

        var uploadUrl = s3Service.generateThumbnailUploadUrl(
                request.getFileName(),
                request.getContentType(),
                java.time.Duration.ofMinutes(15)
        );

        return ResponseEntity.ok(uploadUrl);
    }

    // ==========================================
    // STUDENT ENDPOINTS (Video Viewing)
    // ==========================================

    /**
     * Get all sections for a course.
     * Returns course curriculum structure.
     *
     * @param courseId Course UUID
     * @return List of sections with video counts
     */
    @GetMapping("/courses/{courseId}/sections")
    public ResponseEntity<List<Section>> getCourseSections(@PathVariable UUID courseId) {
        log.info("GET /api/v1/videos/courses/{}/sections", courseId);

        List<Section> sections = videoService.getPublishedSections(courseId);

        return ResponseEntity.ok(sections);
    }

    /**
     * Get preview videos for a course (free for everyone).
     * Used on course detail page to let users preview before buying.
     *
     * @param courseId Course UUID
     * @return List of preview videos
     */
    @GetMapping("/courses/{courseId}/preview")
    public ResponseEntity<List<Video>> getPreviewVideos(@PathVariable UUID courseId) {
        log.info("GET /api/v1/videos/courses/{}/preview", courseId);

        List<Video> previewVideos = videoService.getPreviewVideos(courseId);

        return ResponseEntity.ok(previewVideos);
    }

    /**
     * Get all videos in a section.
     *
     * @param sectionId Section UUID
     * @return List of videos
     */
    @GetMapping("/sections/{sectionId}/videos")
    public ResponseEntity<List<Video>> getSectionVideos(@PathVariable UUID sectionId) {
        log.info("GET /api/v1/videos/sections/{}/videos", sectionId);

        List<Video> videos = videoService.getPublishedVideos(sectionId);

        return ResponseEntity.ok(videos);
    }

    /**
     * Get video details by ID.
     * Used to get streaming URL for video player.
     *
     * @param videoId Video UUID
     * @return Video details with streaming URL
     */
    @GetMapping("/{videoId}")
    public ResponseEntity<Video> getVideo(@PathVariable UUID videoId) {
        log.info("GET /api/v1/videos/{}", videoId);

        Video video = videoService.getVideoById(videoId);

        return ResponseEntity.ok(video);
    }

    /**
     * Update video watch progress.
     * Called periodically by video player (every 10-15 seconds).
     *
     * Request Body:
     * {
     *   "studentId": "uuid",
     *   "positionSeconds": 145
     * }
     *
     * @param videoId Video UUID
     * @param request Progress update request
     * @return Updated progress
     */
    @PostMapping("/{videoId}/progress")
    public ResponseEntity<VideoProgress> updateProgress(
            @PathVariable UUID videoId,
            @RequestBody ProgressUpdateRequest request) {

        log.debug("POST /api/v1/videos/{}/progress - position: {} seconds",
                videoId, request.getPositionSeconds());

        VideoProgress progress = videoService.updateProgress(
                request.getStudentId(),
                videoId,
                request.getPositionSeconds()
        );

        return ResponseEntity.ok(progress);
    }

    /**
     * Batch update progress for multiple videos.
     * More efficient than multiple single updates.
     * Frontend calls this when:
     * - User leaves course page
     * - Browser/tab closes
     * - Switching between videos
     *
     * Request Body:
     * {
     *   "studentId": "uuid",
     *   "updates": [
     *     {"videoId": "uuid1", "positionSeconds": 145},
     *     {"videoId": "uuid2", "positionSeconds": 320}
     *   ]
     * }
     *
     * @param request Batch progress update request
     * @return Summary of updates
     */
    @PostMapping("/progress/batch")
    public ResponseEntity<BatchProgressResponse> batchUpdateProgress(
            @RequestBody BatchProgressRequest request) {

        log.info("POST /api/v1/videos/progress/batch - {} updates for student {}",
                request.getUpdates().size(), request.getStudentId());

        int successCount = 0;
        int failedCount = 0;

        for (VideoProgressUpdate update : request.getUpdates()) {
            try {
                videoService.updateProgress(
                        request.getStudentId(),
                        update.getVideoId(),
                        update.getPositionSeconds()
                );
                successCount++;
            } catch (Exception e) {
                log.error("Failed to update progress for video {}: {}",
                        update.getVideoId(), e.getMessage());
                failedCount++;
            }
        }

        BatchProgressResponse response = new BatchProgressResponse(
                successCount,
                failedCount,
                request.getUpdates().size()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Check if student has access to video.
     * Frontend calls this before playing video.
     *
     * @param videoId Video UUID
     * @param studentId Student UUID (query param)
     * @return Access status
     */
    @GetMapping("/{videoId}/access")
    public ResponseEntity<VideoAccessResponse> checkAccess(
            @PathVariable UUID videoId,
            @RequestParam UUID studentId) {

        log.debug("GET /api/v1/videos/{}/access?studentId={}", videoId, studentId);

        boolean hasAccess = videoService.hasVideoAccess(studentId, videoId);

        VideoAccessResponse response = new VideoAccessResponse(hasAccess);

        return ResponseEntity.ok(response);
    }

    // ==========================================
    // INSTRUCTOR ENDPOINTS (Content Management)
    // ==========================================

    /**
     * Create a new section (Instructor only).
     * TODO: Add @PreAuthorize("hasRole('INSTRUCTOR')") when security is implemented
     *
     * @param courseId Course UUID
     * @param request Section creation request
     * @return Created section
     */
    @PostMapping("/courses/{courseId}/sections")
    public ResponseEntity<Section> createSection(
            @PathVariable UUID courseId,
            @RequestBody CreateSectionRequest request) {

        log.info("POST /api/v1/videos/courses/{}/sections - Creating section: {}",
                courseId, request.getTitle());

        Section section = videoService.createSection(
                courseId,
                request.getTitle(),
                request.getDescription(),
                request.getDisplayOrder()
        );

        return ResponseEntity.status(201).body(section);
    }

    /**
     * Upload/create a new video (Instructor only).
     * Called AFTER video uploaded to S3 via presigned URL.
     * TODO: Add @PreAuthorize("hasRole('INSTRUCTOR')") when security is implemented
     *
     * Flow:
     * 1. Frontend gets presigned URLs (video + thumbnail)
     * 2. Frontend uploads files directly to S3
     * 3. Frontend calls this endpoint with S3 keys
     * 4. Backend creates video record
     *
     * @param sectionId Section UUID
     * @param request Video creation request
     * @return Created video
     */
    @PostMapping("/sections/{sectionId}/videos")
    public ResponseEntity<Video> createVideo(
            @PathVariable UUID sectionId,
            @RequestBody CreateVideoRequest request) {

        log.info("POST /api/v1/videos/sections/{}/videos - Creating video: {}",
                sectionId, request.getTitle());

        Video video = videoService.createVideo(
                request.getCourseId(),
                sectionId,
                request.getTitle(),
                request.getDisplayOrder(),
                request.getS3Key(),
                request.getThumbnailS3Key(),
                request.getIsPreview()
        );

        return ResponseEntity.status(201).body(video);
    }
}

// ==========================================
// REQUEST/RESPONSE DTOs
// ==========================================

/**
 * Upload URL Request
 */
@lombok.Data
class UploadUrlRequest {
    private String fileName;
    private String contentType;
}

/**
 * Progress Update Request
 */
@lombok.Data
class ProgressUpdateRequest {
    private UUID studentId;
    private Integer positionSeconds;
}

/**
 * Batch Progress Request
 */
@lombok.Data
class BatchProgressRequest {
    private UUID studentId;
    private java.util.List<VideoProgressUpdate> updates;
}

/**
 * Video Progress Update (for batch)
 */
@lombok.Data
class VideoProgressUpdate {
    private UUID videoId;
    private Integer positionSeconds;
}

/**
 * Batch Progress Response
 */
@lombok.Data
@lombok.AllArgsConstructor
class BatchProgressResponse {
    private Integer successCount;
    private Integer failedCount;
    private Integer totalCount;
}

/**
 * Video Access Response
 */
@lombok.Data
@lombok.AllArgsConstructor
class VideoAccessResponse {
    private Boolean hasAccess;
}

/**
 * Create Section Request
 */
@lombok.Data
class CreateSectionRequest {
    private String title;
    private String description;
    private Integer displayOrder;
}

/**
 * Create Video Request
 */
@lombok.Data
class CreateVideoRequest {
    private UUID courseId;
    private String title;
    private String description;
    private Integer displayOrder;
    private String s3Key;           // Video S3 key (after upload)
    private String thumbnailS3Key;  // Thumbnail S3 key (after upload)
    private Boolean isPreview;      // Mark as free preview video
}