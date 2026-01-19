package com.mdaudev.mwanzo.mwanzocourseplatformbackend.service;

import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.course.*;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.exception.ResourceNotFoundException;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Video Service (Updated with MediaConvert Integration)
 *
 * Now supports:
 * - Multi-quality video transcoding
 * - Automatic duration extraction
 * - CloudFront CDN delivery
 *
 * @author Mwanzo Development Team
 * @version 2.0
 * @since 2026-01-17
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class VideoService {

    private final SectionRepository sectionRepository;
    private final VideoRepository videoRepository;
    private final VideoProgressRepository videoProgressRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final S3Service s3Service;
    private final MediaConvertService mediaConvertService;
    private final CloudFrontService cloudFrontService;

    public List<Section> getPublishedSections(UUID courseId) {
        return sectionRepository.findPublishedSectionsByCourseId(courseId);
    }

    @Transactional
    public Section createSection(UUID courseId, String title, String description, Integer displayOrder) {
        courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));

        Section section = Section.builder()
                .courseId(courseId)
                .title(title)
                .description(description)
                .displayOrder(displayOrder)
                .build();

        return sectionRepository.save(section);
    }

    public List<Video> getPublishedVideos(UUID sectionId) {
        return videoRepository.findPublishedVideosBySectionId(sectionId);
    }

    public List<Video> getPreviewVideos(UUID courseId) {
        log.debug("Fetching preview videos for course: {}", courseId);
        return videoRepository.findPreviewVideosByCourseId(courseId);
    }

    public Video getVideoById(UUID videoId) {
        return videoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("Video", "id", videoId));
    }

    /**
     * Create video after S3 upload (NEW: with MediaConvert).
     *
     * Flow:
     * 1. Verify S3 upload
     * 2. Create video record (status: PROCESSING)
     * 3. Trigger MediaConvert job
     * 4. Job completes asynchronously via webhook
     */
    @Transactional
    public Video createVideo(UUID courseId, UUID sectionId, String title, Integer displayOrder,
                             String s3Key, String thumbnailS3Key, Boolean isPreview) {

        log.info("🎬 Creating video for section: {}", sectionId);

        // 1. Verify S3 objects exist
        if (!s3Service.objectExists(s3Key)) {
            throw new IllegalArgumentException("Video file not found in S3: " + s3Key);
        }
        if (!s3Service.objectExists(thumbnailS3Key)) {
            throw new IllegalArgumentException("Thumbnail not found in S3: " + thumbnailS3Key);
        }

        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Section", "id", sectionId));

        // 2. Get video metadata
        var metadata = s3Service.getVideoMetadata(s3Key);

        // 3. Generate URLs (will be updated when processing completes)
        String videoUrl = cloudFrontService.generateStreamingUrl(s3Key);
        String thumbnailUrl = cloudFrontService.generateStreamingUrl(thumbnailS3Key);

        // 4. Create video record with PROCESSING status
        Video video = Video.builder()
                .courseId(courseId)
                .sectionId(sectionId)
                .title(title)
                .displayOrder(displayOrder)
                .durationSeconds(0)  // Will be updated by MediaConvert
                .s3Key(s3Key)
                .videoUrl(videoUrl)
                .thumbnailS3Key(thumbnailS3Key)
                .thumbnailUrl(thumbnailUrl)
                .fileSizeBytes(metadata.getFileSizeBytes())
                .isPreview(isPreview != null ? isPreview : false)
                .processingStatus(VideoProcessingStatus.PROCESSING)  // NEW
                .qualityUrlsJson("{}")  // Will be populated after processing
                .build();

        Video savedVideo = videoRepository.save(video);

        // 5. Trigger MediaConvert job (async)
        mediaConvertService.createTranscodingJob(savedVideo.getId(), s3Key)
                .thenAccept(result -> handleTranscodingComplete(savedVideo.getId(), result))
                .exceptionally(ex -> {
                    log.error("❌ Transcoding failed for video: {}", savedVideo.getId(), ex);
                    markVideoAsFailed(savedVideo.getId(), ex.getMessage());
                    return null;
                });

        log.info("✅ Video created (processing): {}", savedVideo.getId());
        return savedVideo;
    }

    /**
     * Handle transcoding job completion.
     * Updates video with quality URLs and duration.
     */
    @Transactional
    public void handleTranscodingComplete(UUID videoId,
                                          MediaConvertService.TranscodingJobResult result) {

        log.info("🎉 Transcoding completed for video: {}", videoId);

        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("Video", "id", videoId));

        // Update video with processed outputs
        if (result.getStatus() == MediaConvertService.JobStatus.COMPLETE) {

            // Generate CloudFront URLs for each quality
            Map<String, String> qualityUrls = new HashMap<>();
            if (result.getQualityOutputs() != null) {
                result.getQualityOutputs().forEach((quality, s3Key) -> {
                    String url = cloudFrontService.generateStreamingUrl(s3Key);
                    qualityUrls.put(quality, url);
                });
            }

            // Update video record
            video.setDurationSeconds(result.getDurationSeconds());
            video.setProcessingStatus(VideoProcessingStatus.READY);
            video.setProcessingJobId(result.getJobId());
            video.setQualityUrlsJson(convertQualityUrlsToJson(qualityUrls));

            // Default videoUrl to highest quality
            if (qualityUrls.containsKey("1080p")) {
                video.setVideoUrl(qualityUrls.get("1080p"));
            } else if (qualityUrls.containsKey("720p")) {
                video.setVideoUrl(qualityUrls.get("720p"));
            }

            videoRepository.save(video);

            // Update section duration (only if duration > 0)
            if (video.getDurationSeconds() != null && video.getDurationSeconds() > 0) {
                Section section = sectionRepository.findById(video.getSectionId())
                        .orElseThrow(() -> new ResourceNotFoundException("Section", "id", video.getSectionId()));
                section.addVideo(video.getDurationMinutes());
                sectionRepository.save(section);
            }

            log.info("✅ Video ready for playback: {} ({}s, {} qualities)",
                    videoId, video.getDurationSeconds(), qualityUrls.size());

        } else {
            markVideoAsFailed(videoId, result.getErrorMessage());
        }
    }

    /**
     * Mark video as failed.
     */
    @Transactional
    public void markVideoAsFailed(UUID videoId, String errorMessage) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("Video", "id", videoId));

        video.setProcessingStatus(VideoProcessingStatus.FAILED);
        video.setProcessingError(errorMessage);
        videoRepository.save(video);

        log.error("❌ Video processing failed: {} - {}", videoId, errorMessage);
    }

    /**
     * Convert quality URLs map to JSON string.
     */
    private String convertQualityUrlsToJson(Map<String, String> qualityUrls) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper()
                    .writeValueAsString(qualityUrls);
        } catch (Exception e) {
            log.error("Failed to serialize quality URLs", e);
            return "{}";
        }
    }

    /**
     * Get video quality URLs (for adaptive streaming).
     */
    public Map<String, String> getVideoQualityUrls(UUID videoId) {
        Video video = getVideoById(videoId);

        if (video.getQualityUrlsJson() == null || video.getQualityUrlsJson().isEmpty()) {
            return Map.of("default", video.getVideoUrl());
        }

        try {
            return new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(video.getQualityUrlsJson(), Map.class);
        } catch (Exception e) {
            log.error("Failed to deserialize quality URLs", e);
            return Map.of("default", video.getVideoUrl());
        }
    }

    // ========== Existing progress tracking methods (unchanged) ==========

    @Transactional
    public VideoProgress updateProgress(UUID studentId, UUID videoId, Integer positionSeconds) {
        VideoProgress progress = videoProgressRepository.findByStudentIdAndVideoId(studentId, videoId)
                .orElseGet(() -> {
                    Video video = getVideoById(videoId);
                    Enrollment enrollment = enrollmentRepository
                            .findByStudentIdAndCourseId(studentId, video.getCourseId())
                            .orElseThrow(() -> new IllegalStateException("Not enrolled"));

                    return videoProgressRepository.save(VideoProgress.builder()
                            .studentId(studentId)
                            .enrollmentId(enrollment.getId())
                            .videoId(videoId)
                            .courseId(video.getCourseId())
                            .sectionId(video.getSectionId())
                            .build());
                });

        Video video = getVideoById(videoId);
        progress.updateProgress(positionSeconds, video.getDurationSeconds());
        VideoProgress saved = videoProgressRepository.save(progress);

        if (saved.getIsCompleted() && saved.getCompletedAt() != null) {
            video.incrementViewCount();
            videoRepository.save(video);
        }

        updateEnrollmentProgress(progress.getEnrollmentId());
        return saved;
    }

    @Transactional
    public void updateEnrollmentProgress(UUID enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment", "id", enrollmentId));

        long completedVideos = videoProgressRepository.countCompletedVideosByEnrollmentId(enrollmentId);
        long totalVideos = videoRepository.countByCourseId(enrollment.getCourseId());

        if (totalVideos > 0) {
            int progressPercentage = (int) ((completedVideos * 100) / totalVideos);
            enrollment.updateProgress(progressPercentage);

            if (completedVideos == totalVideos) {
                enrollment.setVideosCompleted(true);

                // Check if quizzes also completed, then mark course complete
                if (enrollment.getQuizzesCompleted()) {
                    enrollment.markAsCompleted();
                    log.info("🎉 Student {} completed course {}!",
                            enrollment.getStudentId(), enrollment.getCourseId());
                }
            }

            enrollmentRepository.save(enrollment);
        }
    }

    public boolean hasVideoAccess(UUID studentId, UUID videoId) {
        Video video = getVideoById(videoId);
        if (video.getIsPreview()) return true;

        return enrollmentRepository.findByStudentIdAndCourseId(studentId, video.getCourseId())
                .map(Enrollment::isAccessible)
                .orElse(false);
    }
}