package com.mdaudev.mwanzo.mwanzocourseplatformbackend.repository;

import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.course.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Video Repository
 */
@Repository
public interface VideoRepository extends JpaRepository<Video, UUID> {

    List<Video> findBySectionIdOrderByDisplayOrderAsc(UUID sectionId);

    List<Video> findByCourseIdOrderByDisplayOrderAsc(UUID courseId);

    @Query("SELECT v FROM Video v WHERE v.sectionId = :sectionId AND v.isPublished = true ORDER BY v.displayOrder ASC")
    List<Video> findPublishedVideosBySectionId(@Param("sectionId") UUID sectionId);

    @Query("SELECT v FROM Video v WHERE v.courseId = :courseId AND v.isPreview = true AND v.isPublished = true")
    List<Video> findPreviewVideosByCourseId(@Param("courseId") UUID courseId);

    long countBySectionId(UUID sectionId);

    long countByCourseId(UUID courseId);

    @Query("SELECT COALESCE(SUM(v.durationSeconds), 0) FROM Video v WHERE v.courseId = :courseId")
    Integer getTotalDurationByCourseId(@Param("courseId") UUID courseId);
}