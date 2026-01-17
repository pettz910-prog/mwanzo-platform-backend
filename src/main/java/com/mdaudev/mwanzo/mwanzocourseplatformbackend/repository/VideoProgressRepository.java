package com.mdaudev.mwanzo.mwanzocourseplatformbackend.repository;

import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.course.VideoProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Video Progress Repository
 */
@Repository
public interface VideoProgressRepository extends JpaRepository<VideoProgress, UUID> {

    Optional<VideoProgress> findByStudentIdAndVideoId(UUID studentId, UUID videoId);

    List<VideoProgress> findByStudentIdAndCourseId(UUID studentId, UUID courseId);

    List<VideoProgress> findByEnrollmentId(UUID enrollmentId);

    @Query("SELECT COUNT(vp) FROM VideoProgress vp WHERE vp.enrollmentId = :enrollmentId AND vp.isCompleted = true")
    long countCompletedVideosByEnrollmentId(@Param("enrollmentId") UUID enrollmentId);

    @Query("SELECT vp FROM VideoProgress vp WHERE vp.studentId = :studentId AND vp.courseId = :courseId AND vp.lastWatchedAt IS NOT NULL ORDER BY vp.lastWatchedAt DESC")
    List<VideoProgress> findRecentlyWatchedVideos(@Param("studentId") UUID studentId, @Param("courseId") UUID courseId);
}