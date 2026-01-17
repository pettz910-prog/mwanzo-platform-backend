package com.mdaudev.mwanzo.mwanzocourseplatformbackend.service;

import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.course.*;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.exception.ResourceNotFoundException;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Video Service
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

    /**
     * Get preview videos for a course (free for everyone).
     */
    public List<Video> getPreviewVideos(UUID courseId) {
        log.debug("Fetching preview videos for course: {}", courseId);
        return videoRepository.findPreviewVideosByCourseId(courseId);
    }

    public Video getVideoById(UUID videoId) {
        return videoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("Video", "id", videoId));
    }

    /**
     * Create video after S3 upload.
     * Called by frontend after video uploaded to S3.
     */
    @Transactional
    public Video createVideo(UUID courseId, UUID sectionId, String title, Integer displayOrder,
                             String s3Key, String thumbnailS3Key, Boolean isPreview) {
        log.info("Creating video for section: {}", sectionId);

        // Verify S3 objects exist
        if (!s3Service.objectExists(s3Key)) {
            throw new IllegalArgumentException("Video file not found in S3: " + s3Key);
        }
        if (!s3Service.objectExists(thumbnailS3Key)) {
            throw new IllegalArgumentException("Thumbnail not found in S3: " + thumbnailS3Key);
        }

        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Section", "id", sectionId));

        // Get video metadata
        var metadata = s3Service.getVideoMetadata(s3Key);

        // Generate streaming URLs
        String videoUrl = s3Service.generateStreamingUrl(s3Key);
        String thumbnailUrl = s3Service.generateStreamingUrl(thumbnailS3Key);

        // TODO: Extract actual duration from video file (use ffmpeg or AWS MediaConvert)
        // For now, duration must be provided by frontend
        Integer durationSeconds = 0;  // Placeholder

        Video video = Video.builder()
                .courseId(courseId)
                .sectionId(sectionId)
                .title(title)
                .displayOrder(displayOrder)
                .durationSeconds(durationSeconds)
                .s3Key(s3Key)
                .videoUrl(videoUrl)
                .thumbnailS3Key(thumbnailS3Key)
                .thumbnailUrl(thumbnailUrl)
                .fileSizeBytes(metadata.getFileSizeBytes())
                .isPreview(isPreview != null ? isPreview : false)
                .processingStatus(com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.course.VideoProcessingStatus.READY)
                .build();

        Video saved = videoRepository.save(video);
        section.addVideo(video.getDurationMinutes());
        sectionRepository.save(section);

        log.info("Video created: {}", saved.getId());
        return saved;
    }

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

        // If video just completed, increment view count
        if (saved.getIsCompleted() && saved.getCompletedAt() != null) {
            video.incrementViewCount();
            videoRepository.save(video);
        }

        // Update enrollment progress
        updateEnrollmentProgress(progress.getEnrollmentId());

        return saved;
    }

    /**
     * Update enrollment overall progress based on completed videos.
     */
    @Transactional
    public void updateEnrollmentProgress(UUID enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment", "id", enrollmentId));

        long completedVideos = videoProgressRepository.countCompletedVideosByEnrollmentId(enrollmentId);
        long totalVideos = videoRepository.countByCourseId(enrollment.getCourseId());

        if (totalVideos > 0) {
            int progressPercentage = (int) ((completedVideos * 100) / totalVideos);
            enrollment.updateProgress(progressPercentage);

            // Check if all videos completed
            if (completedVideos == totalVideos) {
                enrollment.setVideosCompleted(true);

                // If quizzes also completed (or no quizzes), mark course complete
                if (enrollment.getQuizzesCompleted() || totalVideos == completedVideos) {
                    enrollment.markAsCompleted();
                    log.info("🎉 Student {} completed course {}!",
                            enrollment.getStudentId(), enrollment.getCourseId());
                }
            }

            enrollmentRepository.save(enrollment);
            log.debug("Updated enrollment {} progress: {}% ({}/{} videos)",
                    enrollmentId, progressPercentage, completedVideos, totalVideos);
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